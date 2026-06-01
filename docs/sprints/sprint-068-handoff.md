---
title: Sprint 068 / S-Auto-13 dev handoff — A2 classify-first + A3 paraphrase discipline + OQ-S66.1 goldens
doc_tier: sprint-archive
status: current
implementation_status: implemented
source_of_truth: this file
last_reviewed: 2026-06-02
review_cadence: ad hoc
notes: >
  Dev-authored handoff for S-Auto-13 (third sub-sprint of M-Auto-3,
  Substrate-hygiene). Closes the two remaining upstream step-wasters at
  the skill/projection layer: A2 DISCOVER gating-race (classify-first) and
  A3 RESOLVE paraphrase-storm (faq_miss-keyed grounding + projection echo).
  In-scope consequence: OQ-S66.1 max_tool_steps golden reconciliation.
---

# Sprint 068 / S-Auto-13 / M-Auto-3 — dev handoff

## §0 Summary

**Scope (4 steps, all delivered):**

1. **A2 — `discover_triage.yaml` classify-first.** DISCOVER now commits the
   use case FIRST instead of instructing a pre-classification
   `search_knowledge` (which the tool policy rejects while
   `active_use_case=none`). The `faq-uc-search-before-commit` critical step
   was reconciled in place to `faq-uc-classify-first` (trace_check now
   rewards the classify commit, not search-before-classify). No
   `tool-policy.yaml` edit needed (skill-only, A2-A1 route).
2. **A3 — `resolve_faq_grounded_answer.yaml` paraphrase discipline + projection
   echo.** Grounding instruction now discourages re-search after a
   `faq_miss=false` viable hit; `ContextProjectionBuilder` emits a soft
   `search_reuse_instruction` (loop-path analogue of the legacy
   `knowledge_instruction`) when a prior `search_knowledge` already landed a
   viable hit this turn. Soft-signal-first — **no hard search-count cap**.
3. **OQ-S66.1 `max_tool_steps` golden reconciliation.** Stale goldens synced
   to shipped YAML values (`resolve_faq` 4→6, `discover_triage` 2→3).
4. **3-pass `bad_cases` measurement** (this handoff §1/§2/§3).

**Outcome:** A2 GATING_RACE target **MET** (DISCOVER-phase 0/1/0, aggregate 1
≤ target 1; RESOLVE-phase `'none'` rejections also dropped 5→0). OQ-S66.1
goldens reconciled (Java `Failures 10→1`). A3's soft signal fires + reaches
the LLM but the LLM ignores it — PARAPHRASE_STORM **NOT** at the `≤3` target
(16/16/7); per contract **no hard cap was added** → STOP-and-surface
(OQ-S68.3). Per ship-safe-part-and-surface, the sub-sprint is not paused; the
A3 soft layer is kept as the correct first measure.

**Commits:**

- `d4e3c61` — A2 + A3 + OQ-S66.1 code + tests.
- this handoff — committed separately as the sole remaining S-Auto-13 file
  (commit-at-end; staged explicitly, no `git add -A`).

**Final test counts (re-measured at HEAD this sub-sprint):**

| Gate | Baseline (prompt) | This sub-sprint | Note |
|---|---|---|---|
| Java `mvn -pl server test` | `1186 / Failures 10 / 0 / 2` | **`1192 / Failures 1 / 0 / 2`** | 9 OQ-S66.1 goldens cleared; +6 new A2/A3 tests; inherited `SystemPromptUserRequestedTiebreakerTest` (1) remains |
| eval_interactive pytest | `499 passed, 4 failed` (stale) | **`495 passed, 8 failed`** | Δ = +4 failures are PRE-EXISTING at HEAD, **disjoint from this sub-sprint** (see §5) — the 2026-06-01 action_bank split moved closed R-items to `action_bank_archive.md`; the prompt baseline predates the split |
| autoloop pytest | `276 passed` | **`276 passed`** | unchanged |
| 17-fixture anti-hardcode | `31 passed` | **`31 passed`** | unchanged |
| scoring SHA | `35305bd8…` | **`35305bd84402ac455b126be5bcf8b2cb3b3fa55e3399f2c02443ea74bd8704e8`** | unchanged |

## §1 A2 — DISCOVER gating-race (classify-first)

### Before / after — `$.procedure` (Sprint 7 §I0 weak-candidate cue)

**Before** (search-before-classify — the gating-race driver):

> Instead, gather enough evidence to classify toward the right FAQ-path UC:
> call `search_knowledge` with the user's question as the query, then call
> `classify_use_case` with the most plausible UC (…). Once classified,
> RESOLVE will run the grounded resolve sequence.

**After** (classify-first):

> Instead, classify FIRST toward the right FAQ-path UC: call
> `classify_use_case` with the most plausible UC (…). Do NOT call
> `search_knowledge` before `classify_use_case` in DISCOVER — while no use
> case is committed (active_use_case=none) a knowledge search is outside the
> tool's policy scope and only wastes a step; the grounded `search_knowledge`
> runs in RESOLVE once the FAQ-path UC is committed. Once classified, RESOLVE
> will run the grounded resolve sequence.

Substring anchors preserved for `Sprint7CandidateUseCasesProjectionTest`
substring checks (`Sprint 7`, `candidate_use_cases`, `UNKNOWN`,
`payment`/`sale-proceeds`, `UC-F`, `faq_miss_threshold_exceeded`,
`search_knowledge`, `classify_use_case`). The two misleading assertion
*messages* in `forbidsPrematureFaqMissHandover` were updated to classify-first
semantics (the substring checks themselves are unchanged and still pass).

### `faq-uc-search-before-commit` disposition

Reconciled **in place** (not removed) → renamed `faq-uc-classify-first`:

- `desc`: now says DISCOVER commits the UC first; a pre-classification
  `search_knowledge` is rejected by tool-policy (active_use_case=none) and
  wastes a step; retrieval-grounded disambiguation happens in RESOLVE.
- `trace_check`: `tool_event_seq(search_knowledge) < tool_event_seq(classify_use_case)`
  → **`accumulated_tool_results.classify_use_case`** (rewards the classify
  commit, not search-before-classify — the contradiction is gone).
- `severity: advisory`, `mandatory_for: [UC-A..UC-FP]` unchanged.

**Why in-place, not removed:** `SkillCriticalStepsLoadingTest` asserts
`discover_triage` has exactly **3** critical steps and **18** total across the
6 skills (S-Eval-3 architectural count). Removing the step would break that
count golden (an intended count, not a stale value) → editing it in place
preserves the invariant (count stays 3 / 18) and is the minimal reconciliation.

### Tool-policy alignment (no edit)

`tool-policy.yaml` is unchanged. `search_knowledge.allowed-ucs: [UC-A..UC-FP]`
(no `none`) and `classify_use_case.allowed-ucs: [ALL]` already encode the
asymmetry; A2 re-orders the **skill procedure** to match the existing policy
(A2-A1 skill-only route). The A2-A2 policy-change route was NOT taken (no
STOP-and-surface needed; skill-only fully resolves the contradiction).

### GATING_RACE evidence

Detector: `search_knowledge` tool_call with `success=false` and
`error_message` containing `not allowed for use case 'none'` (the DISCOVER
gating-race symptom). Methodology applied identically to the pre-fix and
post-fix runs via `/tmp/analyze_signals.py`.

**Pre-fix reference** (run `20260601-131542`, 12-case bad_cases, 1 pass on the
stale pre-A2/A3 backend):

- GATING_RACE = **7** total `'none'` rejections (DISCOVER-phase subset = 2:
  `cs012` turn0, `fg5q` turn0; the other 5 are RESOLVE-phase `'none'`
  rejections where the UC had not been stamped).

**Post-fix (3 passes, fresh backend with A2/A3 — runs `20260601-180756`,
`-181320`, `-181722`):**

| signal | pass 1 | pass 2 | pass 3 | aggregate |
|---|---|---|---|---|
| GATING_RACE total (`'none'` rejections, any phase) | 0 | 1 | 0 | **1** |
| GATING_RACE **DISCOVER-phase** subset (A2 target) | 0 | 1 | 0 | **1** |

**A2 GATING_RACE target MET.** DISCOVER-phase gating-race fell from a pre-fix
~2/pass to **1 across all 3 passes** (the lone residual: `fg5q` turn0 in pass
2 — the LLM occasionally still elects a DISCOVER search; it owns the choice
§1.3, the procedure soft-guides). Target was `4 → ≤1`; aggregate = 1 ✓.

**Bonus:** the RESOLVE-phase `'none'` rejections (5 in the pre-fix reference,
where the UC was never stamped before RESOLVE) dropped to **0** across all 3
passes — classify-first commits the UC so RESOLVE no longer searches with
`active_use_case=none`.

## §2 A3 — RESOLVE paraphrase-storm (faq_miss-keyed discipline + echo)

### Before / after — `$.grounding_instruction`

**Appended** (the rest of the instruction is unchanged):

> After a search_knowledge returns a viable hit (faq_miss=false), do NOT
> re-search this turn — draft your grounded answer from the existing hits via
> resolve_article, or escalate; a fresh search_knowledge is only warranted if
> the prior result was faq_miss=true or your new query is materially different
> from what you already searched.

### Projection echo — `ContextProjectionBuilder.build(...)`

Added in the loop-path `build(...)` after the `accumulated_tool_results`
injection. It is the **run-loop-path analogue** of the legacy
`knowledge_instruction` snippet (`buildProjection` ~:738), which only fires
when `knowledgeHits` is pre-loaded — never the case in the loop, where
knowledge arrives via `accumulated_tool_results`. When the prior
`accumulated_tool_results.search_knowledge.faq_miss == false`:

- `prior_search_knowledge_viable_hit: true`
- `search_reuse_instruction`: "A prior search_knowledge in this turn already
  returned a viable hit (faq_miss=false) … Do NOT call search_knowledge again
  this turn — draft … via resolve_article … or escalate. A fresh
  search_knowledge is only warranted if the prior result was faq_miss=true or
  your new query is materially different…"

LLM owns whether to re-search (§1.3 / §1.5 soft-signal-first); the runtime
does **not** block a re-search dispatch on this slot.

### Distinction from S-Auto-12 A1

A1 was intended to dedup BYTE-IDENTICAL repeats (same
`canonicalArgumentsHash`); A3 targets NON-identical PARAPHRASE repeats
(different query string, same intent) — A1 cannot catch those. The detector
below counts only re-searches with a query **different** from a prior
same-session search that already returned `faq_miss=false` (a byte-identical
repeat is therefore NOT counted as a paraphrase by this detector).

**Secondary observation (OQ-S68.4):** a byte-identical re-search slipped
through in the post-fix runs — `cs011` turn1 step1 and step2 are identical
(query `'password reset email not received after adding safe senders'`,
`uc_tags=['UC-D']`) yet both dispatched `success=true`. A1's idempotency
回挡 did not block step2. This is an A1-scope observation (S-Auto-12
FINALIZED / hard-fenced — NOT touched here); surfaced for the deliver-agent /
S-Auto-14 owners.

### PARAPHRASE_STORM evidence

Detector: a successful `search_knowledge` whose `query` differs from a prior
same-session successful search that already returned `faq_miss=false`.

**Pre-fix reference** (run `20260601-131542`, 1 pass, stale backend):

- PARAPHRASE_STORM = **18** of 40 successful searches, across `alice`(5),
  `cs001`(1), `cs014`(3), `cs015`(1), `cs095`(2), `fg5q`(3), `iwzx`(1),
  `wmkb`(2).

**Post-fix (3 passes, fresh backend with A2/A3):**

| signal | pass 1 | pass 2 | pass 3 | pre-fix ref (1 pass) |
|---|---|---|---|---|
| PARAPHRASE_STORM (non-identical re-search after `faq_miss=false`) | 16 | 16 | 7 | 18 |
| of successful searches | /32 | /31 | /23 | /40 |

### ⚠️ A3 PARAPHRASE_STORM target NOT met — STOP-and-surface (no hard cap added)

The soft `faq_miss`-keyed grounding instruction + projection echo did **NOT**
bring the paraphrase storm to the `≤3` target (16 / 16 / 7 vs `11/24 → ≤3`).
Per the contract's explicit instruction — *"Soft-signal-first (§1.5) — do NOT
add a hard search-count cap as the primary fix; if soft-signal + echo are
insufficient, STOP-and-surface before adding an A3-cap"* — **no hard cap was
added**; this is surfaced for the deliver-agent + human (OQ-S68.3).

**Diagnosis (why the soft signal is insufficient, not why it didn't fire):**

1. The echo **fires and reaches the LLM**: `search_reuse_instruction` appears
   15–17×/run and `prior_search_knowledge_viable_hit` 12–16×/run in the
   persisted per-step projections.
2. The run loop **rebuilds the projection per step** (`AgentRunLoopImpl:189`
   `for step` loop → `:211` `build(...)` with updated
   `accumulatedToolResults`), so each re-search is preceded by a projection
   that carries the echo. Example `cs011` turn1: searches at steps 0/1/2/3 in
   **separate LLM responses**; step0 returned `faq_miss=false`, the echo was
   present in steps 1–3, and the LLM re-searched anyway.
3. Therefore the LLM (`deepseek-v4-flash`) is **ignoring** the soft echo +
   grounding instruction at this rate. A stronger lever (a structural
   cardinality backstop / runtime dedup extension, or a different projection
   treatment) is a deliver-agent + human design decision, not a unilateral
   dev cap.

The A3 soft signal is **kept** (it is the correct first layer, is harmless,
fires correctly, and run 3 shows it has *some* effect — 7 vs pre-fix 18); it
is simply insufficient on its own. This follows the
ship-the-safe-part-and-surface posture (do not pause the sub-sprint; A2 +
OQ-S66.1 stand).

### No-hard-cap confirmation

**No hard search-count cap was added.** `resolve_faq` `max_tool_steps` remains
6 (unchanged). The fix is a soft `faq_miss`-keyed grounding instruction + a
soft projection echo only.

## §3 Negative controls

The contract names three negative controls; each is preserved by construction
and verified against the post-fix traces:

1. **Distinct RESOLVE searches not suppressed** — the PARAPHRASE detector and
   the `search_reuse_instruction` echo only act on a re-search whose query
   differs from a prior `faq_miss=false` query; the *first* search of a turn,
   and a genuinely-distinct topic search, are never flagged.
2. **`faq_miss=true` re-search allowed** — the echo fires only when
   `faq_miss == false`. After a `faq_miss=true` miss the echo is absent, so a
   fresh search remains warranted (covered by
   `Sprint068ClassifyFirstParaphraseTest.a3_projectionEcho_suppressedOnFaqMiss`).
3. **DISCOVER clarify-then-classify not broken** — A2 only re-orders the
   weak-candidate FAQ path (classify instead of pre-search); the
   clarify-question path, the Sprint 33 ad-status disambiguation cue, the
   `alternate_candidate_use_cases` / `discover_disambiguation_signals` slots,
   and the confidence guidance are all unchanged in the procedure.

**Post-fix trace confirmation (3 passes):**

1. **Distinct RESOLVE searches not suppressed** — the first search of every
   turn dispatched normally across all 36 case-runs; the detector + echo only
   act on a re-search whose query differs from a prior `faq_miss=false` query.
   No legitimate first/distinct search was blocked (runtime never gates on the
   echo slot).
2. **`faq_miss=true` re-search allowed** — confirmed on `cs014` turn0: after
   step0's `faq_miss=false` viable hit, the bot re-searched at steps 1/2/4/5,
   all returning `faq_miss=true`; those re-searches were permitted (the echo
   only fires on `faq_miss=false` and never blocks dispatch). The unit test
   `a3_projectionEcho_suppressedOnFaqMiss` covers the echo-suppression side.
3. **DISCOVER clarify-then-classify not broken** — DISCOVER turns still
   produced clarifying questions and `classify_use_case` commits across the
   suite; the Sprint 33 ad-status cue, `alternate_candidate_use_cases` /
   `discover_disambiguation_signals` slots, and confidence guidance are
   unchanged in the procedure (A2 only re-ordered the weak-candidate FAQ path).
4. **Byte-identical repeats not mis-counted** — the paraphrase detector
   excludes byte-identical re-searches (e.g. `cs011` step1==step2), so the
   16/16/7 figures are genuine non-identical paraphrases, not double-counts.

## §4 OQ-S66.1 `max_tool_steps` golden reconciliation

Root cause: commit `7871c62` ("update budget and pii Sanitizer") bumped both
skill caps (`discover_triage` 2→3, `resolve_faq` 4→6) but updated only
`PhaseEvaluatorSkillIntegrationTest`, leaving two other test classes' goldens
stale. This is a **stale-value sync**, NOT an intended cap — verified via git
blame; the YAML bump was the deliberate change.

Shipped YAML values taken as source of truth (A2/A3 did **not** change either
`max_tool_steps`). Golden tests updated:

| Test (golden) | Was | Now | Skill |
|---|---|---|---|
| `PhaseEvaluatorPlanTest.plan_resolveFaqUc_returnsFullPlan` :87 | 4 | 6 | resolve_faq |
| `PhaseEvaluatorPlanTest.plan_discoverPhase_returnsDiscoverPlan` :265 | 2 | 3 | discover_triage |
| `PhaseEvaluatorResolveSkillIntegrationTest.assertGoldenFaq` :151 (×7 FAQ UC tests) | 4 | 6 | resolve_faq |

Plus the two exact-match text goldens that A2/A3 necessarily touched:
`PhaseEvaluatorSkillIntegrationTest.DISCOVER_SYSTEM_INSTRUCTION` (A2 procedure)
and `PhaseEvaluatorResolveSkillIntegrationTest.FAQ_GROUNDING` (A3 grounding).

**Java before/after:** `Failures: 10 → 1` (9 goldens cleared; inherited
`SystemPromptUserRequestedTiebreakerTest` remains the documented baseline).
**No** edit to `PhaseEvaluator.resolveMaxStepsReason` logic (that is
B1 / S-Auto-14 — only golden TEST expectations were touched). No STOP-and-
surface: every changed golden was a stale-value or A2/A3-text sync, none
encoded an intended cap (§5.4 clear).

## §5 Baselines, §7-stanza self-walk, fence disposition

### Re-measured baselines + delta attribution

Per the re-measure-baselines discipline, all five gates were re-measured at
HEAD this sub-sprint (see §0 table). The one delta from the prompt's stated
baselines — eval_interactive `4 → 8` failures — is **fully attributed and
disjoint from this sub-sprint**:

- All 8 changed/new files are under `server/`. The 4 extra-failing tests
  (`TestRItemClosuresRecordedInActionBank::test_r_item_closure_annotated[…]`)
  read `docs/action_bank.md` and assert closed-R-item annotations there.
- Those R-items (e.g. `R-l1-source-citation-quality-rubric`) were moved to
  `docs/action_bank_archive.md` by the 2026-06-01 action_bank split, so they
  no longer appear in `action_bank.md` → the tests fail. Confirmed by grep:
  `R-l1-source-citation-quality-rubric` is present in `action_bank_archive.md`
  and absent from `action_bank.md`.
- The total stays 503 (495+8 = 499+4); the prompt's 499/4 baseline predates
  the split. **This sub-sprint adds zero eval_interactive regressions.**
- Surfaced as **OQ-S68.1** (§6) — the eval test should read the archive; out
  of S-Auto-13's skill/projection scope (do not fix here).

### §7 stanza self-walk

- **Target failure layer:** `prompt_projection`/skill (A2 procedure; A3
  grounding_instruction + projection echo) + `semantic_planner` (A3 — LLM owns
  whether to re-search) + config-governance (tool-policy alignment context).
  No runtime semantic-decision logic added. ✓ (matches delivered code)
- **Tier-0 invariant:** none added. ✓
- **Semantic hardcode:** none. A2 RE-ORDERS the skill procedure to match the
  EXISTING tool-policy (removes a self-contradiction; no keyword/regex/enum/
  per-UC matrix; LLM still owns the UC choice §1.3). A3 is a soft
  `faq_miss`-keyed grounding instruction + projection echo (LLM owns whether to
  re-search; soft-signal-first §1.5; no hard cap). OQ-S66.1 aligns stale TEST
  expectations to shipped behaviour. **Net: LOWERS the hardcode surface**
  (removes a contradicting search-before-classify cue). ✓
- **Generalization coverage:** target = `GATING_RACE` + `PARAPHRASE_STORM` on
  the 12-case bad_cases suite (3 passes); neighbor = same shape across the
  UC-FP/UC-C/UC-A cases; negative = §3 (distinct searches not suppressed,
  `faq_miss=true` re-search allowed, DISCOVER clarify-then-classify intact);
  shadow = held-out (not consumed by dev). ✓

### Fence disposition

No hard-fenced surface was touched:
`PhaseEvaluator.resolveMaxStepsReason` LOGIC (untouched — only golden TEST
expectations), `AgentRunLoopImpl` / `ToolEvent` / `ControlKernel` /
`system_prompt.txt` (S-Auto-12 FINALIZED — untouched), `eval_interactive/
case_specs/**` (untouched), `user_simulator.py` (untouched), the 4 SHA-locked
scoring files (SHA preserved), `loop.py` / `applier.py` / sandbox /
meta_agent / `cli.py` / `preflight.py` (untouched), `docs/foundational/**` /
`docs/current/**` / `docs/runtime_freeze_and_risk_policy.md` / `docs/teams/**`
/ prior archives (untouched). Edits confined to in-scope surfaces:
`discover_triage.yaml`, `resolve_faq_grounded_answer.yaml`,
`ContextProjectionBuilder.java` (A3 echo), `server/src/test/**`. No
`git add -A`; staged explicitly; autoloop runs were on a clean committed tree;
backend restarted before measurement; local-Mac only.

## §6 Open questions surfaced

- **OQ-S68.1** — `eval_interactive/tests/test_s_eval_5_l3_repositioning.py::
  TestRItemClosuresRecordedInActionBank` reads `docs/action_bank.md` for
  closed-R-item annotations, but the 2026-06-01 action_bank split moved those
  rows to `docs/action_bank_archive.md`. 4 tests fail at HEAD (pre-existing,
  disjoint from S-Auto-13). Fix: point the test at the archive (or both).
  Layer: `eval_spec` / test-maintenance. Deferred (out of S-Auto-13 scope).
- **OQ-S68.2** — `GATING_RACE` has a RESOLVE-phase component (search rejected
  with `'none'` while the UC was never stamped — e.g. `cs012`/`cs015`/`fg5q`
  in the pre-fix run). A2 targets the DISCOVER-phase subset; the RESOLVE-phase
  `'none'` rejections are a separate "UC not committed before RESOLVE" symptom
  (the planner/skill_state surface). Flag for M-Auto-3 review whether a
  follow-on item is warranted. Layer: `semantic_planner` / `skill_state`.

- **OQ-S68.3 (load-bearing — A3 STOP-and-surface)** — A3's soft `faq_miss`-keyed
  grounding instruction + projection echo did NOT reach the PARAPHRASE_STORM
  `≤3` target (post-fix 16 / 16 / 7 across 3 passes; pre-fix ref 18). The echo
  demonstrably fires per-step and reaches the LLM (15–17 `search_reuse_instruction`
  occurrences/run), and the re-searches occur across separate steps (so the
  echo precedes them) — the LLM (`deepseek-v4-flash`) simply ignores the soft
  signal at this rate. Per contract, **no hard cap was added**; the
  deliver-agent + human decide the next lever (a structural cardinality
  backstop / runtime dedup extension, a stronger non-soft projection treatment,
  or accepting the soft partial). Layer: `semantic_planner` (LLM ignoring a
  valid soft signal) + possible `prompt_projection` escalation.
- **OQ-S68.4 (A1 scope)** — a byte-identical re-search dispatched `success=true`
  despite A1's idempotency 回挡 (`cs011` turn1 step1==step2, identical query +
  `uc_tags`). A1 is S-Auto-12 FINALIZED / hard-fenced (NOT touched). Surfaced
  for S-Auto-14 / A1 owners to confirm whether A1's dedup window is per-step,
  per-tool, or otherwise scoped such that this cross-step identical repeat
  escaped. Layer: runtime dedup (A1).

## §7 Self-check tick-off

- [x] A2: `discover_triage.yaml` classifies-first; `faq-uc-search-before-commit`
  reconciled to `faq-uc-classify-first` (search<classify reward removed);
  aligned with tool-policy (NOT edited); count invariant (3/18) preserved.
  GATING_RACE → see §1 (PENDING 3-pass aggregate).
- [~] A3: `resolve_faq_grounded_answer.yaml` grounding_instruction + projection
  echo discourage re-search after `faq_miss=false`; soft-signal-first (NO hard
  cap added). **PARAPHRASE_STORM ≤3 target NOT met** (16 / 16 / 7); soft signal
  fires + reaches the LLM but is ignored → **STOP-and-surface OQ-S68.3** (per
  contract: surface before adding an A3-cap). A2 + OQ-S66.1 stand; sub-sprint
  not paused (ship-safe-part-and-surface).
- [x] Negative controls: distinct RESOLVE searches not suppressed;
  `faq_miss=true` re-search allowed; DISCOVER clarify-then-classify not broken
  (§3 + `Sprint068ClassifyFirstParaphraseTest`).
- [x] OQ-S66.1: discover_triage + resolve_faq `max_tool_steps` goldens
  reconciled to shipped values; Java `Failures 10 → 1`; no red tests left
  beyond the inherited tiebreaker.
- [x] eval_interactive `495/8` (delta attributed, §5) + autoloop `276` +
  17-fixture `31` + scoring SHA `35305bd8…` preserved.
- [x] No edits to PhaseEvaluator resolver logic / AgentRunLoopImpl / ToolEvent /
  system_prompt / case_specs / user_simulator / loop.py / scoring / applier.py
  / sandbox / meta_agent; A2/A3 manual; no Tier-0; no semantic hardcode (no A3
  hard-cap).
- [x] No `git add -A`; autoloop run on a clean committed tree; backend
  restarted for measurement; local-Mac only.
- [x] Handoff §0–§7 filled.
