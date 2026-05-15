---
title: Sprint 25 — Per-LLM-call latency instrumentation (R-per-llm-call-latency-instrumentation)
doc_tier: current-runtime
status: current
implementation_status: not_started
source_of_truth: this file
last_reviewed: 2026-05-14
review_cadence: per sprint
supersedes: [docs/sprints/sprint-024-objective.md]
superseded_by: null
notes: >
  Single-track, single-R-item sprint. Implements
  `R-per-llm-call-latency-instrumentation` per Sprint 24 §4.5 acceptance
  bar: persist per-LLM-call duration on every smoke run + produce a
  reproducible synthetic baseline that isolates LLM round-trip from tool
  dispatch and persistence overhead. PREREQUISITE deliverable for any
  future deadline-budget widening or model-revert decision (Sprint 24
  §4.5 prerequisite-flag). Layer: `infra` / eval-harness. NO deadline
  budget edit. NO model config edit. NO acting on the data this sprint
  (the comparison and decision belong to a future sprint).

  Two surface options the dev walks before committing: (Option A) Java-
  side capture in `LlmInvocationService.invokeChat` persisted into a
  new structured field that flows to `results.json`; (Option B)
  writer-side enrichment in `eval_interactive/eval_interactive/batch/executor.py`
  that surfaces the existing `LlmCallLogger`-emitted timing (which is
  already persisted to the `llm_call_log` DB table per V9 migration)
  into a structured per-turn duration field on `results.json`. The dev
  walks both, picks one, records the justification in handoff §3 (per-
  option file lists in §5 below). The dev also resolves Sprint 24 §10
  Q1 methodology — naming where the planning-turn numbers (pre
  p95≈24.6s / post p95≈27.6s / n="105+27 case-turns") might have come
  from, OR honestly recording "source unknown / cannot be reconstructed".

  Reproducibility bar primary per
  `.claude/agent-memory/sprint-deliver-orchestrator/feedback_deliver_agent_cited_numbers_must_be_reproducible.md`:
  every quantitative claim in the handoff (latency numbers, percentiles,
  sample counts) MUST cite source path + extraction command. Sprint 25's
  whole deliverable is reproducible methodology; the same standard
  applies to the dev's own evidence.

  §7 stanza form: single-layer prospective `infra`. NOT exempt from §7
  (the sprint touches a runtime semantic surface adjacent — `LlmInvocationService`
  is on the runtime side — even though the instrumentation itself is
  side-channel observability, the eval-harness writer is on the eval
  contract surface; either option is a semantic-touching change for §7
  scoping purposes, so the stanza is required).
---

# Sprint 25 — Per-LLM-call latency instrumentation

## 1. Sprint name

Sprint 25 — Per-LLM-call latency instrumentation (R-per-llm-call-latency-instrumentation)

## 2. Goal

Implement `R-per-llm-call-latency-instrumentation` (Sprint 24 §4.5 +
`docs/action_bank.md:616`) so that per-LLM-call duration is persisted
on every smoke run via a reproducible source path + extraction command,
and a fixed-prompt synthetic baseline isolates LLM round-trip from
tool dispatch and persistence overhead. The instrumentation is the
PREREQUISITE to any future deadline-budget widening or model-revert
decision (Sprint 24 §4.5 prerequisite-flag); without it, the coarse
proxy that Sprint 24 Track B surfaced is a signal, not evidence.

Sprint 25 does NOT act on the data. It produces the instrument; a
future sprint produces the decision.

## 3. Premise verification (deliver-agent planning turn, 2026-05-14)

Confirmed before drafting:

- `server/src/main/java/com/gumtree/csagent/service/runtime/LlmInvocationService.java`
  exists. Method `invokeChat(String, String, String, int)` at lines
  92–160. `LlmDeadlineExceededException` is caught at line 120 and
  re-thrown at line 132 after `llmCallLogger.logFailure(...)` (line
  130). Success-path: `elapsed` captured at line 110, log emit "LLM
  [chat] response: latency=…ms" at line 111, `llmCallLogger.log(...)`
  at line 114. (Sprint 24 §4.3's `:116` citation is one statement off
  but the timing facts hold.)
- `server/src/main/java/com/gumtree/csagent/service/observability/LlmCallLogger.java`
  exists. Methods `log(...)` (line 37) and `logFailure(...)` (line 71)
  both persist `latency_ms` to the `llm_call_log` table.
- `server/src/main/resources/db/migration/V9__create_llm_call_log.sql`
  creates `llm_call_log(id, session_id, turn_index, call_type, model,
  prompt_tokens, completion_tokens, latency_ms INT NOT NULL,
  request_summary, response_summary, success, error_message,
  created_at)` with an index on `(session_id, turn_index)`. **Per-LLM-
  call latency is already in the DB on every call**; the gap is
  surfacing it into the eval-harness `results.json`.
- `eval_interactive/results/20260514-080835/results.json` confirms the
  current schema. Top-level: `run_id`, `label`, `timestamp`,
  `elapsed_ms`, `summary`, `case_results`. Per-case: `case_id`,
  `session_id`, `total_turns`, `elapsed_ms` (case-level), `transcript`
  (per-turn: `turn_index`, `role`, `message`, `source`). No per-turn
  or per-LLM-call duration. Writer at
  `eval_interactive/eval_interactive/batch/executor.py`
  `_build_case_result` (line 282) + `_save_results` (line 585).
- Sprint 24-landed surfaces stable: `BotSession.consecutiveDeadlineCount`
  at line 81; `Sprint24DeadlinePlaceholderCoalesceTest.java` at
  `server/src/test/java/com/gumtree/csagent/service/runtime/`.
- `docs/sprint_objective.md` did NOT exist at session start (Sprint 24
  archived to `docs/sprints/sprint-024-objective.md` in `1541e00`);
  this file is written from scratch.

If the dev agent finds the premise has drifted (a code path moved,
the schema changed, the DB table contents are inaccessible), STOP and
report rather than improvise.

## 4. Surface options — dev walks both before committing

### Option A — Java-side instrumentation

Capture per-call duration in `LlmInvocationService.invokeChat` and
persist into a new structured field that flows through
`AgentRunLoop` → `SessionRunner` → the per-turn transcript entry in
`results.json`. The `elapsed` variable already exists (line 110);
the change is to thread it into a new persisted slot on the turn
record (and have the eval harness surface it).

Pros: change is at the originating call site; symmetric across
success and `LlmDeadlineExceededException` paths (both compute
`elapsed`).

Cons: touches Java + plumbing across `AgentRunLoop` /
`SessionRunner` / `TraceCollector` to land in `results.json`. Higher
blast radius.

### Option B — Writer-side enrichment from existing DB log

Query the `llm_call_log` table (or re-emit the existing
`LlmCallLogger.log(...)` payload through a side channel) and have
the eval harness writer (`executor.py` `_build_case_result` line
282) attach a per-turn `llm_call_latency_ms` field on each
transcript entry by joining on `(session_id, turn_index)`.

Pros: lower blast radius. The data already exists in the DB. Pure
eval-harness change.

Cons: requires the eval harness to either (a) read the DB
directly, OR (b) expose a new bot-side endpoint that returns the
log rows for a session, OR (c) read a sibling artefact emitted by
the bot at session-end. The dev picks the cleanest sub-shape.

### Decision rule

The dev walks both. Picks one. Writes the justification in handoff
§3 (Option chosen, rejected alternative + why). Either choice is
acceptable to the deliver agent and Codex; what is NOT acceptable
is shipping without having considered both. Sprint 24 §4.5 named
both options explicitly; the dev must honor the deliberation.

## 5. Synthetic baseline design

Sprint 24 §4.5 names the requirement: "a worked example comparing
post-`f2d4cb2` per-call p50/p95 against a fixed-prompt synthetic
baseline that isolates LLM round-trip from tool dispatch /
persistence overhead." The dev designs this baseline. Constraints:

- **Fixed prompt.** A single deterministic prompt (the dev picks
  the wording; the prompt itself is not subject to §1.7
  forbidden-list because it is not a runtime prompt — it is a
  measurement instrument).
- **No tool dispatch.** The baseline measures one LLM round-trip;
  the test harness must not call `search_knowledge`,
  `resolve_article`, `get_customer_context`, etc.
- **No persistence overhead.** The baseline must not write to
  `bot_sessions` / `bot_turns` / `llm_call_log` (or if it does,
  the dev measures and subtracts the persistence delta).
- **Reproducible.** The handoff must cite the exact command (e.g.
  `mvn test -Dtest=LlmSyntheticBaselineTest` or `python -m
  eval_interactive.scripts.llm_synthetic_baseline`) and the source
  path of the script / test. n ≥ 30 samples per percentile claim.

Implementation location is the dev's choice:

- A JUnit benchmark test under `server/src/test/java/...` (e.g.
  `LlmSyntheticBaselineTest.java`), OR
- A Python script under `eval_interactive/scripts/` (e.g.
  `llm_synthetic_baseline.py`), OR
- A new Python module under `eval_interactive/eval_interactive/scripts/`.

The dev records the choice + justification in handoff §4.

## 6. Pre-`f2d4cb2` comparison conditional

Sprint 24 §4.5 names the conditional: "compare against pre-`f2d4cb2`
if the historical LLM-call log is still queryable, or take the
current-model baseline as the new ground truth."

The dev verifies whether the local DB (or any preserved snapshot)
contains `llm_call_log` rows from pre-`f2d4cb2` calls. If yes, the
comparison is a delta (pre p50/p95 vs post p50/p95). If no, the
current-model baseline becomes the new ground truth and the
comparison is absolute (post-`f2d4cb2` synthetic baseline + post-
`f2d4cb2` smoke distribution). Either outcome is acceptable; the
dev names the path taken in handoff §5 with the verification
evidence (a `psql -c "SELECT MIN(created_at), MAX(created_at) FROM
llm_call_log;"` invocation + its literal output is a fine record).

## 7. Methodology reconciliation (Sprint 24 §10 Q1)

The Sprint 24 dev's independent extraction over `results.json`
case-level `elapsed_ms` yielded pre p50≈19.9s / p95≈30.5s vs post
p50≈41.1s / p95≈92.3s (n_cases=14 each). The planning-turn citation
in Sprint 24 `docs/sprint_objective.md` §2 was pre p50≈10.4s /
p95≈24.6s vs post p50≈10.5s / p95≈27.6s, n="105 + 27 case-turns".
Both directions agree (p95 widened); magnitude diverges ~20x. The
"105+27" n cannot be reconstructed from `case_results[].elapsed_ms`
+ `total_turns` (case count 14+14, sum_turns 32+42, transcript
entries 66+82, bot turns 34+40).

The Sprint 25 dev investigates where the planning-turn numbers
might have come from. Hypotheses to walk (in order):

1. `llm_call_log` DB table rows (the most likely source since
   per-LLM-call latency exists there). The dev queries the table
   for the relevant timeframes and checks whether the planning-turn
   numbers fall out.
2. An internal log file (e.g. application log from `LlmInvocationService`'s
   `log.info("LLM [chat] response: latency=…")` emit) that may or
   may not still be present in the repo.
3. A different aggregation methodology on the same `results.json`
   files (e.g. per-turn extraction from transcript timestamps if
   any).
4. The numbers were carried forward without methodology from an
   earlier deliver-agent turn and are not reconstructable; mark as
   "source unknown / cannot be reconstructed."

The dev records the determined source (or honest "unknown") in
handoff §6. This is NOT a blocker for the sprint — it is the
methodology honesty point per Sprint 24 §10 Q1.

The dev's own latency numbers (synthetic baseline + post-`f2d4cb2`
smoke distribution) MUST cite source path + extraction command per
§9 reproducibility bar.

## 8. Files in scope (preliminary; refined during dev's option walk)

### If Option A chosen

- `server/src/main/java/com/gumtree/csagent/service/runtime/LlmInvocationService.java`
  (add per-call duration to the persisted call record)
- Plumbing through `AgentRunLoop` / `SessionRunner` / `TraceCollector`
  as needed for the duration to reach `results.json`
- `eval_interactive/eval_interactive/batch/executor.py`
  `_build_case_result` to surface the new field
- (Possibly) bot-side `TraceCollector` / trace endpoint changes
- Synthetic baseline test or script (location per §5)
- JUnit / Python regression test asserting the field is present on
  every smoke run

### If Option B chosen

- `eval_interactive/eval_interactive/batch/executor.py`
  `_build_case_result` (line 282) attaches the new per-turn field
- New query path to read `llm_call_log` by `(session_id, turn_index)`
  — either direct DB read from the eval harness or a new bot
  endpoint exposing the log rows
- (Possibly) `server/src/main/java/com/gumtree/csagent/repository/LlmCallLogRepository.java`
  if a new query method is needed
- Synthetic baseline test or script (location per §5)
- Python regression test asserting the field is present on every
  smoke run

### Bundle policy

Sprint 25 is a bundled instrumentation sprint: Java + Python may
both touch, and at least one regression test is required. The
bundle is `infra` per §3.1; per §3.3 of `iteration_governance.md`,
infra changes do not require Tier-0 protection because they do not
encode semantic decisions — they expose observability.

## 9. Success metrics (reproducibility bar primary)

The sprint is accepted when ALL of the following hold:

- Instrumentation captures per-LLM-call latency on every smoke run.
  Verified by running one smoke rerun (smallest defensible — see
  §10) and confirming the new field is populated on every transcript
  entry / case in the produced `results.json`.
- The synthetic baseline produces reproducible numbers. The handoff
  cites the exact command + source path + the literal output. n ≥
  30 samples per percentile claim.
- The worked-example comparison (per Sprint 24 §4.5 acceptance bar)
  cites methodology: which numbers came from the synthetic
  baseline, which from the smoke rerun, the exact extraction
  command for each percentile in the comparison, and whether the
  comparison is "delta vs pre-`f2d4cb2`" or "absolute current-model
  baseline" per §6.
- Sprint 24 §10 Q1 methodology question is answered: the handoff
  names the source of the planning-turn numbers (DB rows, log file,
  derivation methodology, or honest "source unknown / cannot be
  reconstructed").
- The full `server/` test suite is green (Track A surfaces from
  Sprint 24 + the new instrumentation surfaces).
- No measurable overhead regression introduced by the
  instrumentation itself (the dev measures by comparing the
  pre-instrumentation smoke run elapsed time at the case level
  against a post-instrumentation rerun on the same cases; the delta
  attributable to instrumentation should be at the noise floor —
  document it).

### Generalization-coverage breakdown

- **target** — smoke rerun captures per-call latency on all 14
  cases in the smoke set (full smoke set is one defensible run, see
  §10).
- **neighbor** — `server/` test suite green; eval-harness tests
  green (Python `pytest eval_interactive/`).
- **negative** — instrumentation overhead at the noise floor; no
  case's `case_passed` flips because of instrumentation.
- **shadow** — deferred to G2; no shadow set exists today.

## 10. Smoke rerun: real LLM, smallest defensible

Per
`.claude/agent-memory/sprint-deliver-orchestrator/feedback_mocked_llm_cannot_prove_prompt_causal_change.md`:
the smoke rerun MUST use real LLM calls. A mocked LLM cannot prove
that the latency instrumentation captures real round-trip duration
because the mock controls the variable being measured.

Smallest defensible smoke set:

- One rerun of the full 14-case smoke (matching the Sprint 24
  Track B baseline pre/post extraction) so the new instrumentation
  surfaces on the same case shape Sprint 24 measured. If the dev
  has a defensible reason to run smaller (e.g. parallelism / cost
  budget), the dev records the justification in handoff §7 and the
  reduced n is explicit in every quantitative claim.

The dev does NOT need to rerun the historical pre-`f2d4cb2` smoke
unless the DB has been wiped and the comparison path is the
"absolute current-model baseline" (see §6).

## 11. Layer-classification + anti-hardcode stanza (§7, prospective, single-layer)

**Target failure layer:** `infra` (per `iteration_governance.md`
§3.1: "orchestration, transport, persistence, timeouts, OOM,
endpoint / credential / config wiring. Owns the run loop not
crashing."). Instrumentation that persists per-LLM-call duration
on a side channel + a synthetic baseline that measures the
infrastructure overhead is squarely `infra`. The instrumentation
exposes observability for a future decision; it does NOT alter
any semantic choice (UC routing, drift detection, escalation
posture, follow-up policy).

**Tier-0 invariant:** This sprint adds no Tier-0 invariant. No
deterministic kernel-level Runtime guarantee from §1.4 is created
or modified.

**Semantic hardcode:** No semantic hardcode introduced. The
instrumentation adds no keyword / regex / if-else / enum / per-UC
matrix to runtime, prompt, or eval. The synthetic baseline's
fixed prompt is a measurement instrument, not a runtime prompt
(it never executes in production). The threshold-based behaviour
from Sprint 24 (`consecutiveDeadlineCount` count < 2 vs count >= 2)
is not modified — instrumentation reads downstream of that
behaviour, not upstream.

**Generalization coverage:** target = 14-case smoke rerun
captures per-call latency on every case. neighbor = `server/`
test suite green + eval-harness test suite green. negative = no
measurable overhead regression on case `elapsed_ms` attributable
to the instrumentation; no case `case_passed` flips because of
instrumentation. shadow = deferred to G2.

## 12. Not implemented this sprint (anti-scope-creep fence)

- **No deadline-budget widening.** Sprint 24 §4.5 prerequisite
  flag governs: without instrumentation evidence, budget decisions
  are not justified. This sprint produces the instrument, not the
  decision.
- **No model config change / model revert.** Same reason; the
  decision is a future sprint.
- **No `prompt_projection` work.** No new projected slot, no
  signal surface change, no prompt edit.
- **No eval-spec edits.** No `case_specs/*` edits, no
  `case_spec_overrides.yaml` edits, no personas edits, no judge
  rubric edits.
- **No Tier-0 changes.** Per §11 above.
- **No edits to Sprint 24-landed surfaces:**
  `BotSession.consecutiveDeadlineCount`,
  `SessionManager.consecutiveDeadlineCount(0)` builder line,
  `PhaseEvaluator` reset hook (lines 684–693), `PhaseEvaluator`
  `DEADLINE_EXCEEDED` branch threshold-gated message (lines
  765–803), `PhaseEvaluator` `LLM_UNAVAILABLE` branch (lines
  804–817), `V13__add_consecutive_deadline_count.sql`,
  `Sprint24DeadlinePlaceholderCoalesceTest.java`.
- **No edits to Sprint 23-landed surface:** `system_prompt.txt`
  teaching paragraph (the `already_called` paragraph).
- **No edits to** `AlreadyCalledPromptConsumptionTest.java`.
- **No edits to** `docs/sprints/sprint-024-*` or any
  `docs/sprints/*` archive or any `docs/archive/*` file.
- **No edits to** `docs/current/iteration_governance.md`,
  `docs/current/doc_governance.md`,
  `docs/current/agent_context_guide.md`,
  `docs/runtime_freeze_and_risk_policy.md`, or any
  `docs/foundational/*` file.
- **No action_bank §5.2 edits beyond updating
  `R-per-llm-call-latency-instrumentation` status to
  `implemented`** on close (and the deliver agent does the close
  update, not the dev).
- **No `csagent_system_design_review.md` edits** (pre-existing
  working-tree mod; not authored by this sprint).
- **No acting on the data this sprint.** The dev surfaces the
  numbers, names the methodology, and stops. A future sprint
  decides whether to widen the budget, revert the model, etc.

If the dev encounters a gap that would require widening any of
these fences to fix correctly, the dev STOPs and records the gap
as a new R-item in `docs/action_bank.md` §5.2 (deferred) and
reports to the human via handoff §7.

## 13. Reproducibility bar (every quantitative claim)

Per
`.claude/agent-memory/sprint-deliver-orchestrator/feedback_deliver_agent_cited_numbers_must_be_reproducible.md`:
every quantitative claim in the handoff (latency numbers,
percentiles, sample counts, overhead deltas) MUST cite:

1. The exact source path (e.g.
   `eval_interactive/results/<run_id>/results.json` or the literal
   `psql` output or the literal `jq` output).
2. The exact extraction command (the literal `jq` filter, the
   literal `psql -c "..."` invocation, the literal `python -c "..."`
   invocation, or "manual eyeball" if no scripted extraction was
   used).
3. The aggregation window if the source has multiple plausible
   ones (per-case, per-turn, per-LLM-call). "p95 over
   `llm_call_log.latency_ms` filtered by `call_type='chat'`" is
   unambiguous; "p95 over the smoke run" is not.
4. The derivation of any n-value (`COUNT(*)` from
   `llm_call_log` vs `len(case_results)` vs `sum(total_turns)` —
   these are different counts).

A claim that does not satisfy 1–4 is methodologically
unsupported and Codex will block on it.

## 14. Bundle policy + Codex scope-drift guidance

This sprint is a bundled instrumentation sprint. Java + Python
both touch (per Option A or B). One regression test is required.
Codex should NOT flag Java + Python in the same commit as scope
drift — the bundle is in scope.

What IS scope drift: edits to any file listed in §12 (not
implemented). Codex blocks on those.

## 15. Review-rule pointer

Codex runs the §4.1 Anti-Hardcode kernel verbatim as loaded from
`iteration_governance.md`. Sprint 25 is NOT exempt — instrumentation
is `infra`, and §4.1's exemption list ("pure infra, docs-only,
config-governance, characterization-test PRs") names "pure infra"
narrowly (e.g. CI config, container builds). A code change that
adds a new field to a persisted artefact consumed by downstream
eval is on the eval contract surface, so §4.1 applies. Codex
walks the 9 questions; instrumentation should pass cleanly because
no semantic decision moves from LLM to Java.

Codex also runs the Sprint-25-specific checks from
`compact/sprint-025-review-prompt.md`: hard-fence violations,
reproducibility-of-quantitative-claims, methodology reconciliation,
real-LLM smoke rerun (not mocked).

## 16. Pointer to handoff

The dev agent writes `docs/sprints/sprint-025-handoff.md` per the
12-section contract in
`compact/sprint-025-dev-prompt.md` §10 on completion.

---

## Sprint 25 fix iteration

### Status

Codex re-review of commit `1541e00..1b54b14` returned
`decision: fix_required / blocking_count: 1`. Anti-hardcode kernel
`approve`, all 8 hard fences hold, smoke artefact verified real (per
`docs/codex-findings.md` lines 9–12). The single blocking finding
(`docs/codex-findings.md` Finding 1, lines 14–89) is **reproducibility
hygiene on the handoff**: several quantitative claims in handoff
sections 5, 6, and 11/12 fail the Sprint 25 reproducibility bar
because they are not paired with executable extraction commands.

This fix iteration closes that single blocker. Sprint 25's substantive
deliverable (per-LLM-call latency instrumentation) is unchanged; the
data is real per Codex's smoke-artefact check; the methodology
reconciliation in handoff §6 stands. Only the in-handoff citations are
incomplete.

### Sources

- `docs/codex-findings.md` Finding 1 (lines 14–89) — the blocker
  evidence and required-fix list.
- `docs/sprints/sprint-025-handoff.md` — the four cited line ranges:
  186–216 (DB query with placeholder arrays + uncited driver),
  172–184 vs 226–231 (chat-only extraction command vs rerank table
  with no command), 237–239 (derived deltas from incomplete recipes),
  339 + 351 (overhead-delta claim without mean-computation command
  and without Sprint 24 source path for the 52.9s comparison number).
- `eval_interactive/results/20260505-235231/results.json` (pre-
  `f2d4cb2` smoke, 14 sessions).
- `eval_interactive/results/20260510-134558/results.json` (post-
  `f2d4cb2` smoke, 14 sessions).
- `eval_interactive/results/20260514-111724/results.json` (Sprint 25
  smoke rerun, 14 sessions, new `llm_calls` field).
- `docs/sprints/sprint-024-handoff.md` §4.1 — the source path for the
  52.9s mean comparison cited at handoff §11–12.

### Implement only (handoff-edit-only fix)

Augment `docs/sprints/sprint-025-handoff.md` with the missing
extraction commands. The fix is **pure documentation hygiene**: no
code changes, no new smoke run, no data revision. The data the
handoff cites is correct per Codex's own non-blocking checks; the
gap is that the recipes for extracting that data are not reproducible
as written. Specifically:

1. **Handoff §5.2 placeholder arrays + driver script.** Replace both
   `ARRAY[<14 session_id values, extracted via jq>]` strings (lines
   198, 209) with the literal session_id arrays. Add the `jq` command
   that extracts them from each pre/post `results.json` file. Inline
   the `python3 + psql` driver script (or replace it with a
   self-contained `psql ... <<'SQL' ... SQL` here-doc that takes the
   arrays as literals; either shape is acceptable provided the
   reader can paste it and reproduce the numbers).
2. **Handoff §5.2 rerank extraction command.** Add the sibling
   `python3 -c '...'` command for the rerank row (filter
   `callType == "rerank"` instead of `"chat"`), alongside the
   existing chat-only command at lines 172–184. The Sprint 25 smoke
   rerank row (n=176, p50=690, p95=998, max=1755 at line 231) must
   be regenerable from the named command.
3. **Handoff §11–12 mean-computation command.** Add an executable
   one-shot that computes the Sprint 25 smoke case-level mean
   `elapsed_ms` (a `jq '... | add / length'` filter, or a
   `python3 -c 'import json, statistics; ...'` invocation — either
   works). Cite the Sprint 24 source path explicitly for the 52.9s
   figure: `docs/sprints/sprint-024-handoff.md` §4.1 (the 14
   case-level `elapsed_ms` values are listed there; the underlying
   `results.json` source is
   `eval_interactive/results/20260510-134558/results.json`).
4. **Handoff §5.3 derived deltas (lines 237–239).** Once the
   upstream extractions (§5.2 chat + rerank + DB query) become
   reproducible, the derived deltas ("p95 widened pre→post-`f2d4cb2`:
   8.4s → 11.7s → +3.3s", "Sprint 25 smoke lands at p95=10.0s",
   "synthetic baseline subtraction ≈9s") inherit reproducibility.
   No separate command is required — the dev confirms by re-running
   the cited recipes and validating the lines 218–224 table + lines
   228–231 table + line 233 baseline figures.

The dev appends a `## Fix iteration` section to the end of
`docs/sprints/sprint-025-handoff.md` (NOT a new file) documenting
the augmentations. Same convention as Sprint 21 / Sprint 23 fix.

### Do not implement (anti-scope-creep fence)

- **No new latency measurements.** The data Codex confirmed in
  handoff §5.2 (the pre/post DB table and the Sprint 25 smoke
  rerank/chat rows) is correct. The fix iteration adds recipes,
  not new numbers. If the dev's reproduction of the recipes
  surfaces a number that disagrees with the table, **STOP and
  report** rather than silently overwriting the table.
- **No re-running the smoke.** `eval_interactive/results/20260514-111724/results.json`
  stands as the Sprint 25 smoke artefact. Codex verified it has
  real LLM calls. Do not generate a new smoke run.
- **No code edits.** The Sprint 25 commit range `1541e00..1b54b14`
  is final. `eval_interactive/eval_interactive/batch/executor.py`,
  `eval_interactive/eval_interactive/simulator/agent_client.py`,
  `eval_interactive/tests/test_executor_llm_calls_enrichment.py`,
  `server/src/test/java/com/gumtree/csagent/service/runtime/LlmSyntheticBaselineTest.java`
  — all out of scope for this fix iteration.
- **No edits outside `docs/sprints/sprint-025-handoff.md`.** The
  fix is contained to that single file.
- **All Sprint 25 parent fences still apply.** No deadline-budget
  config, no model config, no `prompt_projection` work, no
  eval-spec edits, no Tier-0 changes, no edits to Sprint 24-landed
  surfaces (`BotSession.consecutiveDeadlineCount` etc.), no edits
  to Sprint 23 `system_prompt.txt`, no edits to
  `AlreadyCalledPromptConsumptionTest.java`. See §12 of the parent
  objective above for the full fence list.
- **No `docs/codex-findings.md` edit.** Codex re-writes that file
  on fix re-review.
- **No `docs/action_bank.md` edit.** Deferred items, if any (none
  expected from this fix), are appended by the deliver agent at
  final sprint close.
- **No sprint archive edit.** `docs/sprints/sprint-024-*` and any
  prior archive remain read-only.

### Target cases / regression coverage

The "target" for this fix iteration is the four cited handoff line
ranges (186–216, 172–184/226–231, 237–239, 339/351). The
"regression" check is that the existing tables at lines 218–224,
228–231, and 133–136 (synthetic baseline) remain unchanged after
the dev reproduces them from the new extraction commands. If a
table cell disagrees with the reproduction, that is a STOP signal,
not a license to update the cell.

### Success metrics

The fix iteration is accepted when ALL of the following hold:

- Each of the four cited handoff line ranges carries an executable
  source-path + extraction-command pair per the Sprint 25
  reproducibility bar (parent objective §13).
- The literal `jq` / `python3` / `psql` invocations in the handoff
  are reproducible: a reviewer pasting them into a shell against
  the named source files reproduces the cited numbers exactly (or
  to the run-to-run variance the handoff already documents at
  §5.3 line 238).
- The Sprint 24 source path for the 52.9s comparison mean is
  cited explicitly (`docs/sprints/sprint-024-handoff.md` §4.1 +
  the underlying `results.json`).
- No code edits in the fix commit.
- No edits to files outside `docs/sprints/sprint-025-handoff.md`.
- Sprint 25 substantive verdict (the §12 sprint-objective-met
  checks) is unchanged.

### Review rule

The fix re-review is **bounded**: Codex confirms (a) each of the
four cited gaps is closed, (b) no code change in the fix commit,
(c) no edits to surfaces outside `docs/sprints/sprint-025-handoff.md`,
(d) no Sprint 25 parent fence violation. Codex does NOT re-evaluate
the substantive Sprint 25 verdict (anti-hardcode kernel + hard
fences + smoke artefact) — those were `approve` / clean in the
original review (per `docs/codex-findings.md` lines 9–12).

Codex writes a fresh §4.2 sprint-close header at the top of
`docs/codex-findings.md`, replacing the existing fix-required
header in place. New header: `## Sprint Review Decision (Sprint
25 fix re-review)`.

### §7 Layer-classification + anti-hardcode stanza (fix-iteration form)

This is a fix iteration, not a new sprint. The per-case §3.2 walk
does not apply (no new failure is being diagnosed). The fix
iteration inherits the parent Sprint 25's single-layer prospective
classification:

**Target failure layer:** `infra` (unchanged from parent §11). The
fix touches `docs/sprints/sprint-025-handoff.md` only — the
infra/eval-harness handoff document. No semantic surface, no
prompt, no runtime decision.

**Tier-0 invariant:** This fix iteration adds no Tier-0 invariant
(parent already declared none added).

**Semantic hardcode:** No semantic hardcode introduced (parent
already declared none introduced; the fix iteration adds no
keyword / regex / if-else / enum / per-UC matrix anywhere — it
adds extraction recipes to a handoff document).

**Generalization coverage:** Not applicable to a documentation
hygiene fix. The parent Sprint 25's target / neighbor / negative /
shadow coverage stands as recorded in the handoff §11.

### Deliverables

- One commit on branch `design-v1-without-human-review` editing
  only `docs/sprints/sprint-025-handoff.md`.
- A `## Fix iteration` H2 section appended to that file documenting
  the four augmentations (the four cited gaps now closed).
- Updated handoff text at the four cited line ranges: literal
  session_id arrays, full driver script (or self-contained psql
  here-doc), rerank extraction command, mean-computation command,
  Sprint 24 source-path citation for 52.9s.

At fix re-review close (after Codex re-review passes), the deliver
agent archives `docs/sprint_objective.md` (which contains both the
parent objective and this fix iteration section) to
`docs/sprints/sprint-025-objective.md`, and `docs/codex-findings.md`
to `docs/sprints/sprint-025-codex-review.md`. The dev's fix-
iteration appendix is preserved in the closed
`docs/sprints/sprint-025-handoff.md` archive.
