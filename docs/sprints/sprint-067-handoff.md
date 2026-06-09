---
title: Sprint 067 / S-Auto-12 / M-Auto-3 — Handoff
doc_tier: sprint-archive
status: archived
implementation_status: implemented
source_of_truth: this file
last_reviewed: 2026-06-01
review_cadence: ad hoc
notes: >
  S-Auto-12 sub-sprint 2 of M-Auto-3 (Substrate-hygiene — clean the autoloop
  fitness signal). Layer: infra (tool-dispatch idempotency 回挡) +
  prompt_projection (already_called observation→binding soft signal) + trace
  annotation. §7 stanza REQUIRED (semantic-touching: it constrains how many
  tool steps the LLM spends); self-walked in §5. Goal: cut the byte-identical
  IDENTICAL_RETRY storm at bot_temp=0 by shipping a HYBRID dedup — (A1-backstop)
  a per-run (toolName|canonicalArgumentsHash) cache that serves a success==true
  byte-identical repeat from cache without re-dispatching, + (A1-hybrid) an
  upgrade of the already_called projection slot from observation-only to a
  binding soft signal. Both halves mandatory (red line #2: soft-signal-alone
  was empirically falsified — Sprint 19 chose soft-first, Sprint 20 shipped the
  slot, the storm persisted). NO Tier-0 added (A1 extends the Runtime's existing
  idempotency/persistence responsibility, Constitution §1.4). NO semantic
  hardcode (dedup key reuses the existing order-insensitive canonicalArgumentsHash
  for every tool — no keyword/regex/enum/per-UC matrix). Hard fences honored:
  no edits to PhaseEvaluator / skills / case_specs / user_simulator / loop.py /
  scoring files / meta_agent / sandbox / governance docs / archives. Gates:
  Java 1186/10/0/2 (+3 new tests; the 10 failures are the pre-existing
  OQ-S66.1 max_tool_steps golden drift + tiebreaker baseline); eval_interactive
  499 passed/4 failed; autoloop 276 passed; 17-fixture 31; scoring SHA
  35305bd8… held. Codex deferred to M-Auto-3 milestone close (default §4.3) —
  no Tier-0 elevation argument surfaced.
---

## §0 Sub-sprint summary

- **Sub-sprint**: S-Auto-12 (sub-sprint 2 of M-Auto-3 — Substrate-hygiene).
  **Layer**: `infra` (A1 idempotency 回挡 in the dispatch path) +
  `prompt_projection` (`already_called` observation→binding soft signal) +
  trace annotation. No UC-hypothesis / drift / escalation-posture /
  response-strategy decision changed. **§7 stanza REQUIRED** (self-walked §5).
  **Local-Mac only**, branch `auto-loop-branch`.
- **Goal**: cut the single largest source of execution-path divergence at
  bot_temp=0 — byte-identical tool calls re-dispatched within one
  `AgentRunLoop.run` (the `IDENTICAL_RETRY` storm). A1 ships a **hybrid**:
  (1) a deterministic per-run idempotency 回挡 (serve a `success==true`
  byte-identical repeat from cache, no re-dispatch, no budget) + (2) an
  upgrade of the `already_called` slot to a **binding soft signal** so the LLM
  stops re-emitting. Net: the loop stops burning max-steps on byte-identical
  repeats, lowering the downstream max-steps/budget-exhaustion frequency that
  mis-stamps escalation reasons.
- **Commits** (this sub-sprint, on `auto-loop-branch`):
  - `17991c6` — A1 hybrid tool-call dedup (idempotency 回挡 + `already_called`
    binding soft signal): `ToolEvent` fields, `AgentRunLoopImpl` per-run cache,
    `ControlKernel` trace annotation, `system_prompt.txt` binding upgrade, tests.
  - (handoff commit follows.)
- **Scope files touched**:
  - `server/src/main/java/com/gumtree/csagent/model/ToolEvent.java`
    (+`deduplicated` / `originalAtStep` record fields; 8-arg back-compat ctor;
    `deduplicated(...)` factory).
  - `server/src/main/java/com/gumtree/csagent/service/runtime/AgentRunLoopImpl.java`
    (per-run `successfulDispatchCache` + dispatch-site 回挡 + comment refresh).
  - `server/src/main/java/com/gumtree/csagent/service/runtime/ControlKernel.java`
    (emit `deduplicated` / `original_at_step` on the persisted `tool_calls` trace).
  - `server/src/main/resources/prompts/system_prompt.txt`
    (`already_called` teaching observation→binding).
  - `server/src/test/java/com/gumtree/csagent/integration/AgentRunLoopIdentityDedupIntegrationTest.java`
    (renamed from the Sprint-20 `…AlreadyCalledNonEnforcementIntegrationTest`;
    rewritten to assert the new dedup contract).
  - `server/src/test/java/com/gumtree/csagent/service/runtime/AgentRunLoopIdentityDedupTest.java`
    (new negative-control + prompt-binding tests).
  - No edits to `PhaseEvaluator` / `skills/**` / `discover_triage.yaml` /
    `tool-policy.yaml` / `case_specs/**` / `user_simulator.py` / `loop.py` /
    the 4 SHA-locked scoring files / `applier.py` / sandbox / meta_agent /
    governance docs / archives.
- **Final test gates**:
  - **Java**: `Tests run: 1186, Failures: 10, Errors: 0, Skipped: 2`
    (`mvn -q -pl server test`) — **+3 new A1 tests** over the OQ-S66.1 baseline
    `1183/10/0/2`; **Failures held at 10** (all pre-existing — see §5).
  - **eval_interactive pytest**: `499 passed, 4 failed` (unchanged; A1 is
    server-side).
  - **autoloop pytest**: `276 passed`.
  - **17-fixture detector sweep**: `31 passed`.
  - **scoring SHA**: `35305bd84402ac455b126be5bcf8b2cb3b3fa55e3399f2c02443ea74bd8704e8`
    (held; no scoring file touched).
  - **A1 measurement (deliverable)**: see §4.

## §1 A1 deterministic idempotency 回挡 (backstop half)

**Where**: `AgentRunLoopImpl.run(...)`. A per-run cache is declared at the top
of the run (`AgentRunLoopImpl.java:171`):

```java
Map<String, ToolEvent> successfulDispatchCache = new LinkedHashMap<>();
```

**Key**: `toolName + "|" + canonicalArgumentsHash(args)`. The hash is the
**existing** `ContextProjectionBuilder.canonicalArgumentsHash(...)`
(`ContextProjectionBuilder.java:1267`, order-insensitive SHA-256 truncated to
16 hex chars) — the same digest already used to populate the `already_called`
slot. No new hash, no keyword/regex/enum/per-UC matrix; identical for every
tool.

**回挡 (`AgentRunLoopImpl.java:466-501`)**: immediately before the real
`toolDispatcher.dispatch(...)`, the loop computes `dedupKey` and checks the
cache. On a hit:
- it builds a `ToolEvent.deduplicated(...)` event (success=true, latency=0,
  `deduplicated=true`, `originalAtStep=<stepIndex of the cached original>`),
- accumulates the **cached** payload into `accumulatedToolResults` (so the LLM
  still sees the result under that tool's key),
- **`continue`s without calling the dispatcher** — the tool is NOT re-executed
  (no external call / no budget), and
- logs `AgentRunLoop A1 dedup: byte-identical {tool} at step {n} served from
  per-run cache (original_at_step={m})`.

**Cache scope = success only (`AgentRunLoopImpl.java:534-535`)**: only a
`result.isSuccess()` dispatch enters the cache, via `putIfAbsent` (so the FIRST
success is the canonical `original_at_step`). A non-`success` (external-failure)
result is **never** cached, so a legitimate same-args retry re-dispatches
(negative control §3b). A `"hash_error"` arguments hash disables dedup for that
call (it always re-dispatches) — defensive against the rare serializer failure.

**Why `continue` is safe for the side-effecting tools**: `request_handover`
(success) and `classify_use_case` on DISCOVER (success) END the run on first
success (escalate / `useCaseIdentified` short-circuits), so they can never be
cached-then-repeated within a run; a repeat `record_outcome` is idempotent by
design (the first success already stamped the session). The storm is dominated
by read-only `search_knowledge` / `get_customer_context` repeats, which dedup
cleanly.

## §2 `already_called` observation→binding soft-signal upgrade (hybrid half)

The slot JSON shape is **unchanged** (`buildAlreadyCalledNode` still emits one
`{tool, arguments_hash, at_step}` entry per successful prior dispatch — shape
tests `AlreadyCalledProjectionTest` stay green). The upgrade is in the
**consuming instruction** in `system_prompt.txt` (the teaching block the LLM
reads), from observation-only to binding:

**Before** (observation-only):
> The slot is observable state — read it before emitting tool calls.
> … Use the prior payload from `accumulated_tool_results` instead of
> re-emitting the call. … a fresh call may be warranted. **The slot does not
> block dispatch.**

**After** (binding soft signal — still LLM-owned):
> Read this slot before emitting tool calls — **it is a binding signal, not
> merely an observation.**
> … **Do NOT re-emit it: the runtime now automatically deduplicates
> byte-identical repeats and hands back the earlier result, so repeating
> changes nothing and only burns one of your limited turns.** Instead, draft
> your answer from the prior payload in `accumulated_tool_results`, or move on
> to the next distinct action …
> **You own the judgement on which tool to call and what content to send; the
> signal only discourages byte-identical repeats.** If you have new information
> … a fresh call is warranted — its `arguments_hash` will differ …

The LLM still owns WHICH tool / WHAT content (Constitution §1.3); the signal
only discourages byte-identical repeats. The preserved anchors required by the
regression tests remain: the `` `already_called` projection slot `` header, the
`arguments_hash` / `accumulated_tool_results` cross-references, the ownership
phrase ("you own"), the "not to repeat work the runtime has already performed"
principle line, and no tool-name / UC branching in the teaching window
(`AlreadyCalledPromptConsumptionTest`, `SkillTeachingMigrationIntegrationTest`
both green).

## §3 Negative controls + trace tests

`AgentRunLoopIdentityDedupTest` (unit, real `ContextProjectionBuilder` + mocked
dispatcher/parser/LLM):
- **(a) `singleCall_isNotDeduped`** — one dispatch → `dispatch` called once, no
  `deduplicated` event. The backstop never fires spuriously on a first call.
- **(b) `sameArgsRetryAfterExternalFailure_reDispatches`** — first dispatch
  returns `ToolResult.error(...)` (success=false → NOT cached); the identical
  retry returns ok → `dispatch` called **twice**, zero `deduplicated` events.
  Proves an external-failure retry is never mis-deduped (STOP condition guard).
- **(d) `systemPrompt_teachesAlreadyCalledAsBindingSignal`** — the deployed
  prompt now contains "binding" + "deduplicat" while retaining "you own".

`AgentRunLoopIdentityDedupIntegrationTest` (end-to-end, real
`AgentRunLoopImpl` + real `ContextProjectionBuilder`), supersedes the Sprint-20
non-enforcement test:
- **(c)** two byte-identical `search_knowledge` calls → `dispatch` called
  **once**; the second `ToolEvent` is `deduplicated=true`, `originalAtStep=0`,
  `latencyMs=0`, carries the cached payload; the loop reaches FINAL_ANSWER; the
  step-1 projection still carries the `already_called` slot referencing step 0.

The `deduplicated` / `original_at_step` fields are flattened onto the persisted
`bot_turns.tool_calls` trace by `ControlKernel.java:1859-1862` — load-bearing
for downstream report.html / admin trace (`project_observability_debt_pattern`).

## §4 IDENTICAL_RETRY 3-pass bad_cases measurement (before/after)

**Method**: `cd eval_interactive && uv run eval-interactive run --path
case_specs/bad_cases/ --parallel 1` (sim_temp=0 / bot_temp=0 / 60s deadline per
the `b351648` determinism config), ×3, against the **freshly-restarted** `:8080`
backend (PID 99795, built from `17991c6`; prompt + dedup class deployment
verified). A "case-run" = one bot turn = one `per_turn_trace` entry (one
`AgentRunLoop.run`, the per-run dedup scope). An **unmitigated IDENTICAL_RETRY
turn** = a turn with ≥2 SUCCESSFUL, NON-`deduplicated` tool calls sharing the
same `(tool_name, order-insensitive args key)`. Analysis script:
`/tmp/sauto12_retry_analysis.py` (reads `case_results[].per_turn_trace[].tool_calls`).

**Same-harness before (3 pre-change passes earlier today, no A1)**:

| run | total turns | unmitigated IDENTICAL_RETRY | dedup events |
|---|---|---|---|
| 20260601-070530 | 30 | **4/30** | 0 (no A1) |
| 20260601-071402 | 32 | **5/32** | 0 (no A1) |
| 20260601-071943 | 34 | **3/34** | 0 (no A1) |

(The documented historical `15/24` was a harsher pre-determinism-config
trace-dive; on today's `b351648` harness the residual storm sat at ~3–5 turns
per pass — still the single largest divergence source.)

**After (3 post-change passes, A1 live)**:

| run | total turns | unmitigated IDENTICAL_RETRY | dedup events (backstop fired) |
|---|---|---|---|
| 20260601-130648 | 25 | **0/25** | 1 |
| 20260601-131027 | 34 | **0/34** | 6 |
| 20260601-131542 | 30 | **0/30** | 9 |

**Result**: unmitigated `IDENTICAL_RETRY` = **0/25, 0/34, 0/30 → 0 across all 3
passes** (target ≤2/24 — met; the storm is fully eliminated, not merely reduced).
The deterministic backstop fired **16 times total** (1+6+9), each annotated
`deduplicated:true` + `original_at_step` and verified literally present in the
persisted trace JSON, e.g.:

```
case=alice_uc_a_uc_h_misclass turn=1 tool=search_knowledge
  success=True deduplicated=True original_at_step=3 latency_ms=0
```

**Empirical confirmation of the hybrid (red line #2)**: the 16 dedup events
prove the bot LLM **still attempts** byte-identical repeats even with the
binding soft signal in the prompt (e.g. cs095 turn1 re-emitted
`search_knowledge` 4× in one run; cs015 turn4 3×). The binding signal reduces
but does not eliminate the re-emission; the deterministic backstop catches the
remainder. Soft-signal-ALONE would therefore have leaked all 16 as wasted
re-dispatches — exactly the Sprint-19/20 failure mode. Both halves are
load-bearing.

**Negative controls intact in-the-wild**: the 25/34/30 non-retry turns
dispatched normally (no spurious `deduplicated` flag); the §3 unit controls
(single call, external-failure retry) hold.

## §5 Baselines preserved + §7-stanza self-walk + fence disposition

**Baselines preserved**:
- Java `1186/10/0/2` — the 10 failures are the documented OQ-S66.1 pre-existing
  baseline, NOT A1-attributable: `PhaseEvaluatorPlanTest` (2) +
  `PhaseEvaluatorResolveSkillIntegrationTest` (7) are the `max_tool_steps`
  golden drift (`assertEquals` expected 4 but was 6 on
  `resolve_faq_grounded_answer.yaml`; expected 2 but was 3 on
  `discover_triage.yaml`) — they live on hard-fenced skill/test-golden surfaces
  (B1 = S-Auto-14); `SystemPromptUserRequestedTiebreakerTest` (1) is the
  inherited Sprint-24-era working-tree mod. `Tests run` rose by the 3 new A1
  tests; `Failures` held at 10. None reference `already_called`/`deduplicat`.
- eval_interactive `499/4`, autoloop `276`, 17-fixture `31`, scoring SHA
  `35305bd8…` — all unchanged (A1 is server-side; no scoring file touched).

**§7 stanza self-walk**:
- **Target failure layer**: `infra` (A1 idempotency 回挡 in the dispatch path)
  + `prompt_projection` (`already_called` observation→binding soft signal). No
  agent semantic decision changed.
- **Tier-0 invariant**: NONE added. A1 dedup extends the Runtime's existing
  idempotency/persistence responsibility (Constitution §1.4); it is NOT added
  to `runtime_freeze_and_risk_policy.md` §1/§2. No Tier-0 elevation argument
  surfaced during the work, so Codex stays milestone-shared (§4.3 default); had
  one surfaced it would have been `human_review_required` (NOT self-invented).
- **Semantic hardcode**: NONE. The dedup key reuses the existing
  order-insensitive `canonicalArgumentsHash` (identical for all tools); the
  回挡 fires only on byte-identical `(toolName, args)` calls whose prior result
  was `success==true` and makes no semantic judgment. Justification for the
  deterministic backstop: soft-signal-first (Sprint 19) shipped (Sprint 20
  `already_called`, observation-only) and the storm persisted at temp=0 →
  soft-signal-ALONE is empirically falsified; the idempotency backstop is the
  §1.4 Runtime responsibility, not a §1.5 violation; hybrid is mandatory
  (red line #2). Net effect: REMOVES wasted re-dispatches, adds no rule.
- **Generalization coverage**: target = bad_cases `IDENTICAL_RETRY` subset
  (§4); neighbor = the anchor_outcome / shadow same-storm shape (not re-run
  here — server-side change, covered by the bad_cases trace-dive); negative =
  (a) single call not deduped, (b) external-failure same-args retry
  re-dispatches (both green, §3); shadow = held-out, not consumed by dev.

**Fence disposition**: every hard-fenced surface listed in the objective was
left untouched (verified by `git show --stat 17991c6`: only the 6 scope files).
The Sprint-20 `AgentRunLoopAlreadyCalledNonEnforcementIntegrationTest` is NOT a
hard-fenced file; it encoded the precise observation-only contract this
sub-sprint deliberately reverses, so it was renamed + rewritten to assert the
new dedup contract (this is a contract change the sub-sprint owns, not a test
edited to mask a regression — the old assertion `dispatch times(2)` is exactly
the wasted re-dispatch A1 removes).

## §6 Open questions surfaced

- **OQ-S67.1 (no residual unmitigated storm; backstop is load-bearing)**: 0/89
  bot-turns across the 3 post-change passes had an unmitigated byte-identical
  re-dispatch — the storm is closed. BUT the bot LLM still *attempts*
  byte-identical repeats (16 caught by the backstop), so the binding soft
  signal alone is insufficient and the §1.4 idempotency 回挡 carries the close.
  This is a confirmation, not a defect; no action required. It does suggest a
  *future* prompt-projection refinement (surface the dedup hit back to the LLM
  more loudly so it re-emits less) could raise `planner_ownership_ratio`, but
  that is a separate prompt-tuning item, NOT A1.
- **OQ-S67.2 (downstream max-steps/escalation mis-stamp is NOT closed by A1
  alone)**: A1 stops the storm from *consuming* steps, but the
  max-steps/budget-exhaustion → escalation-reason mis-stamp lives in
  `PhaseEvaluator.resolveMaxStepsReason` (B1 = S-Auto-14, hard-fenced here).
  Whether eliminating the storm materially lowers max-steps exits should be
  re-measured at M-Auto-3 close once B1 lands. (Links to
  `project_faq_overescalate_maxsteps_misstamp`.)
- **Bad-case human-judgment (§5.6)**: the per-case PASS/FAIL/IMPROVING judgment
  of the bad_cases suite is deferred to M-Auto-3 milestone close (deliver-agent
  + human). This sub-sprint only measured the `IDENTICAL_RETRY` divergence
  metric, not bad-case closure; one case (cs012) surfaced `HUMAN_REVIEW` per its
  `closure_criterion`, unchanged by A1 (A1 does not touch outcome semantics).

## §7 Self-check tick-off

- [x] Deterministic 回挡 implemented (per-run `(toolName, canonicalArgumentsHash)`
      cache; `success==true`-only; no step/budget on hit; reuses existing hash).
- [x] `already_called` upgraded observation-only → binding soft signal (HYBRID;
      red line #2 satisfied — both halves shipped).
- [x] Trace annotation `deduplicated:true` + `original_at_step` on every 回挡
      hit (`ToolEvent` fields → `ControlKernel` `tool_calls` trace).
- [x] Negative controls pass: normal single call NOT deduped; external-failure
      (non-`success`) same-args retry re-dispatches.
- [x] `IDENTICAL_RETRY` storm eliminated on a 3-pass bad_cases rerun (backend
      restarted; via persisted per-turn traces): **0/25, 0/34, 0/30** unmitigated
      (target ≤2/24); backstop fired 16× with `deduplicated`/`original_at_step`
      visible in-trace — see §4.
- [x] Java no NEW failures beyond the OQ-S66.1 baseline `10` (+3 new tests);
      did NOT touch the 9 golden-drift failures.
- [x] eval_interactive `499/4` + autoloop `276` + 17-fixture `31` + scoring SHA
      `35305bd8…` preserved.
- [x] No edits to PhaseEvaluator / skills / case_specs / user_simulator /
      loop.py / scoring / applier.py / sandbox / meta_agent; no Tier-0
      self-invented.
- [x] No `git add -A`; any autoloop run on a clean committed tree; local-Mac only.
- [x] Handoff §0-§7 filled.
