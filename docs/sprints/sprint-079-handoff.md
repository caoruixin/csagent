---
title: Sprint 079 / S-Auto-24 / M-Auto-6 Sub-sprint B dev handoff — R3.a + R3.b + R3.c admin trace observability (UI-only)
doc_tier: sprint-archive
status: current
implementation_status: implemented
source_of_truth: code (ui/src/components/admin/{SessionList,TraceViewer}.tsx, ui/src/api/client.ts, ui/src/types/index.ts, server/.../runtime/SessionManager.java)
last_reviewed: 2026-06-06
review_cadence: ad hoc
supersedes: []
superseded_by: null
notes: >
  M-Auto-6's observability sub-sprint. UI-only admin-trace catch-up so bad-case
  triage is not blocked by display artefacts: R3.a (SessionList full
  handling_state coverage), R3.b (TraceViewer A1-dedup ToolEvent folding with
  expand-to-audit), R3.c (informational guard rejection badge distinct from
  blocking runtime errors), #4 (terminal handling_state header), #5 (≤10 LOC
  non-behavioural handling_state-transition diagnostic logging — the ONE backend
  touch). ZERO runtime semantic / guard / enum / prompt / yaml / CaseSpec /
  simulator / scoring edit. §7-EXEMPT (UI/observability). NO outcome-evidence
  re-bless at this sub-sprint — that is the M-Auto-6 milestone-shared re-bless
  after C-1 + C-2. First UI test harness in repo history added (vitest +
  @testing-library/react + jsdom) per human decision (see §3).
---

# Sprint 079 / S-Auto-24 / M-Auto-6 Sub-sprint B — dev handoff

## §0 Cold-start summary + verdict

**Goal.** Make admin bad-case triage readable: render every
`BotSession.handling_state` distinctly (R3.a, case c7), fold A1-dedup
tool-call repeats with expand-to-audit (R3.b, case c8), distinguish CORRECT
§1.4 informational guard rejections from BLOCKING runtime errors (R3.c, cases
c2/c10/c17), surface the terminal disposition in the trace header (#4), and
add a non-behavioural paper trail for `handling_state` transitions (#5). The
underlying runtime is already correct; this catches the UI surface up.

**Verdict.** All of #1–#6 landed. **UI tests `vitest run` = 10 passed / 0
failed / 0 skipped.** **`tsc -b` = 0** and **`vite build` = success.** Java
`mvn test` = **1297 run / 1 fail / 0 err / 2 skip** = post-S-Auto-23 baseline
`1244 / 1 / 0 / 2` + 53 inherited tests, **same single inherited failure**
`SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly`
(a Sprint-6 system-prompt anchor assertion, unrelated to this UI/observability
sub-sprint; confirmed pre-existing in the Sprint-078 handoff §0). **eval pytest
= 548 passed / 5 failed = 553 total UNCHANGED** (the 5 failures are all
`test_corpus_lint.py` `ModuleNotFoundError: No module named
'eval_interactive.case_spec'` conda-python subprocess env-drift — env/config,
not a regression; zero `eval_interactive/**` files touched). **autoloop pytest
= 324 passed / 0 failed UNCHANGED** (1 expected baseline-loader warning).

**Layer (per `iteration_governance.md` §3.2).** R3.a / R3.b / R3.c / #4 = `infra`
(observability — admin UI rendering of existing runtime trace events +
structured flags). #5 = `infra` (non-behavioural diagnostic logging). No Tier-0
invariant added; no semantic hardcode; no `BotSession.handling_state` enum
value added; no new `escalation_reason` enum value. **§7-EXEMPT** per proposal
§6.5 / §7.2 (UI-only carve-out); stanza reproduced in §4 record-only.

---

## §1 — Per-change evidence

### #1 — R3.a `SessionList` StatusBadge full handling_state coverage

**Files:**
- `ui/src/types/index.ts:54-62` — `Session` gains `handling_state: string`
  (raw value, no collapse) + `escalation_reason: string`. `status` retained
  for legacy/sort.
- `ui/src/api/client.ts:74-75` — `mapSession` surfaces
  `handling_state: s.handlingState ?? s.handling_state ?? s.currentPhase ?? ''`
  and `escalation_reason: s.escalationReason ?? s.escalation_reason ?? ''`. The
  pre-existing `status: mapHandlingState(...)` 3-bucket mapper is UNCHANGED.
- `ui/src/components/admin/SessionList.tsx:166-201` — `STATUS_BADGES` map +
  rewritten `StatusBadge({ value })`. Every backend `handling_state` value
  gets a distinct colour + label; unknown values render grey with raw text
  (never hidden).
- `ui/src/components/admin/SessionList.tsx:143` — table cell now passes the raw
  `s.handling_state || s.status`.
- `ui/src/components/admin/SessionList.tsx:34-38` — sort `case 'status'` reads
  `handling_state` so column order matches the rendered badge.

**Complete handling_state value set rendered** (sourced from the Java
`setHandlingState(...)` call sites — `SessionManager.java:176`,
`ControlKernel.java:498/573/1255`, `PhaseEvaluator.java:1404/1415/1486` — plus
the `BOT_HANDLING` builder default at `SessionManager.java:101`):
`BOT_HANDLING` → "Bot Handling" (green), `QUEUE_TO_HUMAN` → "Queued to Human"
(amber), `HUMAN_HANDLING` → "Human Handling" (blue), `CLOSED` → "Closed"
(indigo). Legacy semantic statuses (`active`/`escalated`/`ended`) retained for
older rows. **No UI-side session filtering**: `SessionList` renders every row
in the backend response (`DemoInspectionController.java:86` is already
`findAll()` with no filter; the UI adds none).

**Gating tests** (`ui/src/components/admin/SessionList.test.tsx`):
`renders every handling_state value with a distinct label + colour` (asserts
4 distinct backgrounds + labels, all rows present),
`renders an unrecognised handling_state safely (raw text, never hidden)`,
`does not filter ESCALATE-terminated (QUEUE_TO_HUMAN) sessions out of the list`.

### #2 — R3.b `TraceViewer` dedup ToolEvent folding

**Files:**
- `ui/src/types/index.ts:128-131` — `ToolCall` gains optional `deduplicated`,
  `original_at_step`, `step_index`, `sequence_index` (already passed through by
  `mapToolCalls`' `...tc` spread; types added for safety).
- `ui/src/components/admin/TraceViewer.tsx:180-215` — `sameDispatch(a,b)`
  (tool_name + JSON args equality) + `groupDedupToolCalls(toolCalls)` (folds
  `deduplicated===true` events under the most-recent matching primary;
  defensive: an unmatched dedup event becomes its own group so no record is
  dropped).
- `ui/src/components/admin/TraceViewer.tsx:561-616` — `ToolCallsSection`
  renders one `ToolCallRow` per primary + a `DedupRepeats` indicator;
  `DedupRepeats` is collapsed by default ("↳ N dedup'd repeats"), expands to a
  per-repeat list with `step_index` + `original_at_step`.

These render the existing `ToolEvent.deduplicated` / `original_at_step` flags
persisted by A1 dedup (`AgentRunLoopImpl.java:599-634`, serialized at
`ControlKernel.java:2169-2171`). **No per-call timestamp exists in the trace
payload** (timestamps are per-turn, not per-tool-call); the dedup rows surface
`step_index` + `original_at_step`, the references that DO exist.

**Gating tests** (`TraceViewer.test.tsx`):
`folds dedup repeats under the primary by default (collapsed)` (1 row + "2
dedup'd repeats", `dedup-expanded` absent),
`expands to reveal the full dedup audit (step + original_at_step) on click`
(2 `dedup-repeat-row`s, `original_at_step=1` visible).

### #3 — R3.c informational guard rejection badge

**Files:**
- `ui/src/components/admin/TraceViewer.tsx:155-174` —
  `INFORMATIONAL_GUARD_REJECTIONS` allow-list + `classifyToolCall(tc)`
  (`success===false` + error_message in allow-list → `informational`; any
  other failure → `blocking` = SAFER default; success → null).
- `ui/src/components/admin/TraceViewer.tsx:618-end` — `ToolCallRow`:
  informational rejections render a yellow "Informational guard rejection"
  badge, collapsed by default; expand reveals the rejection-code string +
  rationale + full args/result payload. Blocking errors keep the original red
  rendering UNCHANGED (always-inline = audit always visible).

**R3.c allow-list — explicit list + source of each entry.** All four keys are
verbatim public reject-reason constants from
`server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillGuardrailDispatcher.java`;
all four are §1.4 runtime-owned guards (correct behaviour, not crashes):

| UI allow-list key | Java constant | Source line |
|---|---|---|
| `progressive_resolve_record_outcome_premature` | `PROGRESSIVE_RESOLVE_REJECT_REASON` | `SkillGuardrailDispatcher.java:89-90` (guard logic `ResolveDispositionEvaluator.java:160-186`) |
| `intake_required_fields_missing_for_intake_complete` | `INTAKE_INCOMPLETE_REJECT_REASON` | `SkillGuardrailDispatcher.java:86-87` (guard logic `SkillGuardrailDispatcher.java:265-298`) |
| `s1_resolve_required_before_faq_miss_handover` | `FAQ_MISS_REJECT_REASON` | `SkillGuardrailDispatcher.java:83-84` |
| `s1_citation_presence_required` | `S1_CITATION_PRESENCE_REQUIRED` | `SkillGuardrailDispatcher.java:92-93` |

The two canonical seed codes (rows 1–2) cover c2/c10/c17 (record_outcome
premature) and the c1/c5/c6 intake path. The two S1 grounding-floor guards
(rows 3–4) are added per the prompt's "extend ONLY for codes traceable to the
guard files" clause — they are structurally identical guard rejections, not
crashes, and a reviewer seeing them rendered red would be equally misled. 4
entries ≤ the ≤6 STOP cap. The strings are CONSTANTS from the guard layer, NOT
user-message keywords (Codex Q1). **SAFER default:** any error_message NOT in
this set renders as a blocking error — a genuine failure is never visually
downgraded.

**Hard constraint honoured:** the categoriser is a read-side display
distinction over events the guard ALREADY produces; it changes no guard logic.
The runtime guard files (`ResolveDispositionEvaluator.java`,
`SkillGuardrailDispatcher.java`) were NOT edited.

**Gating tests** (`TraceViewer.test.tsx`):
`renders a known guard rejection as an informational badge (collapsed)`
(badge present, `tool-call-error` absent, `data-rejection-kind=informational`,
`rejection-code` collapsed),
`expands the informational rejection to reveal code + rationale (audit
preserved)`,
`renders an UNRECOGNISED rejection code as a blocking error (SAFER default)`
(no informational badge, red `tool-call-error` with the raw code,
`data-rejection-kind=blocking`).

### #4 — `TraceViewer` terminal handling_state header

**Files:**
- `ui/src/components/admin/TraceViewer.tsx:11,17-44` — TraceViewer additionally
  fetches `listSessions()` and finds the matching session for `sessionMeta`
  (the `/trace` endpoint is per-turn and carries no session disposition; no
  new backend endpoint added — best-effort, header degrades if it fails).
- `ui/src/components/admin/TraceViewer.tsx:57` — renders `<TerminalStateHeader>`.
- `ui/src/components/admin/TraceViewer.tsx:83-141` — `HANDLING_STATE_BADGES` +
  `TerminalStateHeader`: prominent terminal `handling_state` pill +
  `escalation_reason` row when present.

**Gating tests** (`TraceViewer.test.tsx`):
`renders the terminal handling_state + escalation_reason for an escalated
session` (QUEUE_TO_HUMAN → "Queued to Human" + `user_requested`),
`renders CLOSED terminal state without an escalation_reason row`.

### #5 — handling_state transition diagnostic logging (the ONE backend touch)

**File:** `server/src/main/java/com/gumtree/csagent/service/runtime/SessionManager.java`
- `:699-712` — new `private void logHandlingStateTransition(sessionId, oldState,
  newState, source)`. Pure logging (INFO); no-ops when unchanged. Uses
  `java.util.Objects` (already imported via `java.util.*`).
- `:353` + `:370-371` — `processMessage` captures `priorHandlingState` before
  `controlKernel.processMessage(...)` and logs the transition after, source
  `control_kernel`.
- `:177-178` — `createSession` hard-OOS branch logs the
  `BOT_HANDLING → QUEUE_TO_HUMAN` transition, source `routing_out_of_scope`.

**LOC fence:** behaviour-affecting additions ≈ 7 lines (capture 1 +
processMessage call 2 + createSession call 2 + helper `if`/`log` 2) — within
the ≤~10 LOC cap. **NO `SessionManager` state-machine edit**; the method only
reads + logs. No new Java test added (the contract permitted +1–2; not needed —
pure logging, and adding one would not change the failure count).

---

## §2 — Test / eval results (full numeric)

| Suite | Result | Baseline | Status |
|---|---|---|---|
| UI vitest (`npm test` / `vitest run`) | 10 passed / 0 failed / 0 skipped | n/a (new harness) | GREEN |
| UI `tsc -b` | exit 0 | exit 0 | GREEN |
| UI `vite build` (production) | success (93 modules) | success | GREEN |
| Java `mvn test` | 1297 run / 1 fail / 0 err / 2 skip | 1244 / 1 / 0 / 2 (+53 inherited) | PRESERVED (same inherited failure) |
| eval pytest (`eval_interactive/tests`) | 548 passed / 5 failed = 553 | 553 (548+5) | UNCHANGED (5 = conda-python corpus_lint env-drift) |
| autoloop pytest (`autoloop/tests`) | 324 passed / 0 failed | 324 | UNCHANGED (1 expected warning) |

The single Java failure
(`SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly`)
and the 5 eval `test_corpus_lint.py` failures are both documented inherited /
env-drift baselines (Sprint-078 handoff §0), unrelated to this sub-sprint —
zero runtime Java behaviour, prompt, yaml, CaseSpec, simulator, eval, or
autoloop scoring file was touched.

---

## §3 — UI test infrastructure note (new harness)

This repo had **no UI test infrastructure** before this sub-sprint (no
vitest/jest, no testing-library, no jsdom, no `test` script — confirmed across
git history; only `eslint` + `tsc` were available). The contract's #6 requires
UI tests GREEN. Per explicit human decision, the minimal harness was added:

- `ui/package.json` — devDeps `vitest@^4`, `@testing-library/react@^16`,
  `@testing-library/jest-dom@^6`, `jsdom@^29`; new `"test": "vitest run"`
  script. `ui/package-lock.json` updated (+83 packages).
- `ui/vite.config.ts:1,16-21` — `test` block (jsdom env, setupFiles,
  `src/**/*.test.{ts,tsx}`).
- `ui/src/test/setup.ts` — jest-dom matchers + `afterEach(cleanup)`.

This is the only structural addition beyond the named file fence; it is
self-contained to the `ui/` package and touches no runtime / eval / scoring
surface. **eslint** still reports 4 errors, ALL pre-existing on HEAD
(`setLoading(true)`-in-effect at `SessionList.tsx`/`TraceViewer.tsx`,
`no-explicit-any` in untouched `mapTrace`/`getPerUCMetrics`); this sub-sprint
introduces zero new lint errors.

---

## §4 — §7 Layer-classification + anti-hardcode stanza (record-only — §7-EXEMPT)

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

---

## §5 — Anti-误杀 invariants (HARD) — confirmation

1. **Audit data NOT lost.** Dedup folding (#2) and informational collapsing
   (#3) are DISPLAY-only; the underlying `ToolCall` records remain in
   `step.tool_calls` with full payload and are reachable by expanding. Tests
   `expands to reveal the full dedup audit...` and `expands the informational
   rejection to reveal code + rationale...` assert this.
2. **No session filtering at the UI level.** Test `does not filter
   ESCALATE-terminated...` + code review of `SessionList` (renders all `sorted`
   rows).
3. **No runtime behaviour change.** Only `SessionManager.java` was touched on
   the backend, and only with read-only logging (#5). No
   `ControlKernel`/`AgentRunLoopImpl`/`ContextProjectionBuilder`/
   `ResolveDispositionEvaluator`/`SkillGuardrailDispatcher`/`BudgetChecker`/
   tool/skill/guard edit.
4. **No scoring or eval surface touched.** eval pytest 553 + autoloop pytest
   324 unchanged by construction.
5. **R3.c SAFER default = blocking.** Test `renders an UNRECOGNISED rejection
   code as a blocking error...` asserts unknown → blocking.
6. **No new `escalation_reason` enum value** (#4 surfaces existing values).
7. **No `BotSession.handling_state` enum change** — `BotSession.java`
   untouched; the UI renders existing values + a safe fallback.

---

## §6 — Visual verification evidence

**What was verified automatically (jsdom component render + assertion).** Each
target case is covered by a vitest test that renders the real component with
the exact data shape and asserts the rendered behaviour:

| Case | Symptom | Test (file → name) |
|---|---|---|
| c7 (admin missing case) | terminal state unclear / states collapsed | SessionList.test.tsx → all 3 tests (full coverage + no filtering); TraceViewer.test.tsx → terminal-state header tests |
| c8 (dedup event UI) | 0ms repeats look like a bug | TraceViewer.test.tsx → dedup fold (collapsed) + expand (audit) |
| c2 / c10 / c17 (record_outcome guard) | correct guard rejection looks like a crash | TraceViewer.test.tsx → informational badge (collapsed) + expand (code+rationale) |
| negative | unknown failure must stay blocking | TraceViewer.test.tsx → UNRECOGNISED rejection → blocking |

**Deliver-reviewable manual step-by-step** (for the running stack):
1. Start backend (`local` profile) + UI (`npm run dev`), open Admin → Sessions.
2. **c7/R3.a:** confirm every session row shows a coloured badge; an
   escalated session shows "Queued to Human" (amber), a closed one "Closed"
   (indigo) — none missing/blank. Click an escalated session.
3. **#4:** confirm the trace header shows a "Terminal state:" pill +
   `escalation_reason` for the escalated session.
4. **c8/R3.b:** open a turn whose run had A1-dedup repeats (e.g. a temp=0
   `search_knowledge` storm); confirm a single "↳ N dedup'd repeats" line;
   click it → per-repeat `original_at_step` list appears.
5. **c2/c10/c17/R3.c:** open a turn with a `record_outcome` premature guard
   rejection; confirm a yellow "Informational guard rejection" badge (NOT a
   red error); click → rejection code + rationale + payload appear.
6. **negative:** a turn with a real tool crash (5xx) still shows the red error
   badge.

**HONEST LIMITATION:** I did **not** run the full backend + browser stack
against real seeded bad-case traces (c7/c8/c2/c10/c17) — that requires the
deliver-agent's seeded environment. Per the contract, "visual verification by
deliver + human is the primary acceptance evidence"; that pass remains to be
done. Automated component tests, `tsc -b`, and the production `vite build` are
the evidence produced here.

---

## §7 — STOP / fence confirmations

- **File fence respected.** Edited only: `ui/src/components/admin/{SessionList,
  TraceViewer}.tsx` (+ co-located `.test.tsx`), `ui/src/api/client.ts`,
  `ui/src/types/index.ts`, `ui/vite.config.ts`, `ui/package.json` +
  `package-lock.json`, `ui/src/test/setup.ts` (test harness),
  `server/.../SessionManager.java` (#5 logging only),
  `docs/sprints/sprint-079-handoff.md`. No runtime behaviour change beyond #5.
- **No `BotSession.handling_state` enum value added** (`BotSession.java`
  untouched).
- **No prompt / yaml / CaseSpec / simulator / scoring touched.**
- **Autoloop 5-file SHA-locked scoring set + `autoloop/config.yaml`
  `baseline_dir` UNCHANGED.**
- **`docs/current_eval_baseline.md` UNCHANGED.**
- **No outcome-evidence re-bless launched** at this sub-sprint — the re-bless
  is the M-Auto-6 milestone-shared one that runs after Sub-sprint C-1 + C-2.

---

## §8 — Commit plan

Recommended split (per `prompt-artifact-rules.md` §9): (1) R3.a SessionList +
client/types + test; (2) test harness (vitest/jsdom config + setup); (3) R3.b
dedup folding + test; (4) R3.c guard badge + test; (5) #4 terminal header +
test; (6) #5 SessionManager logging; (7) this handoff. The harness commit (2)
is broken out because it is the cross-cutting infra enabler for #6.

---

## §9 — Open questions for deliver / human

- **OQ-S79.1 (test infra).** This sub-sprint introduces the repo's first UI
  test harness (vitest + testing-library + jsdom). If the project prefers no
  standing UI test framework, the harness + `.test.tsx` files can be reverted
  and #6 satisfied by visual verification alone — but the human chose to add it.
- **OQ-S79.2 (terminal header fetch).** `TraceViewer` reads session disposition
  via `listSessions()` (no single-session demo endpoint exists). For large
  session tables a dedicated `GET /demo/sessions/{id}` would be lighter; out of
  this UI-only sub-sprint's fence (backend), surfaced for C-tier consideration.
- **OQ-S79.3 (per-call timestamps).** R3.b dedup rows show `step_index` +
  `original_at_step` because the trace payload has no per-tool-call timestamp
  (only per-turn). If per-call timestamps are wanted, that is a backend trace
  shape change — out of scope here.
