---
title: Sprint 070 / S-Auto-14 dev handoff — evidence-aware resolveMaxStepsReason (max-steps escalation_reason honesty; B1)
doc_tier: sprint-archive
status: current
implementation_status: implemented
source_of_truth: this file
last_reviewed: 2026-06-02
review_cadence: ad hoc
notes: >
  Dev-authored handoff for S-Auto-14 (fifth and LAST sub-sprint of
  M-Auto-3, Substrate-hygiene). Makes PhaseEvaluator.resolveMaxStepsReason
  evidence-aware: a MAX_STEPS exit is stamped faq_miss_threshold_exceeded
  ONLY when the most-recent search_knowledge result was a genuine miss
  (faq_miss=true); a viable last hit (faq_miss=false) falls through to the
  existing turn_budget_exhausted catch-all. infra layer; no new enum; one
  lossy presence-heuristic REMOVED. The prescribed step-3 eval
  escalation_reason sync turned out to be a NO-OP (empirically): the L1
  escalation_compliance gate is family-based and faq_miss_threshold_exceeded
  + turn_budget_exhausted are both bot_limit siblings, so the re-stamp
  fails no case; editing the genuine-miss specs would mask bot churn (§5.4).
---

# Sprint 070 / S-Auto-14 / M-Auto-3 — dev handoff

## §0 Summary

**Scope (4 steps):**

1. **B1 — evidence-aware `resolveMaxStepsReason`** (`PhaseEvaluator.java`,
   step 3). Replaced the lossy `search_knowledge`-*presence* check with a
   read of the MOST-RECENT `search_knowledge` event's `faq_miss` flag off
   its `resultData()` (reusing the `Boolean.TRUE.equals(data.get("faq_miss"))`
   idiom at `:1173`; null/malformed-map guarded). `faq_miss=true` (genuine
   miss) → `faq_miss_threshold_exceeded`; viable last hit (`faq_miss=false`),
   no search, or null data → the existing `turn_budget_exhausted` catch-all.
   Step-1 (`incomplete_intake`) and step-2 (`clarification_budget_exhausted`)
   precedence UNCHANGED. **No new `escalation_reason` enum.** Javadoc updated.
2. **B1 unit tests** — rewrote `PhaseEvaluatorMaxStepsResolverTest` to the
   new contract (6 → 10 tests): the fix (viable last hit → `turn_budget`),
   genuine-miss negative control (`faq_miss=true` → faq), mixed-turn both
   directions (most-recent wins), null/malformed-data guard, no-search
   fallback, INTAKE precedence, clarification precedence, null-plan /
   null-session safety.
3. **Eval `escalation_reason` sync (§5.4)** — **ZERO cases synced**
   (empirically justified; see §2). The re-stamp does not break any case
   and syncing the genuine-miss specs would mask churn.
4. **3-pass `bad_cases` measurement + handoff** on a freshly-restarted
   `:8080` backend (B1 compiled in).

**Outcome:** `ESCALATION_MISSTAMP` (max-steps exit stamped
`faq_miss_threshold_exceeded` while the most-recent `search_knowledge`
returned a viable hit `faq_miss=false`) **dropped to 0** across the 3-pass
post-fix run — clearing the ≤1 acceptance bar (deliver-cited baseline
5/24; my same-methodology pre-fix re-measurement over the S-Auto-13b
3-pass = 3; both → **0**). Negative controls intact: genuine FAQ-miss
exhaustion still `faq_miss_threshold_exceeded` (fg5q, all passes; cs001
pass-3); intake still `intake_complete_for_uc_k` (cs066, all passes);
no-search exhaustion still `turn_budget_exhausted` (iwzx, all passes).

**Layer:** `infra` (which canonical reason a MAX_STEPS exit is LABELLED
with, using `faq_miss` evidence the runtime already had). No agent
semantic *decision* changed. **Secondary `eval_spec`:** analysed, no edit
required.

**Commits:**

- B1 code + tests (`PhaseEvaluator.java` resolver + Javadoc;
  `PhaseEvaluatorMaxStepsResolverTest`).
- this handoff — committed separately (commit-at-end; staged explicitly,
  no `git add -A`).

**Final test counts (re-measured at HEAD this sub-sprint):**

| Gate | Baseline (prompt) | This sub-sprint | Note |
|---|---|---|---|
| Java `mvn -pl server test` | `1198 / Failures 1 / 0 / 2` | **`1202 / Failures 1 / 0 / 2`** | +4 new B1 resolver tests; sole failure is the inherited `SystemPromptUserRequestedTiebreakerTest` (OQ-S41.5, unchanged) |
| eval_interactive pytest | `495 passed, 8 failed` | **`491 passed, 12 failed`** | **prompt baseline STALE in this env**; B1 is Java-only → 0 new Python failures. Delta is environmental + pre-existing — see §5 |
| autoloop pytest | `276 passed` | **`276 passed`** | unchanged (autoloop/ untouched) |
| 17-fixture anti-hardcode | `31 passed` | **`31 passed`** | unchanged |
| scoring SHA | `35305bd8…` | **`35305bd84402ac455b126be5bcf8b2cb3b3fa55e3399f2c02443ea74bd8704e8`** | unchanged (4 SHA-locked scoring files untouched) |

**Codex per-sub-sprint dispatch status:** **PENDING (deliver-agent at
close).** Trigger #1 (Tier-0-candidate surface
`R-escalation-reason-runtime-evidence-contract-review`) + §5.4-sensitive
eval surface (analysed → no edit). Verification focus: the resolver reads
the EXISTING `faq_miss` result flag (no new keyword/regex/enum/per-UC); no
new Tier-0 invariant; reuses the existing `turn_budget_exhausted` catch-all;
A1/A3 dispatch-path gates untouched; the zero-sync §5.4 disposition (family
check) is the §5.4-honest call.

## §1 The B1 resolver change (before / after)

`PhaseEvaluator.resolveMaxStepsReason(plan, result, session)` is called
ONLY from the `case MAX_STEPS` branch of the phase evaluator
(`PhaseEvaluator.java:606`). It resolves the canonical `escalation_reason`
for a `TerminalOutcome.MAX_STEPS` exit. Precedence (first match wins):
(1) INTAKE plan → `incomplete_intake`; (2) clarification count > 0 →
`clarification_budget_exhausted`; (3) FAQ attribution; (4) catch-all
`turn_budget_exhausted`.

**Before (step 3, lossy presence heuristic):**

```java
boolean searchedKnowledge = false;
for (ToolEvent te : result.toolEvents())
    if ("search_knowledge".equals(te.toolName())) { searchedKnowledge = true; break; }
if (searchedKnowledge) return "faq_miss_threshold_exceeded";
return "turn_budget_exhausted";
```

Any `search_knowledge` *presence* → `faq_miss_threshold_exceeded`, even
when the last search returned a viable hit. That mislabels "ran out of
budget with a usable answer in hand" as "missed the knowledge".

**After (step 3, evidence-aware):**

```java
ToolEvent lastSearch = null;
for (ToolEvent te : result.toolEvents())
    if ("search_knowledge".equals(te.toolName())) lastSearch = te;   // most-recent wins
if (lastSearch != null && lastSearch.resultData() instanceof Map<?, ?> data
        && Boolean.TRUE.equals(data.get("faq_miss")))
    return "faq_miss_threshold_exceeded";                            // genuine miss only
return "turn_budget_exhausted";                                      // viable hit / no search / null data
```

- Reads the SAME `faq_miss` flag the S-Auto-13b A3 gate keys on, but at
  **resolver time** off `ToolEvent.resultData()` — the A1/A3 dispatch-path
  gates are NOT touched.
- `Map<?,?>.get(...)` + `Boolean.TRUE.equals(...)` needs no unchecked cast
  and treats null/malformed maps as "no positive miss evidence" → catch-all.
- Steps 1/2 unchanged → precedence preserved (verified by the INTAKE and
  clarification unit tests).
- **No new enum value**; one lossy heuristic removed; net-lowers the
  hardcode surface.

## §2 Eval `escalation_reason` sync (§5.4) — ZERO cases, empirically justified

**Disposition: no `case_specs/**` edits.** The prescribed step-3 sync is a
no-op once the actual scorer is consulted. Three independent reasons, in
order of authority:

1. **The L1 gate is family-based, not exact-match.**
   `hard_checks.py:561-574` (`_check_escalation_compliance`) compares the
   bot's `escalation_reason` *family* to the spec's `escalation_trigger`
   *family*, not the literal strings (Codex 2026-05-04 round 4 "Exact
   Escalation Reason" fix). `_ESCALATION_REASON_FAMILY` (`:66-105`) maps
   **both** `faq_miss_threshold_exceeded` **and** `turn_budget_exhausted`
   to the `bot_limit` family. B1's faq→turn_budget re-stamp is a
   **same-family sibling swap** → `same_family == True` → the check does
   NOT fail.

2. **Empirically confirmed on the flipped cases.** Post-fix, cs014 emits
   `turn_budget_exhausted` in all 3 passes and `escalation_compliance.passed
   = true` in all 3 (spec `escalation_trigger: faq_miss_threshold_exceeded`,
   actual `turn_budget_exhausted`, both `bot_limit`). cs014 post-fix pass-1
   had **no** failing L1 hard check. No case flipped pass→fail because of B1.

3. **Editing the genuine-miss specs would VIOLATE §5.4.** The only
   bad_cases pinning `escalation_trigger: faq_miss_threshold_exceeded` are
   cs001 / cs011 / cs014 — all DESIGNED as genuine FAQ-miss cases ("the FAQ
   corpus has no covering article"). Their intended-correct behaviour is to
   recognise the miss and escalate `faq_miss_threshold_exceeded`. When the
   bot instead churns on a marginal viable hit and exits max-steps, B1
   honestly labels THAT run `turn_budget_exhausted` — but rewriting the
   spec's expectation to `turn_budget` would be *widening the spec to
   accept a worse bot run* (masking churn), the §5.4 red line. The
   family check already keeps these cases green without any masking.

**Neighbor suite unaffected:** `promotion/` pins
`faq_miss_threshold_exceeded` on 19 cases; all are protected by the same
`bot_limit` family check, so a max-steps viable-hit re-stamp there also
stays green with no edit.

**§5.4 ordering (GUARDRAIL 1) honoured:** the B1 bot fix landed and the
3-pass rerun was observed BEFORE this disposition was finalised; no
case-spec expectation was touched at any point.

### Per-case trace evidence (the cases B1 re-stamps; would-be sync targets)

| case (run) | pre-fix reason | post-fix reason | (i) last `search_knowledge` `faq_miss` | (ii) viable hit present | (iii) max-steps exit | classification |
|---|---|---|---|---|---|---|
| cs011 (pass-1) | `faq_miss_threshold_exceeded` | `turn_budget_exhausted` | `false` | yes (6× fm=false) | yes (synthetic `request_handover` step=-1) | correct relabel; spec kept (genuine-miss design) |
| cs014 (pass-1/2/3) | `faq_miss_threshold_exceeded`* | `turn_budget_exhausted` | `false` | yes | yes (step=-1) | correct relabel; spec kept |
| wmkb (pass-2/3) | `user_requested` / `turn_budget` | `turn_budget_exhausted` | `false` | yes | yes (step=-1) | correct relabel; no faq trigger to sync |

*cs014 pre-fix ended `faq_miss=false` on pass-1/2 (misstamp) and
`faq_miss=true` on pass-3 (genuine — correctly faq). B1 labels each run by
its own last-search evidence.

## §3 Negative controls

| control | case(s) | result | reading |
|---|---|---|---|
| genuine FAQ-miss exhaustion still faq | fg5q (all 6 runs); cs001 pass-3 | `faq_miss_threshold_exceeded`, last `faq_miss=true`, max-steps step=-1 | most-recent-miss branch preserved |
| intake still intake_complete | cs066 (all 6 runs) | `intake_complete_for_uc_k` unchanged | step-1/INTAKE path untouched |
| clarification precedence | (no live bad_case exercised it) | covered by unit test `clarificationCheckedBeforeSearch_mixedLoop` | step-2 precedence preserved |
| no-search exhaustion | iwzx (all 6 runs) | `turn_budget_exhausted` unchanged | catch-all unchanged for no-search |
| mixed-turn most-recent-wins | unit tests (both directions) | early-miss→viable-hit ⇒ turn_budget; early-hit→miss ⇒ faq | most-recent semantics pinned |
| real LLM handover untouched | alice pass-1 (`request_handover` step=1) | `faq_miss_threshold_exceeded` (LLM-supplied) | B1 only touches MAX_STEPS path, not agent_escalated |

## §4 `ESCALATION_MISSTAMP` 3-pass before/after + OQ-S67.2

**Methodology** (same pre & post): for each `bad_cases` session, read the
S-Auto-11-persisted per-iter trace (`results/<run>/results.json`); a
session is an `ESCALATION_MISSTAMP` iff `escalation_reason ==
faq_miss_threshold_exceeded` AND the most-recent `search_knowledge`
`result_data.faq_miss == false` AND the exit was a MAX_STEPS exit
(synthetic `request_handover` `step_index == -1`, i.e. NOT a real LLM
`request_handover` with `step_index >= 0`). Discriminator validated against
code: `ControlKernel` synthesises the trailing `request_handover` record
from the resolved `session.escalation_reason` for every escalation, so the
`step_index` is what separates a max-steps relabel (B1's domain) from an
LLM-chosen handover (not B1's domain).

| run | pass-1 | pass-2 | pass-3 | total |
|---|---|---|---|---|
| **PRE-FIX** (sprint-069 S-Auto-13b 3-pass) | cs011, cs014 (2) | cs014 (1) | 0 (cs011 was a real LLM handover step=3; cs014 genuine fm=true) | **3** |
| **POST-FIX** (sprint-070 S-Auto-14 3-pass) | 0 | 0 | 0 | **0** |

Deliver-cited baseline was **5/24** (a prior run); my same-methodology
re-measurement over the S-Auto-13b 3-pass = 3; either way **→ 0**, clearing
the ≤1 bar. Post-fix, every surviving `faq_miss_threshold_exceeded` had
`faq_miss=true` (genuine) or was a real LLM handover (alice, step=1) —
**zero misstamps**.

Result dirs: pre = `20260602-094229/094655/095148`
(`sprint-069-s-auto-13b-pass-1/2/3`); post =
`20260602-114022/114804/115332` (`sprint-070-s-auto-14-pass-1/2/3`).

**OQ-S67.2 re-measurement (did upstream A1/A3 lower max-steps EXITS?):**
both my pre-fix (S-Auto-13b) and post-fix (S-Auto-14) runs already have A1
(dedup) + A3 (paraphrase suppression) active, so they cannot *isolate*
A1/A3's effect on the raw max-steps EXIT count — they only bracket B1's
relabel. What they show: the `bot_limit`-family escalation count is stable
across the two runs (pre 11 / post 14 over 3 passes — within LLM
run-variance), and B1 **redistributes within** the family (faq→turn_budget)
without changing the total. Isolating A1/A3's exit-count effect needs a
pre-A3 baseline held by the deliver-agent; flagged as OQ-S70.2.

## §5 Baselines + §7-stanza self-walk + fence disposition

### Baselines re-measured at HEAD

- **Java `1202 / 1 / 0 / 2`** — baseline `1198/1/0/2` + 4 new B1 tests;
  `Failures` stays 1 (inherited `SystemPromptUserRequestedTiebreakerTest`,
  OQ-S41.5). No new failure.
- **eval_interactive pytest `491 / 12`** — prompt baseline `495/8` is
  **stale in this session env**. B1 is Java-only (the Python suite imports
  no Java and reads no file this sub-sprint touched) → contributes **0**
  new failures. Decomposition of the 12: 4× action_bank-split
  `TestRItemClosuresRecordedInActionBank` (the documented OQ-S68.1 set);
  5× `test_corpus_lint` — **environmental** (`ModuleNotFoundError: No
  module named 'eval_interactive.case_spec'` because the test's subprocess
  resolves to `miniconda3/bin/python`, not the uv venv); 2×
  `test_case_spec_overrides`; 1× `test_session_create_timeout_constants_
  widened_to_120s` (config-constant drift, `assert 90.0 == 60.0`). None
  touch the agent runtime, escalation, or `PhaseEvaluator`. Per the
  re-measure-baselines discipline: delta attributed, in-scope work NOT
  reverted to match a stale number. Surfaced as OQ-S70.1; out of scope to
  fix (OQ-S68.1 carve-out).
- **autoloop pytest `276` / 17-fixture `31` / scoring SHA
  `35305bd84402ac455b126be5bcf8b2cb3b3fa55e3399f2c02443ea74bd8704e8`** —
  all unchanged (no autoloop/scoring files touched).

### §7 stanza self-walk

## Layer-classification + anti-hardcode stanza

**Target failure layer:** `infra` (`resolveMaxStepsReason` evidence-aware) +
`eval_spec` (analysed; no case_spec edit required). No agent semantic
*decision* changed; B1 changes which canonical reason a MAX_STEPS exit is
LABELLED with, using the `faq_miss` evidence the runtime already has.

**Tier-0 invariant:** NONE added. Stays `infra`; reuses the existing
`turn_budget_exhausted` catch-all. Touches the Tier-0-*candidate*
`R-escalation-reason-runtime-evidence-contract-review`; no Tier-0
elevation argument raised → no `human_review_required` triggered. Not added
to `runtime_freeze_and_risk_policy.md` §1/§2.

**Semantic hardcode:** None introduced; one REMOVED. B1 deletes the lossy
`search_knowledge`-presence → faq-label heuristic and replaces it with a
read of the EXISTING `faq_miss` flag + fall-through to the existing
catch-all — net-LOWERS the hardcode surface. No new keyword/regex/enum/
per-UC; no new `escalation_reason` value. The eval analysis expects the
CORRECTED reason and made no spec edit (§5.4 — not widening to accept a
bug).

**Generalization coverage:** target / neighbor / negative / shadow case
counts: ESCALATION_MISSTAMP bad_cases 3→0 (target: cs011/cs014; 3-pass) /
`promotion/` 19 faq-pinned cases protected by family check (neighbor) /
genuine-miss fg5q + cs001 + intake cs066 + no-search iwzx + mixed-turn
both-directions unit tests (negative) / shadow held-out (read by
human/review-agent, not consumed here). Counts confirmed at handoff via
the 3-pass rerun (3→0) + §2/§3 tables.

### Fence disposition

- **Edited (in scope):** `PhaseEvaluator.java` (`resolveMaxStepsReason` +
  its Javadoc only); `PhaseEvaluatorMaxStepsResolverTest`.
- **Analysed, NOT edited (in scope but no change needed):**
  `eval_interactive/case_specs/**` — zero sync (§2).
- **Hard-fenced, untouched:** S-Auto-12 A1 `successfulDispatchCache` +
  S-Auto-13b A3 `lastSearchKnowledgeViableHit` gate in `AgentRunLoopImpl`
  (B1 reads `faq_miss` at resolver time, not the dispatch-path gates);
  `skills/**`; `tool-policy.yaml`; `user_simulator.py`; the 4 SHA-locked
  scoring files; `autoloop/**` / `sandbox/**` / `meta_agent/**` /
  `cli.py` / `preflight.py`; `docs/foundational/**`, `docs/current/**`,
  `docs/runtime_freeze_and_risk_policy.md`, `docs/teams/**`; prior
  sprint/milestone archives. `b351648` determinism config not reverted.
- **OQ-S69.1 cross-turn paraphrase storm: NOT touched** (separate surface;
  per-`BotSession` state, not the per-run/resolver path; carrier-decided
  at M-Auto-3 close).
- **OQ-S68.1 4 eval split-failures: NOT touched** (separate housekeeping).
- No `git add -A`; staged explicitly; backend restarted for the
  measurement; local-Mac only.

## §6 Open Questions surfaced

- **OQ-S70.1 — eval_interactive pytest baseline drift in this env (495/8 →
  491/12).** 5× `test_corpus_lint` fail because the test's subprocess uses
  `miniconda3/bin/python` (no `eval_interactive.case_spec` module), and 1×
  `test_session_create_timeout` is a config-constant drift (90 vs 60). B1
  contributes none. Owner: deliver-agent / human — decide whether to
  re-pin the documented baseline or fix the corpus_lint subprocess
  interpreter resolution. Out of scope to fix here.
- **OQ-S70.2 — A1/A3 max-steps-exit-count attribution.** This sub-sprint's
  pre/post runs are both post-A3, so they bracket B1's relabel but cannot
  isolate A1/A3's effect on the raw max-steps EXIT count (OQ-S67.2). Needs
  a pre-A3 baseline (deliver-agent holds S-Auto-10/11 numbers).
- **OQ-S70.3 (observation, not a B1 defect) — cs011 sometimes escalates
  `user_requested` (pass-2/3, pre AND post).** That is a cross-family
  (`user_intent` vs `bot_limit`) L1 escalation_compliance FAIL, present in
  the pre-fix run too — a `semantic_planner` question (why the bot
  picks user_requested on a faq-miss case), NOT a B1/`infra` issue. Logged
  for the deliver-agent's bad-case suite.

## §7 Self-check tick-off

- [x] `resolveMaxStepsReason` step 3 reads most-recent `search_knowledge`
  `faq_miss` (null/malformed guarded); `false`/null→`turn_budget_exhausted`,
  `true`→`faq_miss_threshold_exceeded`; step-1/2 UNCHANGED; Javadoc updated.
- [x] NO new `escalation_reason` enum value; reuses `turn_budget_exhausted`.
- [x] B1 tests cover fix + negatives (genuine miss / intake / clarification /
  mixed-turn both directions / no-search / null-data guard / null plan /
  null session) — 10/10 pass.
- [x] Eval `escalation_reason` sync MINIMAL + empirical (post-B1): ZERO
  cases — family-based L1 gate absorbs the bot_limit sibling swap; genuine-
  miss specs left intact (editing them would mask churn, §5.4); each
  candidate case justified in §2; no case widened to accept a bot mistake.
- [x] `ESCALATION_MISSTAMP` 3-pass → **0** (≤1; deliver baseline 5/24 → 0),
  backend restarted, persisted traces read.
- [x] A1/A3 gates, skills, tool-policy, user_simulator, loop.py, scoring,
  sandbox, meta_agent UNTOUCHED; `b351648` not reverted; no Tier-0
  self-invented; OQ-S69.1 + OQ-S68.1 NOT touched.
- [x] Java no new failures beyond `1198/1/0/2` (now `1202/1/0/2`, +4 B1
  tests); eval pytest `491/12` (prompt `495/8` stale; B1 adds 0 — §5/OQ-S70.1);
  autoloop `276`; 17-fixture `31`; scoring SHA `35305bd8…`.
- [x] No `git add -A`; backend restarted for measurement; local-Mac only.
- [x] Handoff §0–§7 filled (incl. §2 §5.4 table + M-Auto-3 close-readiness
  below); Codex dispatch status noted (PENDING, deliver-agent at close).

## M-Auto-3 close-readiness (LAST sub-sprint summary)

S-Auto-14 is the 5th and final sub-sprint of M-Auto-3 (Substrate-hygiene):
S-Auto-11 (`loop.py` per-iter trace persistence) → S-Auto-12 (A1
byte-identical dedup) → S-Auto-13 (A2 classify-first + A3 soft layer) →
S-Auto-13b (A3 deterministic backstop) → **S-Auto-14 (B1 escalation-reason
honesty)**.

**Gates met by this sub-sprint:** `ESCALATION_MISSTAMP` 5/24 → 0
(B1, this sub-sprint); Java baseline preserved + 4 new tests; scoring SHA /
autoloop / 17-fixture unchanged.

**Storm signal (from S-Auto-13b, unchanged here):** within-turn
`PARAPHRASE_STORM` 6/5/1 → 0/0/0 (A3 backstop, MET). Cross-turn
`PARAPHRASE_STORM` is **OQ-S69.1** — out of scope for the per-run gate
(needs `BotSession`-scoped state); a carrier decision for the milestone
close, NOT touched here. Total (within + cross) therefore still gated on
the OQ-S69.1 cross-turn decision.

**Open carrier decisions for the M-Auto-3 close (deliver-agent + human,
§8.5 / §11 — NOT dev-owned):**

1. **OQ-S69.1** — whether cross-turn paraphrase storm is in-scope for
   M-Auto-3 (split a new sub-sprint) or deferred to a follow-on milestone.
2. **OQ-S68.1 / OQ-S70.1** — the eval_interactive pytest Python-baseline
   decision (action_bank split + the corpus_lint subprocess-interpreter
   env failure surfaced this sub-sprint): re-pin the documented baseline
   or fix the corpus_lint subprocess interpreter resolution.
3. Per-sub-sprint Codex review for S-Auto-14 (§4.3 trigger #1) is PENDING —
   deliver-agent authors `compact/sprint-070-codex-review-prompt.md` at close.

All M-Auto-3 module fixes (A1/A2/A3/B1) are landed and substrate-hygiene's
named substrate bugs (identical-tool-retry-storm, paraphrase-storm
within-turn, escalation-reason-misstamp-maxsteps-faq) are closed at the
runtime layer; the residual open items above are scope/baseline decisions,
not unlanded dev work.
