---
title: Sprint 069 / S-Auto-13b dev handoff — A3 deterministic backstop (faq_miss-state-aware same-turn search_knowledge re-search suppression)
doc_tier: sprint-archive
status: current
implementation_status: implemented
source_of_truth: this file
last_reviewed: 2026-06-02
review_cadence: ad hoc
notes: >
  Dev-authored handoff for S-Auto-13b (fourth sub-sprint of M-Auto-3,
  Substrate-hygiene). Closes the A3 deterministic-backstop follow-up to
  S-Auto-13: a per-run faq_miss-state-aware same-turn `search_knowledge`
  re-search suppression gate placed ALONGSIDE A1's per-run identity cache.
  Keyed purely on the EXISTING `faq_miss` result flag (NOT on query
  content / keyword / regex / enum / per-UC). The S-Auto-13 A3 SOFT layer
  (grounding_instruction + projection echo) stays beneath as the
  soft-signal-first measure. Same falsification → deterministic-backstop
  pattern as A1 (Sprint 19/20 soft-signal-alone falsified → S-Auto-12
  hybrid 回挡), applied to the paraphrase shape A1 (byte-identical only)
  cannot catch.
---

# Sprint 069 / S-Auto-13b / M-Auto-3 — dev handoff

## §0 Summary

**Scope (4 steps, all delivered):**

1. **A3 backstop — `faq_miss`-state gate in `AgentRunLoopImpl`.** New per-run
   tracker `lastSearchKnowledgeViableHit` next to the S-Auto-12
   `successfulDispatchCache`. A new `search_knowledge` dispatch with the
   tracker non-null (the most-recent prior `search_knowledge` this run had
   `faq_miss=false`) is **suppressed**: the prior viable-hit result is
   served, the tool is NOT re-executed, no step/budget charged. Placed
   AFTER A1's byte-identical dedup check — A1's logic is untouched.
2. **Trace annotation.** `ToolEvent` gained two new record components
   `paraphraseSuppressed` + `faqHitAtStep` plus a `paraphraseSuppressed(...)`
   factory; `ControlKernel` flattens them onto the persisted
   `bot_turns.tool_calls` (report.html / admin trace). Distinct from the
   S-Auto-12 `deduplicated` / `originalAtStep` pair — a given event
   carries one annotation or the other, never both.
3. **Negative controls + A1 coexistence tests.** New
   `AgentRunLoopFaqMissStateGateTest` (6 cases) covers: first search not
   suppressed; re-search after `faq_miss=true` IS dispatched; cross-run
   gate state isolation; suppressed event carries annotation + prior hit
   + zero latency + step-0 reference; A1 + A3 coexistence (byte-identical
   caught by A1, paraphrase caught by A3); re-search after dispatch
   failure re-dispatches.
4. **3-pass `bad_cases` measurement + handoff** (this file §1–§4) on a
   freshly-restarted `:8080` backend.

**Outcome:** A3-backstop §11 within-turn target **MET** —
within-turn `PARAPHRASE_STORM` pre-fix **6 / 5 / 1** → post-fix
**0 / 0 / 0** across the 3 passes. The S-Auto-13 A3 SOFT layer
(`grounding_instruction` line + `search_reuse_instruction` projection
echo) was deliberately left in place beneath the backstop as the
soft-signal-first measure. **OQ-S68.4 incidentally subsumed**: any
byte-identical re-search that escapes A1 AND has a prior viable hit
this run is now caught by A3. **OQ-S69.1 surfaced**: cross-turn
paraphrase storm (a new bot turn issues a paraphrase of a prior turn's
viable hit) is OUT OF SCOPE for the per-turn gate by design and is a
separate fix surface (would require `BotSession`-scoped state, not
per-run state).

**Commits:**

- `1acd9b3` — A3 backstop code (ToolEvent + AgentRunLoopImpl +
  ControlKernel) + the 6-case `AgentRunLoopFaqMissStateGateTest`.
- this handoff — committed separately (commit-at-end; staged
  explicitly, no `git add -A`).

**Final test counts (re-measured at HEAD this sub-sprint):**

| Gate | Baseline (prompt) | This sub-sprint | Note |
|---|---|---|---|
| Java `mvn -pl server test` | `1192 / Failures 1 / 0 / 2` | **`1198 / Failures 1 / 0 / 2`** | +6 new gate tests; sole failure is the inherited `SystemPromptUserRequestedTiebreakerTest` (unchanged from baseline) |
| eval_interactive pytest | `495 passed, 8 failed` | **`495 passed, 8 failed`** | unchanged — the 8 are the 2026-06-01 `0323457` action_bank-split tests (OQ-S68.1, disjoint from this sub-sprint and NOT in scope to fix here) |
| autoloop pytest | `276 passed` | **`276 passed`** | unchanged |
| 17-fixture anti-hardcode | `31 passed` | **`31 passed`** | unchanged |
| scoring SHA | `35305bd8…` | **`35305bd84402ac455b126be5bcf8b2cb3b3fa55e3399f2c02443ea74bd8704e8`** | unchanged |

**Codex per-sub-sprint dispatch status:** **PENDING (deliver-agent at
close).** The deliver-agent authors the Codex prompt against §4.3 #2
(deterministic backstop on the LLM-owned "whether to re-search"
decision). Verification focus: keyed on the `faq_miss` RESULT flag
(not query content / keyword / regex / enum / per-UC); soft-signal-first
satisfied (S-Auto-13 soft layer fires + the model ignored it); no
legitimate-second-search false positive in the bad_cases data;
A1 (byte-identical) logic UNCHANGED.

## §1 The `faq_miss`-state gate mechanism

### Placement vs A1 (alongside, not modifying)

In `AgentRunLoopImpl.run(...)` the tool-call inner loop now carries two
per-run trackers next to each other (one local var per backstop):

| Backstop | Tracker | Key | Catches |
|---|---|---|---|
| A1 (S-Auto-12) | `Map<String, ToolEvent> successfulDispatchCache` | `toolName + "|" + canonicalArgumentsHash` | byte-identical `success==true` repeats of ANY tool |
| A3 (this sub-sprint) | `ToolEvent lastSearchKnowledgeViableHit` | the `faq_miss` RESULT flag on the most-recent `search_knowledge` dispatch | paraphrase same-turn re-searches of `search_knowledge` (different query string, same `faq_miss=false` intent) |

The order in the inner loop is:

1. `toolDispatcher.validateAgainstPlan(...)` (existing)
2. Sprint 39 handover/record_outcome skill guardrails (existing)
3. **A1 byte-identical dedup check** (existing, S-Auto-12 region at
   `AgentRunLoopImpl.java:486-503`) — `continue`s on hit
4. **A3 `faq_miss`-state gate** (NEW, `:506-557`) — `continue`s on hit
5. `toolDispatcher.dispatch(...)` (existing)
6. **A1 cache-put** (existing, `:554-556`) on success
7. **A3 tracker refresh** (NEW, `:558-580`) on success/failure
8. Sprint 12 `record_outcome` side effects, Sprint 8.1 §M3 DISCOVER
   short-circuit, handover flagging (all existing)

If A1 caught a byte-identical repeat, A3 never runs that iteration
(`continue`d) — and the tracker stays unchanged (the byte-identical
repeat was already accounted for by A1, no reason to re-stamp the
tracker on it).

### Most-recent-`faq_miss` tracking

The tracker holds the `ToolEvent` of the most-recent `search_knowledge`
dispatch this run whose result carried `faq_miss=false`. Refresh rules
(step 7 above):

- Successful dispatch + result has `faq_miss=false` → tracker = `recorded`
  (viable hit; the LLM still sees the hits via `accumulated_tool_results`).
- Successful dispatch + result has `faq_miss=true` → tracker = `null`
  (no viable hit; a re-search this turn is warranted; gate must allow it).
- Failed dispatch (`success=false`, external failure or malformed result
  map where `faq_miss` cannot be read) → tracker = `null` (same principle
  A1 follows: failures don't enter the cache, legitimate retries
  re-dispatch).

### Suppression + no step / budget charged

When step 4 catches a suppression candidate, the loop:

1. Builds a `ToolEvent.paraphraseSuppressed(step, call, prior.resultData(),
   prior.stepIndex())` — `latencyMs=0`, `success=true`, distinct
   `paraphraseSuppressed=true` + `faqHitAtStep=<prior step>` annotations.
2. Appends the event to `toolEvents` (so it appears on the trace) and
   puts the prior hit into `accumulatedToolResults` under
   `search_knowledge` (so the LLM sees the viable hit next turn).
3. Logs an `INFO` line naming the step indices.
4. `continue`s — `toolDispatcher.dispatch(...)` is not invoked. The
   outer `step` counter (the `maxToolSteps` budget) still increments
   per LLM call, exactly as it does after an A1 dedup; no ADDITIONAL
   tool budget is charged for the suppressed call. (Within a batch
   `tool_calls` list at the same step, multiple suppressed calls cost
   the same 0 budget — mirrors A1.)

The gate is keyed entirely on the EXISTING `faq_miss` flag (a runtime
result-state read out of the dispatched result map). NO query content
inspection, NO keyword/regex/enum/per-UC branching. The agent still
owns the FIRST search, which tool, what content (§1.3); the gate is a
structural state/cardinality backstop on the same surface §1.4 already
delegates to the Runtime (idempotency, budget, tool-schema).

## §2 Trace annotation — before / after

**Before (S-Auto-13):** the `ToolEvent` record carried only the
S-Auto-12 `deduplicated` + `originalAtStep` annotation pair; a
same-turn paraphrase re-search appeared as an ordinary
`search_knowledge` dispatch with no signal that the projection echo /
`grounding_instruction` had asked the LLM to draft from the prior hit.
report.html and the admin trace surfaced these as fresh searches.

**After (S-Auto-13b):** the `ToolEvent` record carries a NEW
`paraphraseSuppressed` + `faqHitAtStep` annotation pair, distinct from
`deduplicated` (a given event carries one or the other, never both).
`ControlKernel.persistRunResult` flattens the new pair onto the
persisted `bot_turns.tool_calls` entries:

```java
if (te.paraphraseSuppressed()) {
    entry.put("paraphrase_suppressed", true);
    entry.put("faq_hit_at_step", te.faqHitAtStep());
}
```

(mirror of the S-Auto-12 `deduplicated` flatten just above it at
`ControlKernel.java:1859-1862`).

**Verified on persisted data:** across the 3 post-fix passes
(20260602-094229 + 20260602-094655 + 20260602-095148), **17 events**
carry `paraphrase_suppressed=true` + `faq_hit_at_step=<n>` on the
persisted trace and would be rendered as suppression annotations by
report.html / admin trace. See §4 for the per-pass breakdown.

## §3 Negative controls

### (a) First `search_knowledge` of a turn NOT suppressed

Test: `AgentRunLoopFaqMissStateGateTest.firstSearchKnowledge_isNotSuppressed`.
A single `search_knowledge` call (tracker is null) — `toolDispatcher.dispatch`
is invoked exactly once; no `ToolEvent` is flagged `paraphraseSuppressed`.

### (b) Re-search after `faq_miss=true` IS dispatched

Test: `AgentRunLoopFaqMissStateGateTest.reSearchAfterFaqMissTrue_isDispatched`.
Step 0 returns `faq_miss=true` (no viable hit) → tracker remains `null`;
step 1 (different query) → tracker is `null` → dispatched. `times(2)`
real dispatches; no suppression event.

### (c) Cross-`run()` (cross-turn) gate state isolation

Test: `AgentRunLoopFaqMissStateGateTest.crossRun_gateStateIsPerRun`.
Two distinct `AgentRunLoop.run(...)` invocations with `search_knowledge`
calls and the SAME viable-hit shape — both dispatch, neither suppressed.
Confirms the tracker has a per-run lifetime and does NOT leak across
turns (and informs §6 OQ-S69.1: cross-turn paraphrase storm is a
separate fix surface).

### (d) Suppressed event carries annotation + prior hit + zero latency

Test:
`AgentRunLoopFaqMissStateGateTest.paraphraseAfterViableHit_isSuppressed_carriesAnnotationAndPriorHit`.
Step 0 viable hit, step 1 paraphrase (different query string). Exactly
ONE real dispatch + ONE `ToolEvent` annotated:

- `paraphraseSuppressed=true`, `faqHitAtStep=0` (step-0 reference)
- `latencyMs=0` (tool not re-executed)
- `success=true` (mirrors the cached viable-hit success)
- `resultData` equals the step-0 cached payload (the LLM sees the
  hits on the next turn via `accumulated_tool_results`)
- `deduplicated=false` and `originalAtStep=-1` (annotation
  distinctness from A1)

### (e) A1 (byte-identical) + A3 (paraphrase) coexistence

Test:
`AgentRunLoopFaqMissStateGateTest.a1AndA3Coexist_byteIdenticalDeduped_paraphraseSuppressed`.
Step 0 dispatch (viable hit), step 1 byte-identical repeat (A1 catches
→ `deduplicated=true, originalAtStep=0`), step 2 paraphrase (A3
catches → `paraphraseSuppressed=true, faqHitAtStep=0`), step 3 final
answer. Exactly ONE real dispatch; the two suppressed events carry
DISJOINT annotation pairs.

### (f) Re-search after dispatch failure re-dispatches

Test: `AgentRunLoopFaqMissStateGateTest.reSearchAfterDispatchFailure_isDispatched`.
Step 0 dispatch fails (`success=false`) → tracker cleared; step 1
paraphrase → dispatched (`times(2)` real dispatches; no suppression).
Confirms transient failures don't freeze the gate.

### (g) Distinct-need validation on `bad_cases` data

Manually sampled the 17 paraphrase_suppressed events across the 3
post-fix passes (`/tmp/analyze_paraphrase_storm.py` + an ad-hoc
inspector). All 17 are textbook paraphrases of the same intent —
representative samples:

- `cs011` — "password reset email not received after checking spam"
  (prior viable hit) → "not receiving password reset email after
  checking spam" (suppressed, word-order swap, same intent)
- `cs011` — same prior → "password reset email not received after
  troubleshooting" (suppressed, "checking spam" ⇄ "troubleshooting"
  synonym swap, same intent)
- `cs014` — "contact email reverted back to old one after update"
  (prior viable hit) → "contact email reverted back to old email
  account" (suppressed, "old one" ⇄ "old email account" same referent,
  same intent)

Zero of the 17 are LEGITIMATELY-DISTINCT needed second searches (a
genuinely different sub-question after a viable hit). The
STOP-and-surface condition documented in the contract was NOT
triggered; the gate's strict "always-suppress-after-viable-hit" form
is correct on this data and a count-cap relaxation is not needed.

## §4 `PARAPHRASE_STORM` 3-pass before/after + OQ-S68.4 subsumption

### Methodology

Detector: a REAL successful `search_knowledge` whose query differs
from a prior REAL successful `search_knowledge` in the SAME session
that returned `faq_miss=false`. Calibrated against the S-Auto-13
reference runs (script in `/tmp/analyze_paraphrase_storm.py`; the
script reads `eval_interactive/results/<run>/results.json`'s
`case_results[].per_turn_trace[].tool_calls[]`). To distinguish the
gate's per-run scope from the broader cross-turn pattern, the detector
breaks out WITHIN-TURN (a prior viable hit in the SAME bot turn as the
re-search — the gate's exact catch) vs CROSS-TURN (a prior viable hit
in a DIFFERENT bot turn — explicitly out of scope per per-run gate
design).

A1's `deduplicated` events and A3's `paraphrase_suppressed` events are
treated as "served from cache" and do NOT enter the detector's
"successful real-dispatch" list — so they neither inflate the
"successful searches" denominator nor contaminate the prior tracker.

### Pre-fix reference (S-Auto-13 measurement)

3-pass `bad_cases` reference runs taken at HEAD before this sub-sprint
(`20260601-180756`, `20260601-181320`, `20260601-181722`; the same
data S-Auto-13 reported as 16 / 16 / 7 in its handoff §2):

| Pass | Real successful searches | Within-turn storm | Cross-turn storm | Total storm | A1 deduped | paraphrase_suppressed |
|---|---|---|---|---|---|---|
| 1 (180756) | 25 | **6** | 9 | 15 | 7 | 0 (annotation didn't exist) |
| 2 (181320) | 19 | **5** | 6 | 11 | 12 | 0 |
| 3 (181722) | 15 | **1** | 6 | 7 | 8 | 0 |
| **Sum** | **59** | **12** | **21** | **33** | **27** | **0** |

(The handoff §2 "/32 /31 /23" denominators count successful real +
A1-deduped; the small numeric drift vs the S-Auto-13 handoff's 16/16/7
is detector-implementation-level — same magnitude and the pass-3
exact-match of 7 / 1 / 6 confirms the methodology is the same one.)

### Post-fix (3-pass, freshly-restarted backend on this commit)

Runs `20260602-094229` (pass 1), `20260602-094655` (pass 2),
`20260602-095148` (pass 3); labels
`sprint-069-s-auto-13b-pass-{1,2,3}`; `parallel=1`; same `bad_cases`
suite (12 cases × 3 passes); sim/bot temp from the default config
(unchanged from the S-Auto-13 pre-fix runs):

| Pass | Real successful searches | Within-turn storm | Cross-turn storm | Total storm | A1 deduped | paraphrase_suppressed |
|---|---|---|---|---|---|---|
| 1 (094229) | 16 | **0** | 7 | 7 | 7 | 8 |
| 2 (094655) | 15 | **0** | 5 | 5 | 6 | 6 |
| 3 (095148) | 20 | **0** | 10 | 10 | 7 | 3 |
| **Sum** | **51** | **0** | **22** | **22** | **20** | **17** |

### Reading

- **Within-turn (the gate's design surface): 12 → 0 across the 3
  passes.** §11 target ≤3 MET on every pass. This is the deterministic
  half of the hybrid doing exactly what the contract required.
- **Cross-turn (NOT the gate's design surface): 21 → 22.** Same
  magnitude, expected. The gate has a per-run lifetime (= one outer
  bot turn) and cannot, by construction, catch a paraphrase the LLM
  issues in a SUBSEQUENT bot turn referencing a prior turn's viable
  hit. Surfaced as **OQ-S69.1** (§6).
- **Total: 33 → 22.** Total dropped by 33% even though the gate is
  per-turn — the within-turn share of the pre-fix storm (~36%) was
  fully eliminated; the cross-turn share (~64%) remains as a separate
  fix surface.
- **paraphrase_suppressed annotation: 17 events visible on the
  persisted trace.** All 17 are TRUE paraphrases (distinct query
  string vs prior); zero of 17 are byte-identical (which would imply
  an A1 escape — see OQ-S68.4 subsumption below).

### OQ-S68.4 subsumption

OQ-S68.4 was "a byte-identical re-search that escaped A1's per-run
dedup". On this run, A1 caught all byte-identical repeats (20
deduplicated events across 3 passes; zero byte-identical events
appeared in the paraphrase_suppressed set). A3 is therefore not
ACTIVELY catching any A1 escape on the current bad_cases. **But A3
LOGICALLY subsumes the byte-identical-after-viable-hit case** as a
side effect: A3 keys purely on the `faq_miss` RESULT flag — it does
NOT consult the args hash and does not care whether A1 fired or
missed. So any future byte-identical that escapes A1 (e.g. a corner
case in `canonicalArgumentsHash` or a tool other than `search_knowledge`
extended to A3 later) will ALSO be caught by A3 IF the most-recent
prior `search_knowledge` this run was a viable hit. Net effect on
OQ-S68.4: "logically subsumed for the search_knowledge-after-viable-hit
shape; no separate fix required" — closing-out follow-up belongs to
the M-Auto-3 close review, not this sub-sprint.

## §5 Baselines + §7 stanza self-walk + fence disposition

### Baselines re-measured at HEAD

| Gate | Result | Status |
|---|---|---|
| Java `mvn -pl server test` | `Tests run: 1198, Failures: 1, Errors: 0, Skipped: 2` | **+6 new gate tests** vs `1192/1` baseline; sole failure is the inherited `SystemPromptUserRequestedTiebreakerTest` (unchanged) |
| eval_interactive `uv run python -m pytest --tb=no -q` | `495 passed, 8 failed` | **unchanged** vs baseline; the 8 failures are the 2026-06-01 `0323457` action_bank split (OQ-S68.1; governance/lint tests reading `action_bank.md` rows that moved to `action_bank_archive.md`; disjoint from this sub-sprint) |
| autoloop `uv run --extra dev pytest -q` | `276 passed` | unchanged |
| 17-fixture `uv run --extra dev pytest -p no:cacheprovider -q tests/test_anti_hardcode_check.py` | `31 passed` | unchanged |
| Scoring SHA (`_compute_scoring_code_sha()`) | `35305bd84402ac455b126be5bcf8b2cb3b3fa55e3399f2c02443ea74bd8704e8` | unchanged (matches the `35305bd8…` lock) |

### §7 stanza self-walk

Stanza copied from `sprint_objective.md` for the record + walked
against what shipped:

```markdown
## Layer-classification + anti-hardcode stanza

**Target failure layer:** infra (faq_miss-state-aware re-search
suppression gate). No agent semantic decision changed; the LLM still
owns the FIRST search, which tool, what content.

**Tier-0 invariant:** none added — extends the Runtime's existing
idempotency / cardinality / budget responsibility (§1.4); NOT added
to `docs/runtime_freeze_and_risk_policy.md` §1/§2. A Tier-0 elevation
argument would have triggered `human_review_required` per §3.2.

**Semantic hardcode:** none. The gate is a STRUCTURAL state /
cardinality backstop keyed on the EXISTING `faq_miss` result flag —
NOT on query content / keyword / regex / enum / per-UC; no semantic
judgment of what was searched. Justification: soft-signal-first
satisfied — S-Auto-13 shipped the soft `faq_miss`-keyed instruction +
projection echo, they FIRE (15-17 echoes/run on the pre-fix
measurement), the model ignored them (PARAPHRASE_STORM 16/16/7 vs ≤3)
→ same falsification → deterministic-backstop pattern as A1
(Sprint 19/20 → S-Auto-12). The S-Auto-13 soft layer STAYS beneath
the backstop. Net: removes wasted re-searches, adds no semantic rule.

**Generalization coverage:** target = bad_cases within-turn
PARAPHRASE_STORM (pre-fix 12 across 3 passes); neighbor = the
anchor_outcome / shadow same shape (deferred to milestone close
review — bad_cases is the gate-scope target); negative = first-search
not suppressed; faq_miss=true re-search dispatched; cross-run
isolation (per-turn lifetime); distinct-need validated on the 17
suppression events (§3 (g)); A1 + A3 coexistence; failure-retry
re-dispatch (all covered by `AgentRunLoopFaqMissStateGateTest`);
shadow = held-out (not read by dev).
```

Each field is filled in concrete, not stretched. The Tier-0 line is
explicit ("none added"); the semantic-hardcode line spells out
*why* a keyword/regex/per-UC route would have been the wrong layer
even after S-Auto-13's soft-signal falsification (the LLM still owns
the first search; the gate is keyed on a runtime result-state, not on
what was searched).

### Fence disposition

| Surface | Fence label (prompt) | Disposition |
|---|---|---|
| `AgentRunLoopImpl.java` (gate alongside A1) | in scope | EDITED — `lastSearchKnowledgeViableHit` tracker decl + step 4 gate + step 7 tracker refresh; A1 logic byte-for-byte unchanged |
| `ToolEvent.java` | in scope | EDITED — added record components `paraphraseSuppressed` + `faqHitAtStep` (12-arg canonical), 10-arg back-compat ctor (for the A1 dispatch site that builds with `deduplicated` only), 8-arg back-compat ctor (every pre-S-Auto-12 call site), `paraphraseSuppressed(...)` factory; existing `deduplicated(...)` factory + `of(...)` + `rejected(...)` unchanged |
| `ControlKernel.java` | in scope | EDITED — mirror flatten just below the S-Auto-12 `deduplicated` flatten (`bot_turns.tool_calls` entry); no other change to the kernel |
| `server/src/test/**` | in scope | ADDED `AgentRunLoopFaqMissStateGateTest.java`; existing `AgentRunLoopIdentityDedupTest` / `AgentRunLoopIdentityDedupIntegrationTest` / `ToolEventTest` unchanged + still pass |
| `KnowledgeSearchResult.java`, `SearchKnowledgeTool.java` | read-only (faq_miss source) | READ ONLY — used to confirm the `faq_miss` flag lands on the dispatched `ToolResult` data map |
| `eval_interactive/case_specs/bad_cases/**` | read-only (measurement) | READ ONLY — case specs untouched; only `results/` got new runs |
| A1 byte-identical `successfulDispatchCache` LOGIC | hard-fenced | UNTOUCHED — A3 sits AFTER A1's `continue`; A1's check, key shape, cache-put, and `deduplicated(...)` annotation are byte-for-byte unchanged |
| `PhaseEvaluator.resolveMaxStepsReason` | hard-fenced (B1) | UNTOUCHED |
| `skills/**` | hard-fenced (S-Auto-13 soft layer STAYS) | UNTOUCHED — `resolve_faq_grounded_answer.yaml`'s `grounding_instruction` paraphrase-discipline line + `ContextProjectionBuilder`'s `search_reuse_instruction` echo remain as the soft-signal-first measure beneath the deterministic backstop |
| `tool-policy.yaml` | hard-fenced | UNTOUCHED |
| `eval_interactive/case_specs/**` (incl. bad_cases content) | hard-fenced (B1 / measurement source) | UNTOUCHED |
| `user_simulator.py` | hard-fenced | UNTOUCHED |
| 4 SHA-locked scoring files | hard-fenced | UNTOUCHED (SHA verified above) |
| `loop.py` / `applier.py` / sandbox / meta_agent / cli.py / preflight.py | hard-fenced | UNTOUCHED |
| `docs/foundational/**`, `docs/current/**`, `docs/runtime_freeze_and_risk_policy.md`, `docs/teams/**` | hard-fenced | UNTOUCHED |
| Prior sprint archives (`docs/sprints/sprint-0*`) | hard-fenced (immutable) | UNTOUCHED |
| OQ-S68.1 eval split-failures | "do NOT 'fix' here" | UNTOUCHED (the 8 are still failing; they're not this sub-sprint's surface) |

## §6 Open Questions surfaced

- **OQ-S69.1 — cross-turn paraphrase storm.** A bot turn issuing a
  paraphrase of a prior turn's viable hit is OUT OF SCOPE for the
  per-run gate. Post-fix cross-turn storm is 7 / 5 / 10 (22 across 3
  passes) vs pre-fix 9 / 6 / 6 (21) — same magnitude, unchanged. A
  fix would require lifting the tracker out of `run()`-local state
  and into something `BotSession`-scoped (e.g. a per-session
  `viable_hit_index` keyed on prior turns' search results, surfaced
  back into `accumulated_tool_results` next turn). Layer would still
  be `infra`; same `faq_miss` result-state key; no semantic content
  change. **Recommended carrier: a future sub-sprint** (M-Auto-3
  close review can decide whether this belongs in the milestone close
  scope or in a follow-up substrate-hygiene sub-sprint).
- **OQ-S68.4 (S-Auto-13 inheritance) — disposition: LOGICALLY
  SUBSUMED by A3.** A byte-identical re-search that escapes A1 AND
  has a prior viable hit this run is now caught by A3 on the
  `search_knowledge` surface. Not actively triggered in this run (A1
  caught all 20 byte-identical events). No separate fix required;
  closing-out belongs to the M-Auto-3 close review.
- **OQ-S68.1 (S-Auto-13 inheritance) — disposition: UNCHANGED,
  DISJOINT.** The 8 eval_interactive failures from the 2026-06-01
  `0323457` action_bank split remain; not this sub-sprint's surface.
- **OQ-S68.2 (S-Auto-13 inheritance) — disposition: UNCHANGED.** A
  separate "UC not committed" symptom; not surfaced or addressed
  here.
- **OQ-S68.3 (S-Auto-13 inheritance) — disposition: RESOLVED by this
  sub-sprint.** S-Auto-13 surfaced OQ-S68.3 as the falsification of
  the A3 SOFT layer; the deterministic backstop shipped here closes
  it on the within-turn surface. The soft layer stays in place
  beneath the backstop (correctly, per soft-signal-first).

## §7 Self-check tick-off

- [x] `faq_miss`-state gate added in `AgentRunLoopImpl` ALONGSIDE A1
      (A1 logic UNCHANGED); tracks most-recent `search_knowledge`
      `faq_miss` per run; suppresses same-turn re-search after
      `faq_miss=false`; serves prior hit; no step/budget charged.
- [x] Exception: re-search after `faq_miss=true` is ALLOWED (test (b)).
- [x] Trace `paraphrase_suppressed:true` + `faq_hit_at_step`
      (distinct from A1's `deduplicated`); flattened onto `tool_calls`
      (17 events visible on the persisted post-fix data).
- [x] Negative controls pass: first search not suppressed; `faq_miss=true`
      re-search dispatched; cross-run not suppressed; distinct-need
      validated on the bad_cases (all 17 suppressions are true
      paraphrases — STOP-and-surface NOT triggered); A1 coexistence holds.
- [x] `PARAPHRASE_STORM` 16/16/7 → **0/0/0 within-turn** on a 3-pass
      bad_cases rerun (backend restarted); cross-turn unchanged and
      surfaced as OQ-S69.1; OQ-S68.4 subsumption checked.
- [x] The S-Auto-13 A3 soft layer was NOT removed (stays beneath the
      backstop — `resolve_faq_grounded_answer.yaml`'s
      `grounding_instruction` paraphrase-discipline line +
      `ContextProjectionBuilder`'s `search_reuse_instruction` echo
      both still in place).
- [x] Java no NEW failures beyond `1192/1/0/2` (+ 6 new tests →
      `1198/1/0/2`); did NOT touch the OQ-S68.1 eval split-failures.
- [x] eval_interactive `495/8` + autoloop `276` + 17-fixture `31` +
      scoring SHA `35305bd8…` preserved.
- [x] No edits to A1 logic / PhaseEvaluator / skills / case_specs /
      user_simulator / loop.py / scoring / applier.py / sandbox /
      meta_agent; no Tier-0 self-invented; no semantic-content keying.
- [x] No `git add -A`; autoloop run on a clean committed tree
      (this sub-sprint did not require autoloop, but the discipline
      was preserved for the code + handoff commits); backend restarted
      for measurement; local-Mac only.
- [x] Handoff §0–§7 filled; per-sub-sprint Codex dispatch status
      noted (PENDING — deliver-agent at close).
