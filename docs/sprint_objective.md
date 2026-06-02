---
title: Sprint 071 / S-Auto-15 — cross-turn paraphrase backstop (BotSession-scoped, narrow drift-aware invariant) + eval pytest baseline housekeeping (M-Auto-3 §8.5-split sub-sprint 6 of 6)
doc_tier: current-runtime
status: current
implementation_status: not_started
source_of_truth: this file
last_reviewed: 2026-06-02
review_cadence: per sub-sprint
supersedes: [docs/sprints/sprint-070-objective.md]
superseded_by: null
notes: >
  M-Auto-3 / Sprint 071 / S-Auto-15. SIXTH sub-sprint, added by a **§8.5 split
  decision (deliver-agent + human, 2026-06-02)** beyond the §8.1 5-sub-sprint
  ceiling to COMPLETE M-Auto-3's own acceptance bar — it closes the §11
  PARAPHRASE_STORM cross-turn residual (OQ-S69.1, the S-Auto-13b carry) and the
  close-gate eval-baseline housekeeping (OQ-S68.1 / OQ-S70.1). It is NOT a new
  milestone; it is the last work before the M-Auto-3 close + the first overnight
  launch on the cleaned substrate. Human directive 2026-06-02: batch the known
  residuals (no one-at-a-time cadence), do NOT pick up B2/B3/C (M-Auto-4), and on
  completion go straight to close + overnight without per-OQ re-confirmation.

  **Two workstreams in one batched sub-sprint:**

  - **Workstream A — cross-turn paraphrase backstop (`infra`; §7 REQUIRED).**
    S-Auto-13b's `faq_miss`-state gate (`AgentRunLoopImpl` lines 558-576 / reset
    626-640) is a per-`run()` (within-turn) local (`lastSearchKnowledgeViableHit`,
    line 193) — it cannot see across turns because `SessionManager.processMessage`
    reloads `BotSession` from the DB every turn (line 306), so `@Transient` state
    does not survive. A is a SEPARATE, ADDITIONAL gate that persists a standing
    viable-hit marker on `BotSession` (new Flyway migration V16; the entity reloads
    per turn) and suppresses a CROSS-turn `search_knowledge` re-search under a
    DELIBERATELY NARROW, fail-open, drift-aware invariant (below). Target: cross-turn
    `PARAPHRASE_STORM` 21→≤3 and total (within+cross) ≤3, with the within-turn gate
    + A1 + B1 untouched.

  - **Workstream B — eval pytest baseline housekeeping (`test-infra`/eval-governance;
    §7 EXEMPT).** Re-measure eval_interactive pytest under the correct interpreter
    (`uv run`), separate the conda-python env artifact from real failures, then fix
    the genuinely-broken tests so the M-Auto-3 close "Python baselines" gate is
    unambiguous. Disjoint from the agent runtime; must NOT be used to mask any agent
    failure (§5.4 spirit).

  **THE NARROW CROSS-TURN INVARIANT (human-mandated; written to avoid semantic
  误杀 / over-suppression of legitimate searches):** the cross-turn gate suppresses
  a `search_knowledge` dispatch ONLY when the runtime has unambiguous EXISTING-signal
  evidence that the search is a redundant repeat of an already-satisfied retrieval
  for the SAME, UN-DRIFTED use case, and it FAILS OPEN (never suppresses) on any
  ambiguity. ALL of the following must hold to suppress:
    1. `session.getActiveUseCase()` is non-null AND EQUAL to the UC recorded when the
       standing viable hit was captured (no UC change);
    2. NO drift is signalled this turn (`driftType` null/"none" AND
       `activeUseCase == previousActiveUseCase`);
    3. a standing viable-hit marker exists for THAT exact UC (persisted on BotSession;
       set only on a `search_knowledge` result with `faq_miss=false`);
    4. [budget] at least ONE cross-turn `search_knowledge` for this UC has ALREADY been
       allowed since the standing hit — so the FIRST post-hit cross-turn re-search (the
       legitimate refinement) is NEVER suppressed; only the storm beyond it is.
  The marker is RESET (gate disabled) immediately on ANY of: UC change, any drift
  signal, a `faq_miss=true` (genuine miss), a FINAL_ANSWER/resolution, an
  escalation/handover, or `record_outcome`. Keyed PURELY on the EXISTING
  `activeUseCase` / `driftType` / `faq_miss` signals + a cardinality budget — NO
  query-content / keyword / regex / semantic-similarity / per-UC matching. If the gate
  cannot be made safe without content matching → STOP-and-surface (it is then out of
  scope as a semantic hardcode, not a structural backstop).

  **§4 / §1.7 discipline (inherited; do NOT violate):** cross-turn is a structural
  cardinality/idempotency backstop on a READ-ONLY retrieval tool (§1.4 budget), NOT a
  semantic decision — the LLM still owns the FIRST search per UC, the first cross-turn
  refinement (budget-1), and drift/topic ownership (the gate YIELDS to the LLM-owned
  drift signal). Soft-signal-first is satisfied by the S-Auto-13 within-turn
  falsification (SAME model `deepseek-v4-flash`, SAME paraphrase shape) — do NOT
  re-run a multi-pass soft-only round.

  **Inherited substrate at HEAD (do NOT re-fix / revert):** S-Auto-11 (`loop.py`) +
  S-Auto-12 (A1 hybrid dedup) + S-Auto-13 (A2 classify-first; A3 soft layer) +
  S-Auto-13b (A3 within-turn `faq_miss`-state backstop) + S-Auto-14 (B1 evidence-aware
  `resolveMaxStepsReason`) are FINALIZED. `b351648` determinism config in place.
  fence-#13 scoring SHA `35305bd8…`.

  **CORRECTED baselines (post-S-Auto-14, deliver-verified):** Java
  **`1202 / 1 / 0 / 2`** (sole failure = inherited `SystemPromptUserRequestedTiebreakerTest`,
  OQ-S41.5); eval_interactive pytest **`491 / 12` in the conda-python env vs `495 / 8`
  documented** — workstream B establishes the TRUE baseline under `uv run` and fixes the
  real failures; autoloop `276`; 17-fixture `31`; scoring SHA `35305bd8…`.

  Dev session source-of-truth: `compact/sprint-071-dev-prompt.md` (self-contained per §9).
---

# Sprint 071 / S-Auto-15 — cross-turn paraphrase backstop + eval pytest baseline housekeeping

## Class

- **Layer (primary)**: workstream A = `infra` (a NEW BotSession-scoped, drift-aware,
  cross-turn `search_knowledge` re-search suppression gate, alongside — NOT modifying —
  the S-Auto-13b within-turn gate and the S-Auto-12 A1 cache). Workstream B =
  `test-infra` / eval-governance housekeeping (no agent-runtime change).
- **§7 stanza**: **REQUIRED** (workstream A is a semantic-adjacent runtime gate). Self-walked
  below. Workstream B alone would be EXEMPT, but the sub-sprint as a whole carries the stanza
  because of A.
- **Codex review plan (§4.3)**: **PER-SUB-SPRINT REQUIRED for A** (a new runtime suppression
  gate on a semantic-adjacent surface), **FOLDED INTO the M-Auto-3 milestone-shared close
  Codex** (precedent: M-Auto-1B per-sub-sprint+milestone consolidation). The deliver-agent
  authors ONE consolidated close prompt `compact/M-Auto-3-review-prompt.md` covering: S-Auto-14
  (B1) per-sub-sprint + S-Auto-15 (A) per-sub-sprint + the cumulative S-Auto-11..15 range + the
  bundled M-Auto-2 residual. No standalone mid-sub-sprint Codex round unless a §1.7 red line is
  crossed.
- **Position in milestone**: 6th of 6, added by a **§8.5 split decision** (deliver+human
  2026-06-02) to complete M-Auto-3's own §11 PARAPHRASE_STORM cross-turn bar + the close-gate
  baseline. Sequence: S-Auto-11 ✅ → S-Auto-12 ✅ → S-Auto-13 ✅-partial → S-Auto-13b ✅ →
  S-Auto-14 ✅ → **S-Auto-15 (A+B)** → M-Auto-3 close + first overnight launch. Any further
  second-order issue after S-Auto-15 → M-Auto-4, NOT another M-Auto-3 sub-sprint.

## Goal

**A:** Extend paraphrase-storm suppression across turns so the autoloop fitness signal stops
carrying the cross-turn step-waster, WITHOUT ever suppressing a legitimate new-topic or
refinement search. The gate is BotSession-scoped (persisted; the entity reloads per turn) and
fires only under the narrow, fail-open, drift-aware invariant in the frontmatter.

**B:** Make the eval_interactive pytest baseline trustworthy and green under the correct
interpreter so the M-Auto-3 close "Python baselines" hard gate is unambiguous.

**Acceptance (A):** on a 3-pass `bad_cases` rerun (sim_temp=0 + bot_temp=0 + 60s deadline +
parallel=1, freshly-restarted backend, measured via S-Auto-11 per-iter trace persistence),
cross-turn `PARAPHRASE_STORM` drops **21 → ≤3** AND total (within+cross) **≤3**, with the
within-turn count still **0** and NEGATIVE CONTROLS intact: (a) a cross-turn search after a UC
change is NOT suppressed; (b) a cross-turn search after a drift signal is NOT suppressed; (c) the
first cross-turn refinement search after a standing hit is NOT suppressed (budget-1); (d) a
re-search after a genuine miss (`faq_miss=true`) is NOT suppressed; (e) write/side-effect tools
are NEVER gated; (f) IDENTICAL_RETRY / GATING_RACE / ESCALATION_MISSTAMP detectors unchanged from
their S-Auto-12/13/14 levels.

**Acceptance (B):** eval_interactive pytest reaches a documented green baseline under
`uv run pytest`; every fix is test-infra/governance only and is shown NOT to mask any
agent-attributable failure.

**Baselines preserved:** Java `1202/1/0/2` + new A tests (Failures stays 1); autoloop `276`;
17-fixture `31`; scoring SHA `35305bd8…` (the 4 SHA-locked scoring files untouched).

## Scope

### Workstream A — cross-turn paraphrase backstop (steps A1–A6)

A1. **Flyway migration V16** (`server/src/main/resources/db/migration/V16__add_cross_turn_faq_hit_state.sql`).
   Add the cross-turn standing-hit state to `bot_sessions`, mirroring the V12/V13 counter-column
   precedent (`ALTER TABLE bot_sessions ADD COLUMN …`). Recommended (dev may refine the exact
   representation as long as the invariant is expressible): a UC marker (varchar, nullable), the
   standing viable-hit payload to SERVE on suppression (jsonb, nullable — mirror the existing
   `*_context jsonb` columns), and a budget counter (`INTEGER NOT NULL DEFAULT 0`). Next migration
   id is **V16** (V15 already exists: `V15__create_bot_turn_llm_calls.sql`).

A2. **BotSession entity** (`server/src/main/java/com/gumtree/csagent/model/BotSession.java`): add the
   matching `@Column` field(s) + getters/setters (Lombok). These are PERSISTED columns, NOT
   `@Transient` — `processMessage` reloads the entity each turn.

A3. **The cross-turn gate** (`AgentRunLoopImpl.run`). At run start, READ the standing cross-turn state
   from `session`. Add a NEW gate in the dispatch path for `search_knowledge` that applies the narrow
   invariant (frontmatter, all 4 AND-conditions + fail-open). On suppression: serve the standing
   viable-hit payload (so the loop proceeds to draft/escalate, mirroring the within-turn serve-and-
   continue), trace-annotate `cross_turn_paraphrase_suppressed:true` + the originating turn, do NOT
   re-dispatch / charge a step. The gate runs ALONGSIDE — never instead of — the S-Auto-12 A1 cache
   (lines 505-526) and the S-Auto-13b within-turn gate (lines 558-576); those are byte-untouched. At
   run end (or on the relevant events), WRITE BACK the updated standing state / budget / resets to
   `session` so the next turn sees it. RESET rules per the invariant (UC change, drift, genuine miss,
   resolution, escalation, record_outcome).

A4. **Drift-signal availability**: verify `driftType` / `previousActiveUseCase` are reliably populated
   on `session` at gate time (they are `@Transient`, set during this turn's processing). If they are
   NOT reliably set before the dispatch loop runs → STOP-and-surface (do NOT guess / do NOT fall back
   to a content heuristic). The gate MUST be able to read a trustworthy drift signal for condition (2).

A5. **Trace annotation** parity: extend the `ToolEvent` annotation + the `ControlKernel` flatten path the
   way S-Auto-13b did for `paraphrase_suppressed` (a distinct `cross_turn_paraphrase_suppressed` marker
   so the detector + admin trace can tell within-turn from cross-turn suppression apart).

A6. **Java tests** (`server/src/test/**`): characterize the narrow invariant directly — suppress only
   when all 4 conditions hold; fail-open on each missing condition; UC-change reset; drift reset;
   genuine-miss reset; budget-1 (first refinement allowed, second suppressed); write-tool never gated;
   resolution/escalation/record_outcome reset; null/malformed standing-state guard. Plus a test that the
   within-turn A3 gate and the A1 cache behaviour are unchanged.

### Workstream B — eval pytest baseline housekeeping (steps B1–B5)

B1. **Re-measure under the correct interpreter** (`feedback_remeasure_baselines_prompts_stale` +
   `reference_eval_pytest_corpus_lint_conda_python`): run `cd eval_interactive && uv run pytest` and
   record the TRUE pass/fail. Decompose every failure into: action_bank-split / corpus_lint-interpreter /
   case_spec_overrides / timeout-constant / OTHER. Any failure that is agent-attributable (not env /
   housekeeping) → STOP-and-surface (do NOT absorb it into housekeeping).

B2. **action_bank-split tests** (`TestRItemClosuresRecordedInActionBank`, the OQ-S68.1 set): repoint the
   assertions at the relocated rows in `docs/action_bank_archive.md` (the `0323457` split moved closed
   rows there) so the governance test reads the right source. Do NOT revert the action_bank split.

B3. **corpus_lint subprocess interpreter** (`eval_interactive/tests/regression/test_corpus_lint.py:36`,
   `sys.executable`): make the subprocess resolve the project venv interpreter robustly (so it does not
   inherit a conda `python` lacking the `eval_interactive` package) OR document the required `uv run`
   invocation as the baseline contract. Choose the lower-risk fix; the goal is a reproducible green.

B4. **timeout constant + case_spec_overrides**: reconcile
   `test_agent_client_session_create_timeout.py::test_session_create_timeout_constants_widened_to_120s`
   (`assert DEFAULT_READ_TIMEOUT_SECONDS == 60.0`) against the actual constant, and the 2
   `test_case_spec_overrides` failures — each is a test-side constant/reference drift; fix the test or the
   constant so they agree, narrated in the handoff (an owned reconciliation, not regression-masking).

B5. **Confirm + document** the green baseline under `uv run pytest`; record the exact before/after counts
   and the per-failure disposition in the handoff §5.

### Shared measurement (step C, end)

C. **Restart `:8080`** (`mvn spring-boot:run` has no hot-reload), then run a 3-pass `bad_cases` measurement
   (`cd eval_interactive && uv run eval-interactive run --path case_specs/bad_cases/ --parallel 1`, ×3 at
   sim/bot temp=0 / 60s). Read the S-Auto-11-persisted per-iter traces and report ALL §11 detectors:
   cross-turn `PARAPHRASE_STORM` 21→≤3 + total ≤3 + within-turn still 0; IDENTICAL_RETRY / GATING_RACE /
   ESCALATION_MISSTAMP unchanged; the negative controls (a)-(f) from Acceptance(A). This run + traces is
   the §5.6 / §11 close evidence for the deliver+human M-Auto-3 close.

## Hard fences / STOP conditions

**GUARDRAIL 0 — ANTI-误杀 BOUNDARY (HARD; ZERO false suppression). This is the binding boundary of workstream A; every other rule serves it.**

- **Default = ALLOW.** Implement the gate as "ALLOW the search UNLESS all 4 invariant conditions PROVE true." NEVER "suppress unless proven safe." Any null / missing / unreadable signal (`activeUseCase`, `driftType`/`previousActiveUseCase`, `faq_miss`, the standing marker) → ALLOW. There is NO code path that suppresses on incomplete evidence.
- **Drift always wins.** `driftType` non-null & non-"none", OR `activeUseCase != previousActiveUseCase` → gate DISABLED for that turn. No exception.
- **Budget is FIXED at 1.** The FIRST cross-turn `search_knowledge` for a UC after a standing hit is ALWAYS allowed (the legitimate refinement); only the 2nd+ same-UC cross-turn re-search is eligible for suppression. Lowering the budget below 1 (suppressing the first cross-turn re-search) is FORBIDDEN — that is within-turn-level aggression applied cross-turn and re-introduces 误杀. If cross-turn won't reach ≤3 at budget 1 → STOP-and-surface; do NOT lower the budget.
- **Read-only only.** ONLY `search_knowledge` may ever enter the gate; write/side-effect tools are structurally excluded.
- **Zero-false-suppression is a HARD acceptance bar (not merely a STOP).** In the 3-pass measurement EVERY `cross_turn_paraphrase_suppressed` annotation is manually audited in handoff §2 (mirror S-Auto-13b handoff §3(g)): each MUST be a same-UC, no-new-searchable-intent re-search. ONE confirmed false suppression — a turn where the user introduced new searchable intent and the gate suppressed the search — FAILS the sub-sprint; revert/redesign the gate, do NOT ship it.
- **Negative-control unit tests are MANDATORY (Java), not illustrative.** (a) UC-change → ALLOW; (b) drift → ALLOW; (c) first cross-turn refinement (budget 1) → ALLOW; (d) genuine-miss (`faq_miss=true`) → ALLOW; (e) write/side-effect tool → NEVER gated; (f) all-4-true → SUPPRESS; (g) null/malformed standing state → ALLOW. All must pass; absence of any of (a)-(g) = incomplete.

**GUARDRAIL 1 — THE NARROW INVARIANT IS THE CONTRACT.** The cross-turn gate is EXACTLY the 4-condition,
fail-open, drift-aware suppression in the frontmatter. It may NOT be widened. In particular: keyed ONLY on
`activeUseCase` + `driftType`/`previousActiveUseCase` + `faq_miss` + a cardinality budget; NO
query-content / keyword / regex / semantic-similarity / embedding / per-UC matching; ALWAYS fail-open
(when any condition is null/unverifiable → ALLOW the search). The gate applies ONLY to `search_knowledge`
(read-only retrieval) — NEVER to any write/side-effect tool.

**GUARDRAIL 2 — A IS A SEPARATE GATE, NOT AN EDIT TO EXISTING GATES.** The S-Auto-12 A1
`successfulDispatchCache` (lines 162-171, 505-526, 601-608), the S-Auto-13b within-turn
`lastSearchKnowledgeViableHit` gate (lines 172-193, 558-576, 610-640), and the S-Auto-14 B1
`resolveMaxStepsReason` are FINALIZED and byte-untouched. A adds a new, additional cross-turn gate +
persisted state; it does not modify the within-turn/per-run paths.

**In scope to edit**: `server/src/main/resources/db/migration/V16__*.sql` (new); `BotSession.java` (new
columns only); `AgentRunLoopImpl.java` (the NEW cross-turn gate + run-start read + write-back only —
do NOT touch the A1/A3 within-turn blocks); `ToolEvent.java` + `ControlKernel.java` (the
`cross_turn_paraphrase_suppressed` annotation/flatten, mirroring S-Auto-13b's pattern); `server/src/test/**`
(new A tests); and the SPECIFIC eval_interactive test files in workstream B
(`tests/regression/test_corpus_lint.py`, `tests/test_agent_client_session_create_timeout.py`, the
`test_case_spec_overrides` test, and the `TestRItemClosuresRecordedInActionBank` test). Read-only:
`SessionManager.java` (the per-turn reload + drift population), `KnowledgeSearchResult.java` (faq_miss
source), the bad_cases suite (measurement).

**Hard-fenced (do NOT edit)**: the A1 / A3 within-turn blocks in `AgentRunLoopImpl`; `PhaseEvaluator.java`
(B1 FINALIZED); `skills/**`; `tool-policy.yaml`; `user_simulator.py`; the 4 SHA-locked scoring files;
`autoloop/autoloop/loop.py` + `sandbox/**` + `meta_agent/**` + `cli.py` + `preflight.py`;
`docs/foundational/**`, `docs/current/**`, `docs/runtime_freeze_and_risk_policy.md`, `docs/teams/**`; prior
sprint/milestone archives. `b351648` not reverted. Do NOT add a new `escalation_reason` enum value; do NOT
add a new Tier-0 invariant; do NOT widen the autoloop mutable surface; do NOT pick up B2/B3/C (M-Auto-4).

**STOP-and-surface conditions**:
- The cross-turn gate would suppress a legitimate new-topic or refinement search (误杀) in ANY observed
  trace → STOP (the invariant is meant to prevent this; if it does not, the design is wrong).
- `driftType` / `previousActiveUseCase` is NOT reliably populated at gate time → STOP (do NOT substitute a
  content heuristic).
- The gate cannot be made to hit cross-turn ≤3 WITHOUT content/keyword/similarity matching → STOP (it is
  then a semantic hardcode, out of scope; route to `human_review_required` / M-Auto-4, do NOT ship it).
- Cross-turn `PARAPHRASE_STORM` stays > 3 after the narrow gate → STOP (deliver+human decide: tune the
  budget, accept residual, or defer to M-Auto-4).
- Workstream B re-measure reveals a REAL agent-attributable pytest failure (not env/housekeeping) → STOP
  (do NOT mask it as housekeeping; §5.4 spirit).
- Landing A appears to require modifying the A1/A3 within-turn gates or B1 → STOP.
- Any other hard-fenced surface needs editing → STOP-and-surface.
- **No `git add -A`** — stage explicitly. Any `autoloop run` only on a clean committed tree
  (`project_autoloop_dirty_index_hazard`). **Local-Mac only.** Restart the backend after server changes
  before the measurement.

## Test / eval requirements

- **Java**: no NEW failures beyond `1202 / 1 / 0 / 2`; `Tests run` rises by the new A tests; `Failures`
  stays 1 (inherited tiebreaker). Migration V16 applies cleanly on a fresh DB.
- **eval_interactive pytest**: documented GREEN baseline under `uv run pytest` (workstream B); report exact
  before/after + per-failure disposition; confirm no agent-attributable failure was masked.
- **autoloop pytest**: `276 passed`. **17-fixture detector sweep**: `31 passed`. **scoring SHA**:
  `35305bd8…` unchanged.
- **A measurement (deliverable)**: restart `:8080`, 3-pass `bad_cases`; cross-turn `PARAPHRASE_STORM`
  21→≤3 + total ≤3 + within-turn 0; negative controls (a)-(f); IDENTICAL_RETRY / GATING_RACE /
  ESCALATION_MISSTAMP unchanged.

## §7 stanza (REQUIRED)

**Target failure layer:** `infra` — a NEW BotSession-scoped, drift-aware, cross-turn `search_knowledge`
re-search suppression gate (workstream A). Workstream B is `test-infra`/eval-governance housekeeping (no
agent-runtime change). No agent semantic *decision* (UC hypothesis, drift detection, escalation posture,
response strategy) is changed; A is a structural cardinality/idempotency backstop on a read-only retrieval
tool that YIELDS to the LLM-owned drift signal.

**Tier-0 invariant:** This sub-sprint adds NO Tier-0 invariant. The gate stays `infra`, reuses existing
signals, and is fail-open. Not added to `docs/runtime_freeze_and_risk_policy.md` §1/§2. If review argues
the cross-turn suppression should be a Tier-0 invariant → `human_review_required` (do NOT self-invent).

**Semantic hardcode:** None introduced. The gate keys PURELY on the EXISTING `activeUseCase` / `driftType`
/ `faq_miss` signals + a cardinality budget (§1.4 budget) — no keyword/regex/enum/per-UC/similarity matching.
Fail-open + drift-reset keep the LLM owning the first search per UC, the first cross-turn refinement, and
topic/drift ownership. Soft-signal-first is satisfied by the S-Auto-13 within-turn falsification (same model,
same paraphrase shape; do NOT repeat the soft round). No new `escalation_reason` enum value.

**Generalization coverage:** target = the cross-turn `PARAPHRASE_STORM` bad_cases subset (21→≤3, 3-pass) /
neighbor = within-turn paraphrase cases (must STAY 0) + same-UC multi-turn continuation cases / negative =
(a) UC-change cross-turn search NOT suppressed, (b) drift-signal cross-turn search NOT suppressed, (c) first
cross-turn refinement NOT suppressed (budget-1), (d) genuine-miss re-search NOT suppressed, (e) write tools
never gated, (f) IDENTICAL_RETRY/GATING_RACE/ESCALATION_MISSTAMP detectors unchanged / shadow = held-out
(not read by dev). Counts confirmed at handoff via the 3-pass rerun + the negative-control table.

## Codex review plan (per §4.3)

**PER-SUB-SPRINT REQUIRED for A**, FOLDED INTO the M-Auto-3 milestone-shared close Codex (consolidation
precedent: M-Auto-1B). The deliver-agent authors ONE consolidated `compact/M-Auto-3-review-prompt.md`
embedding the §4.1 nine-question kernel and covering: S-Auto-14 (B1) + S-Auto-15 (A) per-sub-sprint
verification + the cumulative S-Auto-11..15 range + the bundled M-Auto-2 residual (fence-#13 OQ-S65.5/6
`eval_runner.py`+`tier_evaluator.py` + `b351648` server-Java determinism). Verification focus for A: the gate
keys ONLY on existing signals + a cardinality budget (no content/keyword/similarity matching); fail-open and
drift-reset are present; the within-turn A3 gate + A1 cache + B1 resolver are byte-untouched; migration V16 is
additive; B housekeeping masks no agent failure. Verdict → `docs/codex-findings.md` (§4.2 header). §5.6
bad-case evidence is recorded BEFORE Codex dispatch (`feedback_milestone_close_bad_case_before_codex`).

## Handoff requirements

`docs/sprints/sprint-071-handoff.md` at close. Mandatory: §0 summary (both workstreams, commits, final counts
— Java `1202/1`+new A tests, eval pytest green baseline + per-failure disposition); §1 the cross-turn gate
(the narrow invariant walk; persisted columns + V16; run-start read / write-back; reset rules; how it sits
ALONGSIDE the untouched A1/A3 within-turn paths); §2 the negative-control table (a)-(f) with trace evidence
proving zero 误杀; §3 cross-turn `PARAPHRASE_STORM` 3-pass before/after + total + within-turn still 0 +
IDENTICAL_RETRY/GATING_RACE/ESCALATION_MISSTAMP unchanged; §4 workstream B baseline reconciliation (before/
after under `uv run`, per-failure disposition, no agent failure masked); §5 baselines + §7-stanza self-walk +
fence disposition (explicit "A1/A3 within-turn gates + B1 byte-untouched"); §6 OQs surfaced; §7 self-check
tick-off. As the LAST M-Auto-3 sub-sprint, the handoff also states **M-Auto-3 close-readiness**: all §11
gates now met (IDENTICAL_RETRY ✅ / PARAPHRASE_STORM within ✅ + cross ✅ + total ✅ / GATING_RACE ✅ /
ESCALATION_MISSTAMP ✅ / CONTRACT_VIOL_TURN0 re-attributed) + OQ-S68.1/S70.1 resolved + the Codex dispatch
status.

## Commit discipline

Multi-commit acceptable (V16+entity / cross-turn gate+tests / workstream B / handoff). Commit message:
`Sprint 071 / S-Auto-15 / M-Auto-3 — <description>` + standard footer
`Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>`. No `git add -A` — stage explicitly.
Do not push. Leave the tree CLEAN + committed at the end (the deliver-agent launches the overnight on it).

## Self-check (dev MUST verify before claiming done)

- [ ] Cross-turn gate suppresses ONLY under all 4 invariant conditions; fail-open on every missing/ambiguous
  condition; keyed ONLY on `activeUseCase`+`driftType`+`faq_miss`+cardinality budget; NO content/keyword/
  similarity matching; applies ONLY to `search_knowledge`.
- [ ] Standing state persisted via V16 + BotSession columns (NOT `@Transient`); read at run start, written
  back at run end / reset events; resets on UC-change / drift / genuine-miss / resolution / escalation /
  record_outcome.
- [ ] `driftType`/`previousActiveUseCase` confirmed reliably populated at gate time (else STOPPED-and-surfaced).
- [ ] A1 `successfulDispatchCache` + A3 `lastSearchKnowledgeViableHit` within-turn gate + B1
  `resolveMaxStepsReason` are byte-untouched; A is a SEPARATE additional gate.
- [ ] `cross_turn_paraphrase_suppressed` trace annotation distinct from within-turn `paraphrase_suppressed`.
- [ ] A tests cover suppress-case + all negative controls (UC-change / drift / budget-1 first-refinement /
  genuine-miss / write-tool-never-gated / resolution-reset / null-guard) + within-turn/A1 unchanged.
- [ ] Workstream B: TRUE baseline established under `uv run pytest`; the 4 action_bank-split + corpus_lint
  interpreter + timeout constant + case_spec_overrides fixed; documented green; NO agent failure masked.
- [ ] 3-pass bad_cases: cross-turn `PARAPHRASE_STORM` 21→≤3 + total ≤3 + within-turn 0; negative controls
  (a)-(f); IDENTICAL_RETRY/GATING_RACE/ESCALATION_MISSTAMP unchanged (backend restarted; persisted traces).
- [ ] Java no NEW failures beyond `1202/1/0/2` (+ new A tests); V16 applies clean; autoloop `276`; 17-fixture
  `31`; scoring SHA `35305bd8…`.
- [ ] No `git add -A`; tree clean+committed at end; any autoloop run on a clean tree; backend restarted for
  measurement; local-Mac only.
- [ ] B2/B3/C NOT touched; no new escalation_reason enum; no Tier-0 self-invented; no hard-fenced surface edited.
- [ ] Handoff §0-§7 filled (incl. §2 negative-control table + M-Auto-3 close-readiness); Codex dispatch status noted.
