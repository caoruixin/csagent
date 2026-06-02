---
title: Sprint 071 / S-Auto-15 dev handoff — cross-turn search_knowledge suppression gate + eval pytest baseline housekeeping
doc_tier: sprint-archive
status: current
implementation_status: implemented
source_of_truth: server/src/main/java/com/gumtree/csagent/service/runtime/AgentRunLoopImpl.java + eval_interactive/tests
last_reviewed: 2026-06-02
review_cadence: ad hoc
supersedes: []
superseded_by: null
notes: >
  S-Auto-15 (M-Auto-3 sub-sprint 6 of 6). Workstream A shipped a NEW,
  SEPARATE BotSession-scoped cross-turn search_knowledge suppression gate
  (V16 + persisted columns), fail-open + drift-aware + budget-1, ALONGSIDE
  the byte-untouched A1 cache / A3 within-turn gate / B1 resolver. The
  numeric §11 cross-turn PARAPHRASE_STORM ≤3 / total ≤3 target is NOT met
  (22 by the detector) and is STOP-and-surfaced as OQ-S71.1: ~19/22 are the
  budget-1 FIRST cross-turn refinements GUARDRAIL 0 forbids suppressing.
  Zero-false-suppression HARD bar MET (all 9 suppressions audited clean).
  Workstream B made eval_interactive pytest green (12 failed -> 503 passed
  under uv run).
---

# Sprint 071 / S-Auto-15 dev handoff

## §0 Summary

Two workstreams, four commits, all on `auto-loop-branch` (not pushed; tree
clean+committed).

- **A (commits `7dedd01`, `37dbb35`)** — a NEW, SEPARATE, BotSession-scoped
  **cross-turn `search_knowledge` re-search suppression gate** under the
  narrow, fail-open, drift-aware, budget-1 invariant. New Flyway **V16**
  persists the standing state (the entity reloads from DB per turn). Runs
  ALONGSIDE — never modifying — the A1 per-run dedup cache (505-526), the A3
  within-turn gate (558-576), and the B1 `resolveMaxStepsReason`.
- **B (commit `65b54f7`)** — eval_interactive pytest baseline housekeeping:
  **12 failed -> 503 passed** under `uv run pytest`. All fixes
  test-infra/eval-governance; no agent failure masked.

**Final counts:**
- Java `mvn -pl server test`: **1213 / 1 / 0 / 2** (baseline `1202/1/0/2` +
  **11 new A tests**; the sole failure is the inherited
  `SystemPromptUserRequestedTiebreakerTest`, unchanged).
- eval_interactive `uv run pytest`: **503 passed, 0 failed** (was 12 failed /
  491 passed).
- autoloop `uv run --extra dev pytest -q`: **276 passed** (unchanged).
- 17-fixture `uv run --extra dev pytest -p no:cacheprovider -q tests/test_anti_hardcode_check.py`:
  **31 passed** (unchanged).
- Scoring SHA (`_compute_scoring_code_sha()`):
  `35305bd84402ac455b126be5bcf8b2cb3b3fa55e3399f2c02443ea74bd8704e8` (unchanged).
- V16 applied clean on the live `csagent` DB (Flyway: "now at version v16").

**Acceptance verdict:** A's safe-harden half SHIPPED (gate fires correctly on
the 2nd+ cross-turn refinement, zero false suppression, all other §11
detectors unchanged); the numeric **cross-turn ≤3 / total ≤3 target is NOT
met** (22) and is **STOP-and-surfaced as OQ-S71.1** — the residual is
dominated by budget-1 first refinements GUARDRAIL 0 forbids suppressing, so
≤3 is unreachable without violating the budget floor. B SHIPPED fully.

## §1 The cross-turn gate (narrow-invariant walk)

### Why a cross-turn gate (and why persisted)

S-Auto-13b shipped a within-turn backstop (`lastSearchKnowledgeViableHit`,
`AgentRunLoopImpl` per-run local declared line 212). It only sees ONE outer
turn because `SessionManager.processMessage` reloads the `BotSession` from the
DB every turn (`SessionManager.java:306` `sessionRepository.findById`), so
per-run locals AND `@Transient` fields do not carry across turns. The
cross-turn storm (the bot re-searching the same un-drifted intent across
consecutive turns) survives it. Workstream A persists the standing state so a
SEPARATE cross-turn gate can catch the storm beyond the first refinement.

### V16 + persisted columns (A1/A2)

`server/src/main/resources/db/migration/V16__add_cross_turn_faq_hit_state.sql`
(mirrors V13's `ALTER TABLE bot_sessions ADD COLUMN …` form; validated against
the live DB, applied clean):

- `cross_turn_faq_hit_use_case VARCHAR(64)` (nullable) — the `active_use_case`
  at which a standing viable hit was captured. Condition 1 compares it to the
  current UC. Null = no standing hit = gate disabled (fail-open).
- `cross_turn_faq_hit_payload jsonb` (nullable) — the serialized
  `search_knowledge` result payload of that standing hit, SERVED to the LLM on
  suppression (under `accumulated_tool_results`).
- `cross_turn_search_allowed_since_hit INTEGER NOT NULL DEFAULT 0` — the
  cardinality budget counter.

`BotSession.java` carries matching PERSISTED `@Column` fields (NOT
`@Transient`) + Lombok getters/setters.

### The narrow invariant (default = ALLOW; suppress only when ALL prove true)

1. `session.getActiveUseCase()` non-null AND EQUAL to the standing-hit UC
   (no UC change);
2. NO drift this turn — `driftType` null/"none" AND
   `activeUseCase == previousActiveUseCase`;
3. a standing viable-hit marker (UC + payload) exists for THAT exact UC;
4. **[budget FIXED at 1]** at least ONE cross-turn `search_knowledge` for this
   UC has ALREADY been allowed since the standing hit — the FIRST post-hit
   cross-turn re-search is NEVER suppressed; only the 2nd+ is eligible.

Keyed PURELY on the existing `activeUseCase` / `driftType` / `faq_miss`
signals + a cardinality budget — **NO query-content / keyword / regex /
similarity / embedding / per-UC matching.** ONLY `search_knowledge` (a
read-only retrieval tool) may ever enter the gate.

### Run-start read / dispatch-site gate / write-back (A3)

- **Run start** (`AgentRunLoopImpl.run`, after `mergePartialIntakeFromContext`):
  snapshot the persisted standing state (`standingHitUcSnapshot`,
  `standingHitPayloadSnapshot`, `standingBudgetSnapshot`). Compute
  `crossTurnGateConditionsHold` (conditions 1-3, turn-stable) from the
  snapshot + current signals. If `isDriftThisTurn(session)` → CLEAR the
  standing state (drift always disables + does not leak).
- **Dispatch site** (`6a-quater`, AFTER A1 `6a-bis` and the A3 within-turn
  `6a-ter`, both of which `continue` first): for a `search_knowledge` call
  with `crossTurnGateConditionsHold`:
  - budget snapshot ≥ 1 → SUPPRESS: serve the deserialized standing payload +
    annotate `cross_turn_paraphrase_suppressed:true` + the originating turn +
    `continue` (no dispatch, no step charged). Malformed payload → FAIL OPEN
    (fall through to a normal dispatch).
  - budget snapshot < 1 (first refinement) → ALLOW + spend the budget
    (`session.setCrossTurnSearchAllowedSinceHit(+1)`, once per turn via the
    `crossTurnRefinementCountedThisTurn` run-local).
- **Capture / reset** (`6c-ter`, after the byte-untouched A3 refresh `6c-bis`):
  on a fresh `search_knowledge` result, `faq_miss=false` →
  `captureCrossTurnStandingHit` (UC marker + serialized payload; budget reset
  to 0 ONLY on a genuinely NEW capture — a same-UC viable re-capture PRESERVES
  the spent budget, see commit `37dbb35`); non-viable / malformed / failure →
  `clearCrossTurnStandingHit`.
- **Resolution / escalation resets**: `record_outcome` dispatch and successful
  `request_handover` both `clearCrossTurnStandingHit`.
- **Write-back**: every mutation is applied to the `session` entity directly;
  `SessionManager.processMessage` saves it AFTER `run()` returns, so no
  per-return write-back is needed.

### A4 — drift availability CONFIRMED (not a STOP)

`ControlKernel.applyRerouteDecision` sets `previousActiveUseCase` /
`driftType` at `ControlKernel.java:765-766`; it is called at line 330, BEFORE
`agentRunLoop.run(...)` at line 356. So the `@Transient` drift slots ARE
reliably populated at gate time. `isDriftThisTurn` treats any non-null,
non-"none" `driftType` (incl. `SAME_ISSUE` / `SAME_UC_NEW_TASK` /
`SOFT_SHIFT` / `RISK_SHIFT` / `ESCALATE`) OR a concrete UC change
(`active != previous`, both non-null) as drift → gate disabled. This is MORE
conservative than the contract's minimum (which only required `driftType`
null/"none"), strictly in the fail-open direction.

### A5 — trace annotation parity

`ToolEvent` carries distinct `crossTurnParaphraseSuppressed` /
`crossTurnHitAtTurn` (separate from within-turn `paraphraseSuppressed` /
`faqHitAtStep` and from A1 `deduplicated` / `originalAtStep`). `ControlKernel`
flatten emits `cross_turn_paraphrase_suppressed` + `cross_turn_hit_at_turn` on
the persisted trace, mirroring the within-turn flatten. A given event carries
at most ONE of the three annotations.

### ALONGSIDE the untouched A1 / A3 / B1

GUARDRAIL 2 honored: the A1 `successfulDispatchCache` block (505-526), the A3
within-turn `lastSearchKnowledgeViableHit` gate (558-576) + refresh (626-640),
and the B1 `resolveMaxStepsReason` are **byte-untouched**. The cross-turn gate
is a separate dispatch-site block (`6a-quater`) + a separate capture block
(`6c-ter`) + separate helpers; the existing inline `"search_knowledge"`
literals in the A1/A3 blocks are unchanged.

## §2 Negative-control table + zero-false-suppression audit

### Unit-test negative controls (MANDATORY; all pass)

`AgentRunLoopCrossTurnParaphraseGateTest` (11 tests, all green):

| control | test | result |
|---|---|---|
| (a) UC-change → ALLOW | `ucChange_crossTurnSearch_isNotSuppressed` | ✅ |
| (b) drift → ALLOW (+ clears marker) | `driftThisTurn_crossTurnSearch_isNotSuppressed` | ✅ |
| (c) first refinement budget-1 → ALLOW | `firstCrossTurnRefinement_budget1_isNotSuppressed` | ✅ |
| (d) genuine-miss → ALLOW (+ clears marker) | `genuineMissReSearch_isNotSuppressed_andClearsMarker` | ✅ |
| (e) write tool → never gated | `writeTool_isNeverGated` | ✅ |
| (f) all-4-true → SUPPRESS | `allFourConditionsTrue_crossTurnReSearch_isSuppressed` | ✅ |
| (g) null standing state → ALLOW | `nullStandingState_isNotSuppressed` | ✅ |
| (g) malformed payload → FAIL OPEN | `malformedStandingPayload_failsOpen_andDispatches` | ✅ |
| within-turn A3 + A1 unchanged | `withinTurnA3AndA1_unchanged_byCrossTurnGate` | ✅ |
| capture → first-refinement-allowed | `endToEnd_captureThenFirstRefinementAllowed` | ✅ |
| viable-hit chain → 3rd turn suppressed | `viableHitChain_budgetAccumulates_thirdTurnSuppressed` | ✅ |

### 3-pass zero-false-suppression audit (the HARD acceptance bar — MET)

Every `cross_turn_paraphrase_suppressed` annotation across the 3 fixed-backend
passes (`20260602-145940/150545/151056`) was audited from the persisted trace.
**9 suppressions total; ZERO confirmed 误杀.** Each is a same-UC,
same-searchable-intent re-search where a prior viable hit already exists:

| run | case | suppressed query | prior viable-hit query (same UC, same intent) |
|---|---|---|---|
| p1 | cs095 (UC-D) | "how to check which email is linked to my account login" | "check which email is linked to my account login" |
| p2 | cs012 (UC-FP) | "kitten rehoming policy specific issue ad removed" | "kitten rehoming ad removed no email" |
| p2 | cs015 (UC-FP) | "wording guidelines photo requirements ad posting rules" | "ad removed wording photo policy violation what to change" |
| p3 | cs015 (UC-FP) ×6 | "For Sale category posting rules …" (storm) | "what is not allowed in ad title wording photo guidelines" |

None introduced a NEW searchable intent; each is a paraphrase/narrowing of a
question already answered by the standing viable hit. **The HARD
zero-false-suppression bar is MET.**

## §3 Cross-turn PARAPHRASE_STORM 3-pass + other §11 detectors

### Methodology

Same S-Auto-13b detector (`/tmp/analyze_paraphrase_storm.py`, extended in
`/tmp/analyze_paraphrase_storm_s071.py` to treat the NEW
`cross_turn_paraphrase_suppressed` annotation as "served from cache" — not a
real dispatch, not entering the prior-tracker, identical to how
`paraphrase_suppressed` and `deduplicated` are handled). Reads
`results/<run>/results.json` `case_results[].per_turn_trace[].tool_calls[]`.
Cross-turn = a real successful `search_knowledge` whose query differs from a
prior real successful `search_knowledge` (faq_miss=false) in a DIFFERENT bot
turn of the same session.

### Before / after

| | within-turn | cross-turn | total | XT-suppressed events |
|---|---|---|---|---|
| **PRE-FIX** (S-Auto-13b, `094229/094655/095148`) | 0 / 0 / 0 = **0** | 7 / 5 / 10 = **22** | **22** | n/a (gate didn't exist) |
| **POST-FIX** (S-Auto-15, `145940/150545/151056`) | 0 / 0 / 0 = **0** | 9 / 7 / 6 = **22** | 1 / 2 / 6 = **9** | (zero 误杀) |

within-turn **0** (target ≤3 MET); cross-turn **22** (target ≤3 **NOT met**);
total **22** (target ≤3 **NOT met**). See OQ-S71.1 below for why.

### Storm decomposition (the load-bearing finding)

Classifying each detector-counted cross-turn storm by refinement RANK per
same-UC chain:

| pass | rank-1 (budget-1, MANDATORY-allowed) | rank-2+ total | caught by gate | missed |
|---|---|---|---|---|
| p1 | 7 | 3 | 1 | 2 |
| p2 | 7 | 2 | 2 | 0 |
| p3 | 5 | 7 | 6 | 1 |
| **sum** | **19** | **12** | **9** | **3** |

- **19 of 22** detector "cross-turn storm" hits are the **FIRST cross-turn
  refinement** for a same-UC chain — which GUARDRAIL 0 (budget FIXED at 1)
  REQUIRES the gate to ALLOW. The detector cannot distinguish "first
  legitimate refinement" from "storm".
- Of the genuine rank-2+ (12), the gate caught **9** (zero 误杀) and missed
  **3**. The 3 "missed" are detector artifacts, not gate bugs: in cs014 / wmkb
  the rank-1 was a `faq_miss=true` search that CORRECTLY cleared the marker, so
  the same-turn rank-2 viable search is rightly allowed (a genuine miss
  warrants fresh searching); the 1 cs001-p3 case is a single budget-edge turn.

### Other §11 detectors — UNCHANGED

| detector | p1 / p2 / p3 | vs S-Auto-12/13/14 |
|---|---|---|
| within-turn PARAPHRASE_STORM | 0 / 0 / 0 | unchanged (A3 within-turn intact) |
| GATING_RACE (search rejected UC 'none') | 0 / 0 / 0 | unchanged |
| ESCALATION_MISSTAMP | 0 / 0 / 0 | unchanged (S-Auto-14 → 0 held) |
| IDENTICAL_RETRY (A1 dedup events) | 2 / 9 / 7 | A1 still catching byte-identical |
| CONTRACT_VIOLATIONS | 0 / 0 / 0 | unchanged |

## §4 Workstream B baseline reconciliation (under `uv run pytest`)

| | before | after |
|---|---|---|
| eval_interactive pytest | **12 failed, 491 passed** | **503 passed, 0 failed** |

TRUE baseline re-measured under `cd eval_interactive && uv run pytest`. Per
failure disposition (all test-infra / eval-governance; **no agent failure
masked**):

| failure(s) | category | fix |
|---|---|---|
| `TestRItemClosuresRecordedInActionBank` ×4 | action_bank-split | fixture now reads `action_bank.md` AND `action_bank_archive.md`; scans EVERY R-item occurrence for a closure marker (the 4 closure rows moved to the archive in the 2026-06-01 split, commit `0323457`; the ids also appear in a non-closure recommendation list in `action_bank.md`). |
| `test_corpus_lint.py` ×5 | corpus_lint-interpreter | `_venv_python()` resolves the linter subprocess interpreter from `VIRTUAL_ENV` (under `uv run pytest` here `sys.executable`/`sys.prefix` report conda → `ModuleNotFoundError`). |
| `test_session_create_timeout_constants_widened_to_120s` | timeout-constant | assertion `DEFAULT_READ_TIMEOUT_SECONDS == 60.0` → `== 90.0`, tracking the intentional OQ-S65.8 production widening in `agent_client.py`. |
| `test_v2_schema_loads_cleanly` | case_spec_overrides | registry count `15` → `17` (Sprint 21 wave a5/a6 L3 dispositions landed in `case_spec_overrides.yaml`, commit `5cbb373`; the per-entry assertions still pin the original reviewed entries). |
| `test_smoke_review_report_tracks_smoke_set_and_overrides` | case_spec_overrides | `qa-reports/smoke-case-review.md` doc-sync: cs_095 left smoke / cs_190 joined / cs_001 recommended-outcome aligned to its approved override (`faq_miss_threshold_exceeded`); summary counts updated. |
| `test_full_corpus_lints_clean_with_smoke_subset_flag` (revealed by the B3 fix) | corpus-lint scope | scoped from the WHOLE `case_specs/` tree to the four canonical golden buckets. It had been failing all along, masked by the conda interpreter bug; `case_specs/` has since grown intentionally-non-golden buckets (`bad_cases` §5.6, `anchor_outcome`, `case_families`, `probe`) the golden-policy linter (R1/R2/R9) legitimately flags. **Corpus NOT edited; golden-policy linter NOT relaxed.** |

The override YAML (approved L3 dispositions) and the golden-policy linter are
the sources of truth; every B fix aligned a stale test/doc to them, never the
reverse (§5.4 respected).

## §5 Baselines + §7-stanza self-walk + fence disposition

### Baselines

| baseline | value | status |
|---|---|---|
| Java `mvn -pl server test` | 1213 / 1 / 0 / 2 | 1202 + 11 new A tests; sole failure inherited `SystemPromptUserRequestedTiebreakerTest` |
| eval_interactive `uv run pytest` | 503 passed | was 12 failed / 491 passed |
| autoloop `uv run --extra dev pytest -q` | 276 passed | unchanged |
| 17-fixture `…tests/test_anti_hardcode_check.py` | 31 passed | unchanged |
| scoring SHA `_compute_scoring_code_sha()` | `35305bd8…704e8` | unchanged (4 SHA-locked scoring files untouched) |
| V16 on live DB | applied clean | "now at version v16" |

### §7 stanza self-walk

- **Target failure layer: `infra`** — a new BotSession-scoped, drift-aware,
  cross-turn `search_knowledge` re-search suppression gate. B is
  `test-infra`/eval-governance. No agent semantic decision changed; A is a
  structural cardinality/idempotency backstop on a read-only tool that YIELDS
  to the LLM-owned drift signal. ✅
- **Tier-0 invariant: none added.** Stays `infra`, fail-open, existing signals
  only. Not added to `runtime_freeze_and_risk_policy.md`. ✅
- **Semantic hardcode: none.** Keyed purely on `activeUseCase`/`driftType`/
  `faq_miss` + a cardinality budget; no keyword/regex/enum/per-UC/similarity.
  Fail-open + drift-reset keep the LLM owning the first search per UC, the
  first cross-turn refinement, and drift ownership. No new escalation_reason
  enum. ✅
- **Generalization coverage:** target = cross-turn PARAPHRASE_STORM bad_cases
  subset (measured 3-pass; ≤3 NOT met → OQ-S71.1) / neighbor = within-turn
  (stays 0) + same-UC multi-turn continuation (unit `viableHitChain`) /
  negative = (a)-(g) unit controls all pass + zero-误杀 audit clean / shadow =
  held-out (not read by dev). ✅ measured.

### Fence disposition

A1 `successfulDispatchCache` (505-526), A3 within-turn
`lastSearchKnowledgeViableHit` gate (558-576) + refresh (626-640), and B1
`resolveMaxStepsReason` are **byte-untouched**. `PhaseEvaluator.java`,
`skills/**`, `tool-policy.yaml`, `user_simulator.py`, the 4 SHA-locked scoring
files, `autoloop/loop.py` + `sandbox/**` + `meta_agent/**` + `cli.py` +
`preflight.py`, `docs/foundational/**`, `docs/current/**`,
`runtime_freeze_and_risk_policy.md`, `docs/teams/**`, prior archives — none
edited. No `git add -A` (staged explicitly). Not pushed. autoloop NOT run.
Local-Mac only. Tree clean+committed.

## §6 Open questions

- **OQ-S71.1 (STOP-and-surface; primary) — cross-turn PARAPHRASE_STORM ≤3 /
  total ≤3 is UNREACHABLE under GUARDRAIL 0 budget-1 + the bad_cases
  structure.** The detector counts 22 cross-turn storms; **19 of 22 are the
  budget-1 FIRST cross-turn refinement per same-UC chain**, which the binding
  invariant FORBIDS suppressing (lowering the budget below 1 re-introduces
  误杀 and is explicitly forbidden). The gate caught 9 of the 12 genuine
  rank-2+ refinements with ZERO false suppression; the 3 misses are detector
  artifacts (genuine-miss-then-viable within a turn correctly clears the
  marker). **The numeric ≤3 target cannot be met without either (a) lowering
  the budget below 1 (FORBIDDEN), (b) content/similarity matching to
  distinguish "first legitimate refinement" from "storm" (a semantic hardcode,
  out of scope → human_review_required / M-Auto-4), or (c) re-specifying the
  §11 detector to NOT count the mandatory-allowed first refinement.** Per the
  contract ("cross-turn stays > 3 after the narrow gate → STOP; deliver+human
  decide"), this is surfaced for the deliver+human M-Auto-3 close decision, NOT
  pushed through. Recommended option (c): the detector should exclude rank-1
  refinements (the budget-1 floor) from the "storm" count, since they are
  not a defect — under that definition the residual is the 3 detector-artifact
  misses, all benign.
- **OQ-S71.2 — the `active_use_case` trace column is null on every persisted
  bad_cases turn** (37/28/29 turns null across the 3 passes) even though the
  LIVE `bot_sessions.active_use_case` is populated (verified via direct DB
  query: UC-A/UC-C/UC-D/UC-FP present). This is the known observability-debt
  pattern (trace surfaces lag runtime). It did NOT affect the gate (which reads
  the live session, not the trace), but any detector keyed on the trace's
  per-turn UC would be blind. Out of scope; flagged for the M-Auto-3 close
  observability sweep.
- **OQ-S68.1 / OQ-S70.1 — RESOLVED.** The eval_interactive pytest
  Python-baseline drift (495/8 doc → 491/12 real, action_bank-split +
  corpus_lint-interpreter + timeout + case_spec_overrides) is closed:
  **503 passed, 0 failed** under `uv run pytest`, documented per-failure in §4.
- **OQ-S69.1 — addressed (partial).** The cross-turn paraphrase residual the
  within-turn A3 gate could not catch now HAS a deterministic backstop; the
  gate fires on the 2nd+ same-UC refinement with zero 误杀. The numeric ≤3
  residual is re-cast as OQ-S71.1 (detector counts the mandatory first
  refinement).

## §7 Self-check

- [x] Gate suppresses ONLY under all 4 conditions; fail-open on every
  missing/ambiguous one; keyed ONLY on `activeUseCase`+`driftType`+`faq_miss`+
  budget; no content/similarity; only `search_knowledge`.
- [x] Standing state persisted via V16 + BotSession columns (not `@Transient`);
  read at run start, written back + reset on UC-change/drift/genuine-miss/
  resolution/escalation/record_outcome.
- [x] `driftType`/`previousActiveUseCase` confirmed reliably populated at gate
  time (ControlKernel:765-766 before run:356) — A4 not a STOP.
- [x] A1 cache + A3 within-turn gate + B1 resolver byte-untouched; A is a
  SEPARATE gate.
- [x] `cross_turn_paraphrase_suppressed` annotation distinct from within-turn
  `paraphrase_suppressed` and A1 `deduplicated`.
- [x] A tests: suppress-case + all negative controls + within-turn/A1
  unchanged + viable-hit-chain accumulation.
- [x] B: TRUE baseline under `uv run pytest`; action_bank-split + corpus_lint
  interpreter + timeout + case_spec_overrides fixed; documented green; no agent
  failure masked.
- [~] 3-pass bad_cases: cross-turn 22 (NOT ≤3 — **STOP-and-surfaced as
  OQ-S71.1**) + total 22 + within-turn 0; negative controls (a)-(g) pass;
  zero 误杀; IDENTICAL_RETRY/GATING_RACE/ESCALATION_MISSTAMP unchanged
  (backend restarted; persisted traces).
- [x] Java no new failures beyond `1202/1/0/2` (+11 new A tests); V16 applies
  clean; autoloop `276`; 17-fixture `31`; scoring SHA `35305bd8…`.
- [x] No `git add -A`; tree clean+committed; autoloop NOT run; backend
  restarted before measurement; local-Mac.
- [x] B2/B3/C-fences honored; no new escalation_reason enum; no Tier-0
  self-invented; no hard-fenced surface edited.

## M-Auto-3 close-readiness

| §11 gate | status |
|---|---|
| IDENTICAL_RETRY | ✅ A1 dedup intact (2/9/7) |
| PARAPHRASE_STORM within-turn | ✅ 0 / 0 / 0 |
| PARAPHRASE_STORM cross-turn | ⚠️ **22 — numeric ≤3 NOT met; STOP-and-surfaced as OQ-S71.1.** Gate ships safely (zero 误杀, 9 caught); ≤3 unreachable under GUARDRAIL 0 budget-1 (19/22 are mandatory-allowed first refinements). **Deliver+human close decision required.** |
| PARAPHRASE_STORM total | ⚠️ 22 (same residual as cross-turn) |
| GATING_RACE | ✅ 0 / 0 / 0 |
| ESCALATION_MISSTAMP | ✅ 0 / 0 / 0 |
| CONTRACT_VIOL_TURN0 | ✅ 0 contract violations across all 3 passes |

- **OQ-S68.1 / S70.1: RESOLVED** (eval pytest 503 passed under `uv run`).
- **OQ-S69.1: addressed** (cross-turn deterministic backstop shipped; numeric
  residual re-cast as OQ-S71.1).
- **NEW: OQ-S71.1** (cross-turn ≤3 unreachable under budget-1) + **OQ-S71.2**
  (trace `active_use_case` null observability debt) for the close.
- **Codex dispatch status: PENDING** — workstream A is a semantic-surface
  change (a runtime suppression gate on a read-only tool); per-sub-sprint Codex
  is FOLDED INTO the M-Auto-3 milestone-shared close Codex, which the
  deliver-agent authors at close. Dev notes status = PENDING.

## Commit SHAs (this sub-sprint, on `auto-loop-branch`, not pushed)

- `65b54f7` — B: eval_interactive pytest baseline housekeeping (12 → 503).
- `7dedd01` — A: V16 + entity + cross-turn gate + ToolEvent/ControlKernel
  annotation + 10 tests.
- `37dbb35` — A: preserve cross-turn budget on same-UC viable re-capture +
  viable-hit-chain test (the fix that made the gate actually fire).
