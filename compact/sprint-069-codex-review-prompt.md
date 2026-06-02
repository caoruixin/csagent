# Sprint 069 / S-Auto-13b / M-Auto-3 — Codex Per-Sub-Sprint Review Prompt

You are **Codex / Review Agent** for **Sprint 069 / S-Auto-13b / M-Auto-3 — A3 paraphrase-storm deterministic backstop (faq_miss-state-aware same-turn `search_knowledge` re-search suppression), sub-sprint 4 of 5 — Per-sub-sprint review per `iteration_governance.md` §4.3 trigger #2**. This sub-sprint adds a **deterministic backstop on the LLM-owned "whether to re-search this turn" decision**; a deterministic gate on a decision §1.3 nominally hands the LLM is §1.7-adjacent, which is exactly why §4.3 trigger #2 makes this review PER-SUB-SPRINT REQUIRED (NOT deferrable to the M-Auto-3 milestone-shared close).

**Cumulative scope claim**: review commits `bbd385e..HEAD` on `auto-loop-branch` (= 2 commits):
- `1acd9b3` — A3 backstop code: `ToolEvent.java` (+74 −5), `AgentRunLoopImpl.java` (+104), `ControlKernel.java` (+14), and the new `AgentRunLoopFaqMissStateGateTest.java` (+449). 4 files, +636 −5.
- `a73e2e4` — dev handoff `docs/sprints/sprint-069-handoff.md` (+509). docs-only.

The immediate predecessor `bbd385e` ("Add framework-template manifest and Layer A skeleton") + the four governance/template housekeeping commits between the S-Auto-13 close (`5970664`) and this sub-sprint are NOT in scope (they are docs/template-only and unrelated to the A3 backstop). Confirm the review range with `git diff --stat bbd385e..HEAD` — expected exactly the 5 files above.

This prompt is **self-contained per `iteration_governance.md` §9 invariant**. You do NOT need to read any repo doc beyond `AGENTS.md` (auto-loaded via the constitution chain) + this prompt + the specific code anchors + the sub-sprint handoff named below. Do NOT edit code; do NOT re-judge bad-case manual review verdicts; do NOT adjudicate the milestone-level §11 PARAPHRASE_STORM interpretation (that is a deliver-agent + human decision reserved for M-Auto-3 close — see "What this review does NOT cover"); do NOT auto-PASS / auto-FAIL.

## Read order (minimal)

1. `AGENTS.md` is auto-loaded by Codex on session start; the constitution chain `doc_governance.md` → `agent_context_guide.md` → `iteration_governance.md` loads transitively. **Do NOT manually read** these.
2. This prompt.
3. **Sub-sprint handoff** (dev-authored; required for axis evidence): `docs/sprints/sprint-069-handoff.md` (509 lines; §0 summary + §1 the `faq_miss`-state gate mechanism + placement vs A1 + §2 trace annotation before/after + §3 negative controls (a)-(g) + §4 PARAPHRASE_STORM 3-pass before/after + OQ-S68.4 subsumption + §5 baselines + §7-stanza self-walk + fence disposition table + §6 OQs + §7 self-check).
4. **Code anchors** (read on demand during review):
   - `server/src/main/java/com/gumtree/csagent/service/runtime/AgentRunLoopImpl.java` — the gate. Three regions (confirm exact lines on read): the per-run tracker decl `ToolEvent lastSearchKnowledgeViableHit = null;` (~:191, just after the `@@ -169` hunk); the **6a-ter** suppression gate (`if ("search_knowledge".equals(toolName) && lastSearchKnowledgeViableHit != null)` ~:525-557, AFTER A1's 6a byte-identical dedup `continue`); the **6c-bis** tracker refresh (~:607-638, reads `result.getData()` `Map`'s `faq_miss` flag → sets the tracker to the just-recorded event on `faq_miss=false`, else null).
   - `server/src/main/java/com/gumtree/csagent/model/ToolEvent.java` — new record components `paraphraseSuppressed` + `faqHitAtStep`; 12-arg canonical ctor; 10-arg + 8-arg back-compat ctors; new `paraphraseSuppressed(...)` factory. The existing `deduplicated(...)` / `of(...)` / `rejected(...)` factories are unchanged.
   - `server/src/main/java/com/gumtree/csagent/service/runtime/ControlKernel.java` — the flatten mirror (~:1859-1862 region): `if (te.paraphraseSuppressed()) { entry.put("paraphrase_suppressed", true); entry.put("faq_hit_at_step", te.faqHitAtStep()); }`, placed just below the existing S-Auto-12 `deduplicated` flatten.
   - `server/src/test/.../runtime/AgentRunLoopFaqMissStateGateTest.java` — 6 cases (negative controls + A1 coexistence).
   - **Read-only (faq_miss source)**: `server/src/main/java/.../model/KnowledgeSearchResult.java` + `service/tools/SearchKnowledgeTool.java` — confirm the `faq_miss` flag lands on the dispatched `ToolResult` data map (the gate reads it; it does NOT recompute or interpret it).

## Embedded sub-sprint contract (verbatim from `docs/sprint_objective.md`)

### S-Auto-13b — Class

- **Layer (primary)**: `infra` — a `faq_miss`-state-aware same-turn `search_knowledge` re-search suppression gate in the tool-dispatch path (`AgentRunLoopImpl`), added ALONGSIDE the S-Auto-12 A1 `successfulDispatchCache` (NOT modifying A1) + trace annotation. No UC-routing / drift / escalation-posture decision changes; the LLM still owns whether to search the FIRST time, which tool, and what content.
- **§7 stanza**: REQUIRED (self-walked below).
- **Codex review plan (§4.3)**: PER-SUB-SPRINT REQUIRED (trigger #2 — a deterministic backstop on the LLM-owned "whether to re-search" decision is §1.7-adjacent; Codex verifies the structural-cardinality justification + keyed-on-`faq_miss`-not-content + soft-signal-first-was-tried).
- **Position in milestone**: 4th of 5 (S-Auto-11 ✅ → S-Auto-12 ✅ → S-Auto-13 ✅-partial → **S-Auto-13b A3 backstop** → S-Auto-14 B1 → M-Auto-3 close). The optional fix-iteration buffer is consumed by this sub-sprint; M-Auto-3 is at the §8.1 5-sub-sprint ceiling.

### S-Auto-13b — Goal

Close the A3 paraphrase-storm to the §11 bar deterministically, since the soft signal (correct, and firing) is ignored by the model. After S-Auto-13b, once a `search_knowledge` in the current turn returns a viable hit (`faq_miss=false`), subsequent same-turn `search_knowledge` re-searches are served from the prior viable hit + trace-annotated instead of re-executing — so the loop stops burning steps re-confirming an answer it already has.

**Acceptance** (S-Auto-13b's own contract): on a 3-pass `bad_cases` rerun (sim_temp=0 + bot_temp=0, freshly-restarted backend, measured via the S-Auto-11 per-iter trace persistence) the **within-turn** `PARAPHRASE_STORM` (the gate's design surface) drops to ≤3 with negative controls intact (FIRST search of a turn never suppressed; re-search after `faq_miss=true` allowed; cross-turn / new-run search not suppressed; a legitimately-distinct needed second search not wrongly suppressed — STOP-and-surface if it is). Baselines preserved: Java `1192/1/0/2` + new gate tests; eval_interactive `495/8` (the 8 are OQ-S68.1, not this sub-sprint's); autoloop `276`; 17-fixture `31`; scoring SHA `35305bd8…`.

### S-Auto-13b — Scope (4 steps)

1. **`faq_miss`-state-aware re-search suppression gate** in `AgentRunLoopImpl`, in the dispatch path ALONGSIDE the S-Auto-12 A1 `successfulDispatchCache` (do NOT modify A1's byte-identical logic). Track, per run (`AgentRunLoop.run` = one turn), the **most-recent `search_knowledge` result's `faq_miss` state**. When a NEW `search_knowledge` dispatch occurs and the most-recent prior `search_knowledge` in this run was `faq_miss=false` (viable hit): suppress it — serve the prior viable-hit result (so the LLM still sees the hit), do NOT re-execute, do NOT charge a step/budget (mirror A1). **Exception**: if the most-recent `search_knowledge` was `faq_miss=true` (no viable hit), DO allow the re-search.
2. **Trace annotation**: `paraphrase_suppressed:true` + `faq_hit_at_step:<n>` on the `ToolEvent` (distinct from A1's `deduplicated`); flatten onto the persisted `tool_calls` via `ControlKernel`.
3. **Negative controls + tests**: (a) first search not suppressed; (b) re-search after `faq_miss=true` dispatched; (c) cross-run not suppressed (per-run scope); (d) suppressed re-search returns prior hit + writes annotation + charges no step; (e) A1 + A3 coexistence; plus validate against the bad_cases that no LEGITIMATELY-DISTINCT needed second search is wrongly suppressed (STOP-and-surface if so).
4. **3-pass `bad_cases` measurement + handoff + OQ ledger**, incl. PARAPHRASE_STORM before/after + OQ-S68.4 subsumption.

### S-Auto-13b — §7 stanza (REQUIRED; verbatim)

**Target failure layer:** `infra` (a `faq_miss`-state-aware same-turn `search_knowledge` re-search suppression gate in the dispatch path). No agent semantic decision (UC hypothesis, drift, escalation posture, response strategy) is changed; the LLM still owns whether to search first, which tool, and what content.

**Tier-0 invariant:** This sprint adds NO Tier-0 invariant. The gate extends the Runtime's existing idempotency / cardinality / budget responsibility (Constitution §1.4); it is NOT added to `docs/runtime_freeze_and_risk_policy.md` §1/§2. If review argues for elevating "per-turn search cardinality after a viable hit" to Tier-0 → `human_review_required` (do NOT self-invent).

**Semantic hardcode:** None introduced. The gate is a STRUCTURAL state/cardinality backstop keyed on the EXISTING `faq_miss` result flag — NOT on query content, keyword, regex, enum, or per-UC matrix; it makes no semantic judgment about what the LLM searched for. **Justification for the deterministic backstop (anti-hardcode):** soft-signal-first was satisfied — S-Auto-13 shipped the `faq_miss`-keyed soft `grounding_instruction` + projection echo, they demonstrably FIRE (15-17/run), and the model empirically ignored them (`PARAPHRASE_STORM` 16/16/7). This is the SAME falsification → deterministic-backstop pattern as A1 (Sprint 19/20 soft-signal-alone → S-Auto-12 hybrid 回挡). The soft layer STAYS beneath the backstop. Net effect: REMOVES wasted re-searches, adds no semantic rule.

**Generalization coverage:** target = the bad_cases `PARAPHRASE_STORM` subset; neighbor = `anchor_outcome` / shadow same-storm shape; negative = (a) first search of a turn not suppressed, (b) re-search after `faq_miss=true` allowed, (c) cross-turn/new-run search not suppressed, (d) a legitimately-distinct needed second search not wrongly suppressed (STOP-and-surface if so); shadow = held-out (not read by dev).

## Embedded milestone context (brief — for orientation only)

**M-Auto-3 — Substrate-hygiene** cleans the agent-runtime substrate so the autoloop fitness signal measures propose-quality, not storm noise. The §11 unlock criteria include `PARAPHRASE_STORM 11/24→≤3`. S-Auto-13 shipped the A3 **soft** layer (grounding_instruction + projection echo); it FIRES but `deepseek-v4-flash` ignores it (`PARAPHRASE_STORM` stayed 16/16/7) → OQ-S68.3 → this sub-sprint adds the deterministic backstop. The S-Auto-13 soft layer STAYS in place beneath the backstop (soft-signal-first, Constitution §1.5).

**Milestone-level §11 interpretation is OUT OF SCOPE for this review.** The dev's measurement decomposes PARAPHRASE_STORM into **within-turn** (the per-run gate's design surface: 12→0 across 3 passes, §11 ≤3 met) and **cross-turn** (a paraphrase issued in a SUBSEQUENT bot turn referencing a prior turn's viable hit: 21→22, structurally uncatchable by a per-run gate, surfaced as OQ-S69.1). Whether the milestone §11 bar is read as the within-turn surface or the total count is a **deliver-agent + human decision at M-Auto-3 close** — NOT something this review adjudicates, and the residual cross-turn count is **NOT a S-Auto-13b sprint-level failure**. Your job is the anti-hardcode kernel + the gate's correctness/scope/fence discipline.

## §4.1 nine-question kernel (embedded verbatim from `docs/current/anti-hardcode-review-kernel.md`)

```text
You are the Anti-Hardcode Review Agent. The PR below proposes a change
to this repo. Your job is to decide whether the change introduces a
semantic hardcode — a keyword / regex / if-else / enum / per-UC matrix
that encodes a decision the LLM is supposed to own under
docs/current/iteration_governance.md §1.3 — and to issue a verdict.

Scope exemption: pure infra, docs-only, config-governance, and
characterization-test PRs are not subject to this review. If the PR
is purely one of those, return `approve` with a one-line note naming
the exemption.

For every other PR, walk these nine questions in order. For each
"yes" or each concern, paste the diff snippet and the reasoning.

1. Does the PR add a keyword / regex / if-else / enum / per-UC matrix
   for a semantic decision (drift detection, escalation, UC
   selection, risk classification, follow-up, intake routing)?
2. If yes to (1), is the change justified as protecting a current
   Tier-0 invariant named in docs/runtime_freeze_and_risk_policy.md
   §1 / §2?
3. Could the same outcome be achieved by projecting a soft signal to
   the LLM (an additional projected slot, a candidate list, a
   diagnostic flag) instead of a hard branch in Java or the prompt?
4. Does the change encode visible-eval case text, trace-specific
   phrasing, or a CaseSpec id into runtime, prompt, or judge config?
5. Does the change move semantic ownership from the LLM to Java —
   that is, shrink what docs/current/iteration_governance.md §1.3
   says the LLM owns?
6. Does the change add an if-else block to the prompt instead of
   principle-level or observable-state guidance?
7. Does the change preserve tool schema, capability / permission
   boundary, PII / safety floor, and grounding floor?
8. Does the PR ship generalization eval coverage — target, neighbor,
   negative, and shadow cases — and not only the target case?
9. If the change is temporary, does it carry an explicit rollback or
   sunset plan (downgrade-to-signal trigger, retirement sprint id)?

Return exactly one verdict:

- `approve` — the change is not a semantic hardcode, or is justified
  as protecting a current Tier-0 invariant with adequate generalization
  coverage and a clear rollback if temporary.
- `approve with downgrade-to-signal follow-up` — the change is
  acceptable as an interim measure, but a follow-up sprint must
  convert it into a soft signal projected to the LLM. Name the
  trigger that should fire the conversion.
- `reject as semantic hardcode` — the change encodes a soft semantic
  decision the LLM should own; questions 1 and 2 fail, or questions
  5 / 6 fail, with no Tier-0 claim and no sunset plan.
- `needs human architecture decision` — the change crosses an
  unresolved governance question (new escalation reason enum value,
  new Tier-0 candidate, LLM-vs-Java boundary shift, new tool surface)
  and a human reviewer must decide before merge.

Do not rewrite the PR. Do not propose a code fix beyond naming the
layer in docs/current/iteration_governance.md §3 that the fix should
target.
```

## §4.1 Verdict set (use ONE)

- `approve` — no concerns; clean PASS on all 9 questions, the deterministic-backstop justification holds (soft-signal-first genuinely tried + falsified; keyed on `faq_miss` result-state not content; §1.4 idempotency/cardinality framing valid; no new Tier-0).
- `approve with downgrade-to-signal follow-up` — kernel PASS but a residual concern warrants a non-blocking follow-up. Name the trigger.
- `reject as semantic hardcode` — at least one §1.7 forbidden-list red line crossed (e.g., the gate keys on query content / keyword / regex / per-UC; or soft-signal-first was NOT actually tried; or it shrinks §1.3 LLM ownership of the FIRST search). Targeted fix-iteration required before close.
- `needs human architecture decision` — kernel cannot be resolved without a product / governance / Tier-0 decision (e.g., a reviewer judges the per-turn-search-cardinality gate SHOULD be a Tier-0 invariant). Escalate.

## Verification axes (Codex walks each; report PASS / CONCERN / FAIL + per-axis evidence with `file:line`)

### Axis A — §4.1 nine-question kernel walk

Apply the kernel to the cumulative 2-commit scope (`bbd385e..HEAD`; code-bearing commit `1acd9b3`). The PR is NOT a pure-infra carve-out auto-approve — a deterministic gate on the "whether to re-search" decision is §1.7-adjacent; walk all 9 questions with evidence. Expected reads (verify, don't assume):

- **Q1** — does the gate add a keyword / regex / if-else / enum / per-UC matrix for a *semantic* decision? Expected PASS: the only branch conditions are `"search_knowledge".equals(toolName)`, `lastSearchKnowledgeViableHit != null`, and reading the boolean `faq_miss` flag out of `result.getData()`'s `Map`. There is **no** query-string inspection, no keyword/regex match on what was searched, no per-UC branch. FAIL Q1 if you find the gate keying on query content / case text / UC.
- **Q2** — Tier-0 claim? Expected PASS: no new Tier-0 invariant; the §7 stanza + handoff §5 explicitly say none and frame the gate as §1.4 idempotency/cardinality. (If you judge it SHOULD be Tier-0, that is `needs human architecture decision`, not a silent pass.)
- **Q3** — could a soft signal achieve the same outcome instead of a hard gate? This is THE central question. Expected PASS *with justification*: a soft signal WAS tried (S-Auto-13's `grounding_instruction` + `search_reuse_instruction` projection echo) and empirically falsified — it fired 15-17×/run and the model ignored it (`PARAPHRASE_STORM` 16/16/7 vs ≤3). Verify this falsification claim is supported (handoff §0/§4/§5 + the soft layer STAYS in place beneath the backstop, not removed). Same pattern as A1 (Sprint 19/20 → S-Auto-12). If you judge soft-signal-first was NOT genuinely tried, FAIL Q3.
- **Q4** — visible-eval case text / CaseSpec id / trace phrasing encoded into runtime? Expected PASS: the gate keys on a runtime result-state flag, not on any case text. (The handoff §3(g) quotes bad_cases query strings for the distinct-need audit, but those are in the HANDOFF doc, not the runtime code — verify no case text leaked into `AgentRunLoopImpl`.)
- **Q5** — does it shrink §1.3 LLM ownership? Expected PASS: the LLM still owns the FIRST search, which tool, what content; the gate only suppresses a *redundant re-search after a viable hit this same turn* — a §1.4 cardinality/budget concern. Verify the FIRST search is never gated (negative control (a)).
- **Q6** — if-else added to a prompt? Expected PASS: the change is Java dispatch-path code + a record + a trace flatten; no prompt / Skill YAML edited (the S-Auto-13 soft layer in `resolve_faq_grounded_answer.yaml` is UNCHANGED, not extended).
- **Q7** — tool schema / capability / PII / safety / grounding floor preserved? Expected PASS: the suppressed re-search SERVES the prior viable hit into `accumulatedToolResults` (the LLM still sees the grounded hits), so the grounding floor is preserved, not bypassed. Verify the suppression path puts the prior `resultData` under `search_knowledge` so grounding is not starved.
- **Q8** — generalization coverage (target / neighbor / negative / shadow)? Expected PASS: `AgentRunLoopFaqMissStateGateTest` covers first-search-not-suppressed, `faq_miss=true` re-search dispatched, cross-run isolation, suppressed-event annotation/prior-hit/zero-latency, A1+A3 coexistence, failure-retry re-dispatch; plus the §3(g) bad_cases distinct-need audit (17 suppressions all true paraphrases, zero false positives). Shadow is held-out (not read by dev).
- **Q9** — rollback / sunset? The backstop is durable (not temporary), so a sunset plan is not strictly required; note that the soft layer remains beneath it. If you judge a sunset/relaxation trigger is warranted (e.g., "if a future bad case shows a legitimate distinct second search is wrongly suppressed, relax to an N-count cap"), surface as `approve with downgrade-to-signal follow-up`.

### Axis B — `faq_miss`-keyed-NOT-content-keyed (the central anti-hardcode check)

The human-named #1 verification focus. Read `AgentRunLoopImpl` 6a-ter + 6c-bis. Confirm:
- (a) The suppression trigger is `"search_knowledge".equals(toolName) && lastSearchKnowledgeViableHit != null` — no inspection of `call`'s query/argument STRINGS to decide suppression.
- (b) The tracker refresh (6c-bis) sets `lastSearchKnowledgeViableHit` purely from the boolean `faq_miss` flag read out of `result.getData()`'s `Map` (`viableHit = !fm`), never from query text. The gate READS `faq_miss`; it does not RECOMPUTE or interpret it (faq_miss is produced upstream by `SearchKnowledgeTool` / `KnowledgeSearchResult` — read-only confirm).
- (c) No keyword / regex / enum / per-UC matrix anywhere in the gate.

FAIL Axis B if any suppression/tracking decision consults query content or case text.

### Axis C — soft-signal-first genuinely tried + falsified

The human-named #2 focus. Verify the deterministic backstop is justified by a real prior soft attempt, not a shortcut:
- The S-Auto-13 A3 soft layer (`grounding_instruction` paraphrase-discipline line in `resolve_faq_grounded_answer.yaml` + `search_reuse_instruction` projection echo in `ContextProjectionBuilder`) is STILL IN PLACE (handoff §5 fence-disposition row "skills/** UNTOUCHED — soft layer STAYS"); it was NOT removed when the backstop landed.
- The falsification evidence: the soft signal fired 15-17×/run yet `PARAPHRASE_STORM` stayed 16/16/7 vs ≤3 (handoff §0/§4). This mirrors the A1 precedent (Sprint 19/20 soft-signal-alone falsified → S-Auto-12 hybrid 回挡).
- The dev did NOT add the backstop in S-Auto-13 (it surfaced OQ-S68.3 and waited for the deliver+human decision) — soft-signal-first discipline was honored at the sprint boundary.

If you judge the soft layer was removed, or the falsification is unsupported, CONCERN/FAIL.

### Axis D — A1 (byte-identical) logic UNMODIFIED + coexistence

The human-named #3 focus. Verify A1 is untouched and the two backstops coexist correctly:
- The A3 6a-ter gate runs AFTER A1's 6a byte-identical dedup `continue` — so a byte-identical repeat is caught by A1 first and A3 never runs that iteration. Confirm the A1 region (`successfulDispatchCache` decl ~:171; the 6a dedup check + `continue` ~:486-503; the cache-put ~:534-535/`:554-556`) is byte-for-byte unchanged in the diff (the diff hunks are `@@ -169` add tracker decl, `@@ -503` add 6a-ter, `@@ -535` add 6c-bis — A1's existing lines are context, not modified).
- The annotations are DISJOINT: a given `ToolEvent` carries `deduplicated`+`originalAtStep` (A1) OR `paraphraseSuppressed`+`faqHitAtStep` (A3), never both — verify via `ToolEvent` ctors + the coexistence test `a1AndA3Coexist_byteIdenticalDeduped_paraphraseSuppressed`.
- When A1 dedups, the A3 tracker is NOT refreshed on that iteration (A1 `continue`s before 6c-bis) — confirm this is intentional and does not desync the tracker.

FAIL Axis D if A1's behavior changed or the annotations can both set on one event.

### Axis E — same-turn (per-run) scope clear; cross-turn explicitly out of scope

The human-named #4 focus. Verify the gate's scope is exactly one `run()` invocation (one outer bot turn):
- The tracker is a `run()`-local var (`ToolEvent lastSearchKnowledgeViableHit = null;`), not session/field state — it cannot leak across turns. Confirm via the cross-run negative control test `crossRun_gateStateIsPerRun`.
- The cross-turn paraphrase storm (a paraphrase in a SUBSEQUENT bot turn) is structurally uncatchable by this per-run gate and is correctly surfaced as **OQ-S69.1** (handoff §6), NOT silently dropped and NOT mis-claimed as fixed. Verify the handoff does not overclaim (it should report within-turn 12→0, cross-turn 21→22 unchanged, total 33→22).

CONCERN if the scope is muddled or the cross-turn residual is mis-stated as resolved.

### Axis F — no false-positive suppression of a legitimate distinct search

The human-named #5 focus. Verify the "always-suppress-after-viable-hit" form does not wrongly suppress a genuinely-different needed second search:
- Handoff §3(g): the dev manually sampled all 17 `paraphrase_suppressed` events across the 3 post-fix passes; all 17 are textbook paraphrases of the same intent (representative `cs011` / `cs014` examples cited); zero are legitimately-distinct second searches. The contract's STOP-and-surface condition ("if the gate suppresses a genuine need → halt, may need an N-count cap") was NOT triggered.
- Judge whether "zero false positives on the current bad_cases" is adequate evidence for the strict form, OR whether you'd want a sunset trigger (relax to an N>1 distinct-search count cap) if a future case surfaces a legitimate distinct second search. If the latter, surface as `approve with downgrade-to-signal follow-up` naming the trigger.

FAIL Axis F only if you find evidence a legitimate distinct search WAS suppressed.

### Axis G — trace annotation completeness

The human-named #6 focus. Verify the suppression is observable on the persisted trace (load-bearing for report.html / admin trace per `project_observability_debt_pattern`):
- `ToolEvent` carries `paraphraseSuppressed` + `faqHitAtStep`; the suppressed event has `latencyMs=0`, `success=true`, `resultData` = the prior viable hit, `deduplicated=false`, `originalAtStep=-1` (distinctness from A1) — verify via test `paraphraseAfterViableHit_isSuppressed_carriesAnnotationAndPriorHit`.
- `ControlKernel` flattens `paraphrase_suppressed` + `faq_hit_at_step` onto `bot_turns.tool_calls` (mirror of the S-Auto-12 `deduplicated` flatten).
- Handoff §2 claims 17 events carry the annotation on the persisted post-fix data — internal-consistency check against §4's per-pass `paraphrase_suppressed` column (8+6+3=17).

### Axis H — test deltas + reproducibility

Independent reproduction expected (you MAY verify; do not extend scope):

```bash
git diff --stat bbd385e..HEAD
# Expected: exactly ToolEvent.java, AgentRunLoopImpl.java, ControlKernel.java,
#   AgentRunLoopFaqMissStateGateTest.java, docs/sprints/sprint-069-handoff.md.

mvn -q -pl server test
# Expected: Tests run: 1198, Failures: 1, Errors: 0, Skipped: 2
#   (+6 new gate tests vs the 1192/1 baseline; the sole failure is the
#    inherited SystemPromptUserRequestedTiebreakerTest — OQ-S41.5 status quo,
#    NOT introduced here).

cd eval_interactive && uv run python -m pytest --tb=no -q
# Expected: 495 passed, 8 failed (UNCHANGED; the 8 are the 2026-06-01 0323457
#   action_bank-split governance/lint tests — OQ-S68.1, disjoint from this
#   server-side sub-sprint; NOT in scope to fix here).

cd autoloop && uv run --extra dev pytest -q
# Expected: 276 passed.

cd autoloop && uv run --extra dev pytest -p no:cacheprovider -q tests/test_anti_hardcode_check.py
# Expected: 31 passed.

cd autoloop && uv run --extra dev python -c "from autoloop.scoring.gaming import _compute_scoring_code_sha; print(_compute_scoring_code_sha())"
# Expected: 35305bd84402ac455b126be5bcf8b2cb3b3fa55e3399f2c02443ea74bd8704e8 (held).
```

If any count differs, CONCERN (and identify whether the delta is attributable to S-Auto-13b or to the inherited OQ-S68.1 / OQ-S41.5 baselines). The Java failure count must stay `1` (only the inherited tiebreaker).

### Axis I — hard-fence cumulative verification

```bash
git diff --stat bbd385e..HEAD -- \
  server/src/main/resources/skills/ \
  server/src/main/resources/config/tool-policy.yaml \
  server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java \
  eval_interactive/ \
  autoloop/autoloop/scoring/ \
  autoloop/autoloop/loop.py \
  autoloop/autoloop/sandbox/ \
  autoloop/autoloop/meta_agent/ \
  autoloop/autoloop/memory/ \
  autoloop/autoloop/preflight.py \
  autoloop/autoloop/cli.py \
  docs/foundational/ docs/current/ docs/runtime_freeze_and_risk_policy.md docs/teams/
# Expected: empty.

git diff --stat bbd385e..HEAD -- docs/sprints/sprint-0[0-6]*-* docs/milestones/
# Expected: empty EXCEPT none — sprint-069-handoff.md is a NEW file (this
#   sub-sprint's own archive), not a prior archive; prior sprint/milestone
#   archives must be byte-identical (immutable).
```

FAIL Axis I with cited paths if non-empty. Specifically confirm: the S-Auto-12 A1 `successfulDispatchCache` LOGIC is unchanged; `PhaseEvaluator.resolveMaxStepsReason` (B1 / S-Auto-14 territory) is UNTOUCHED; the S-Auto-13 A3 soft layer in `skills/**` is UNTOUCHED (it STAYS); the 4 SHA-locked scoring files are untouched (SHA reasserted in Axis H); `loop.py` / `applier.py` / sandbox / meta_agent are untouched; the OQ-S68.1 eval split-failures were NOT "fixed" here.

### Axis J — §7 stanza + Tier-0 self-invention check

- Confirm the §7 stanza is present in `docs/sprint_objective.md` (now being archived to `sprints/sprint-069-objective.md` at close) and the handoff §5 self-walk matches what shipped (each field filled concrete, not stretched).
- Confirm NO new Tier-0 invariant was self-invented (the gate is framed as §1.4 idempotency/cardinality; the §7 stanza explicitly routes a Tier-0-elevation argument to `human_review_required`). If YOU judge the gate warrants Tier-0 status, that is `needs human architecture decision`, not a silent approve.

## §4.2 Per-sub-sprint header (write at TOP of `docs/codex-findings.md`)

```
## Sprint 069 / S-Auto-13b — Per-sub-sprint Review Decision
decision: pass | fix_required | out_of_scope_review
blocking_count: <number>
summary: <one paragraph stating the verdict + key findings + §4.1 verdict>

### Axis A - §4.1 Nine-question Kernel: PASS / CONCERN / FAIL
<evidence + 1-sentence per-question verdict>

### Axis B - faq_miss-keyed not content-keyed: PASS / CONCERN / FAIL
<gate condition + tracker refresh evidence>

### Axis C - soft-signal-first tried + falsified: PASS / CONCERN / FAIL
<soft layer still in place + falsification evidence>

### Axis D - A1 logic unmodified + coexistence: PASS / CONCERN / FAIL
<A1 byte-identical region unchanged + disjoint annotations>

### Axis E - same-turn scope clear; cross-turn out of scope (OQ-S69.1): PASS / CONCERN / FAIL
<per-run tracker lifetime + no overclaim>

### Axis F - no false-positive distinct-search suppression: PASS / CONCERN / FAIL
<17-suppression distinct-need audit>

### Axis G - trace annotation completeness: PASS / CONCERN / FAIL
<ToolEvent fields + ControlKernel flatten + 17-event consistency>

### Axis H - test deltas + reproducibility: PASS / CONCERN / FAIL
<Java 1198/1 + eval 495/8 + autoloop 276 + 17-fixture 31 + scoring SHA>

### Axis I - hard-fence cumulative verification: PASS / CONCERN / FAIL
<git diff --stat output for gated paths>

### Axis J - §7 stanza + Tier-0 self-invention check: PASS / CONCERN / FAIL
<stanza present + no Tier-0 self-invented>

### §4.1 Verdict

`approve` | `approve with downgrade-to-signal follow-up` | `reject as semantic hardcode` | `needs human architecture decision`

Follow-up trigger (if applicable): <name + 1-sentence justification>
```

## Constraints (Codex MUST respect)

- **No code edits.** Codex provides verdicts + evidence; deliver-agent + human + dev close any fixes.
- **No adjudicating the milestone-level §11 PARAPHRASE_STORM interpretation** (within-turn vs total). That is a deliver-agent + human decision at M-Auto-3 close. The residual cross-turn count (OQ-S69.1) is NOT a S-Auto-13b sprint-level failure.
- **No re-judging bad-case manual review verdicts** (the M-Auto-3 close §5.6 review is deliver+human territory; not in this sub-sprint's scope).
- **No mvn / pytest reproduction beyond what's claimed** (you MAY verify counts; don't extend scope).
- **Per-sub-sprint Codex review prompt is `compact/sprint-069-codex-review-prompt.md`** (this file); verdict lands in `docs/codex-findings.md` per the §4.2 header convention; the deliver-agent archives at S-Auto-13b close to `docs/sprints/sprint-069-codex-review.md`.

## What this review DOES and DOES NOT cover

**DOES cover**:
- The 2-commit cumulative scope `bbd385e..HEAD` (= `1acd9b3` code + `a73e2e4` handoff).
- The A3 `faq_miss`-state-aware same-turn re-search suppression gate (anti-hardcode kernel + the 6 human-named focus checks).
- A1-coexistence + non-modification.
- Trace annotation completeness.
- Baseline preservation (Java / eval / autoloop / 17-fixture / scoring SHA).
- Hard-fence discipline.
- OQ-S68.3 resolution + OQ-S68.4 subsumption claim (logical-consistency only).

**DOES NOT cover**:
- The milestone-level §11 PARAPHRASE_STORM within-turn-vs-total interpretation (deliver+human at M-Auto-3 close).
- The cross-turn paraphrase storm fix (OQ-S69.1 — a future sub-sprint / §8.5 split decision, post-M-Auto-3).
- S-Auto-14 / B1 escalation-reason honesty (separate per-sub-sprint review).
- The M-Auto-3 milestone-shared close Codex (separate; bundles the cumulative S-Auto-11..14 range + the M-Auto-2 residual code).
- The OQ-S68.1 eval_interactive split-failures (separate housekeeping; not server-side; not this sub-sprint's surface).
- The bad-case §5.6 manual review (deliver+human at M-Auto-3 close).

## Decision conditions

- **`pass / 0 / approve`**: all 10 axes PASS; the deterministic-backstop justification holds (soft-signal-first tried+falsified, `faq_miss`-keyed-not-content, A1 untouched, no new Tier-0, fences clean). S-Auto-13b can flip → CLOSED; the deliver-agent proceeds to close maintenance + S-Auto-14 draft.
- **`pass / 0 / approve with downgrade-to-signal follow-up`**: axes PASS or PASS-WITH-CONCERN; a non-blocking follow-up trigger named (e.g., "relax the strict always-suppress form to an N-count cap if a future bad case surfaces a legitimate distinct second search"; or "OQ-S69.1 cross-turn carrier decision at M-Auto-3 close"). S-Auto-13b can close with the recommendation recorded.
- **`fix_required / >0 / reject as semantic hardcode`**: at least one axis FAIL with a §1.7 red line crossed (gate keys on query content; soft-signal-first not actually tried; A1 modified; §1.3 FIRST-search ownership shrunk). Targeted fix-iteration required BEFORE S-Auto-13b can close.
- **`fix_required / >0` (non-hardcode)**: axis FAIL on fence-creep / baseline-regression / annotation-incompleteness. Targeted fix-iteration scope discussion with deliver-agent + human.
- **`out_of_scope_review`**: review broadens beyond the sub-sprint scope. Surface to deliver-agent + human.
- **`needs human architecture decision`**: the per-turn-search-cardinality gate is judged to warrant Tier-0 status, or another unresolved governance question surfaces. Escalate (do NOT self-invent a Tier-0).

Begin review. Walk each axis in order. Write the verdict header at the TOP of `docs/codex-findings.md` before the axis bodies. Cite evidence with `file:line` references for all PASS / CONCERN / FAIL claims.
