# Dev prompt — Sprint 071 / S-Auto-15 / M-Auto-3 (cross-turn paraphrase backstop + eval pytest baseline housekeeping)

## Role identity

你是 **dev agent for Sprint 071 / S-Auto-15** (M-Auto-3, sub-sprint 6 of 6, added by a §8.5 split decision —
the LAST work before the M-Auto-3 close + first overnight launch). One-line goal: **(A)** extend
paraphrase-storm suppression ACROSS turns under a deliberately narrow, fail-open, drift-aware invariant that
NEVER suppresses a legitimate search; **(B)** make the eval_interactive pytest baseline green+trustworthy
under the correct interpreter.

## Read order (minimal)

Read ONLY: `AGENTS.md` (auto-loaded) + this prompt. Everything you need is embedded below; code anchors are
cited with exact paths/lines — open them on demand, do not go exploring beyond them.

## Why this sub-sprint

S-Auto-13b shipped a within-turn paraphrase backstop in `AgentRunLoopImpl` (the `lastSearchKnowledgeViableHit`
gate, lines 558-576 / reset 626-640) keyed on the `faq_miss` result flag. It is a per-`run()` LOCAL (declared
line 193), so it only sees ONE outer turn. The cross-turn paraphrase storm (the bot re-searching the same
intent across consecutive turns) survives it: S-Auto-13b measured cross-turn `PARAPHRASE_STORM` 21→22 (total
33→22). It survives because `SessionManager.processMessage` reloads `BotSession` from the DB EVERY turn
(`SessionManager.java:306` `sessionRepository.findById`), so per-run locals AND `@Transient` fields do not
carry across turns. Workstream A persists a standing viable-hit marker on `BotSession` (new Flyway migration)
and adds a SEPARATE cross-turn gate under a narrow invariant. Workstream B clears the eval-baseline
housekeeping that blocks the M-Auto-3 close Python-baselines gate.

## THE NARROW CROSS-TURN INVARIANT (human-mandated — written to avoid semantic 误杀)

Suppress a `search_knowledge` dispatch ACROSS turns ONLY when the runtime has unambiguous EXISTING-signal
evidence that it is a redundant repeat of an already-satisfied retrieval for the SAME, UN-DRIFTED use case.
**FAIL OPEN** (never suppress) on any ambiguity. ALL must hold to suppress:

1. `session.getActiveUseCase()` is non-null AND EQUAL to the UC recorded when the standing viable hit was
   captured (no UC change);
2. NO drift this turn — `driftType` null/"none" AND `activeUseCase == previousActiveUseCase`;
3. a standing viable-hit marker exists for THAT exact UC (persisted on BotSession; set only on a
   `search_knowledge` result with `faq_miss=false`);
4. **[budget]** at least ONE cross-turn `search_knowledge` for this UC has ALREADY been allowed since the
   standing hit — so the FIRST post-hit cross-turn re-search (the legitimate refinement) is NEVER suppressed;
   only the storm beyond it is.

RESET the marker (disable the gate) immediately on ANY of: UC change, any drift signal, a `faq_miss=true`
(genuine miss), a FINAL_ANSWER/resolution, an escalation/handover, or `record_outcome`.

Keyed PURELY on the EXISTING `activeUseCase` / `driftType` / `faq_miss` signals + a cardinality budget — **NO
query-content / keyword / regex / semantic-similarity / embedding / per-UC matching.** The gate applies ONLY
to `search_knowledge` (read-only retrieval), NEVER to a write/side-effect tool. **If you cannot make the gate
hit cross-turn ≤3 without content matching → STOP-and-surface** (it is then a semantic hardcode, out of
scope — route to human_review_required / M-Auto-4, do NOT ship it).

## Code anchors (verified at HEAD by the deliver-agent)

- `server/src/main/java/com/gumtree/csagent/service/runtime/AgentRunLoopImpl.java`
  - `run(PhasePlan plan, BotSession session, String userMessage, List<...> history)` — line 142; per-turn.
  - A1 per-run cache `successfulDispatchCache` decl line 171; dedup gate 505-526; populate 601-608. **DO NOT TOUCH.**
  - A3 within-turn `lastSearchKnowledgeViableHit` decl line 193; suppression gate 558-576; refresh/reset 626-640. **DO NOT TOUCH** (your cross-turn gate is SEPARATE and ADDITIONAL).
  - `faq_miss` read idiom: `result.getData() instanceof Map<?,?> dataMap` → `dataMap.get("faq_miss") instanceof Boolean fm` (see 627-636).
- `server/src/main/java/com/gumtree/csagent/model/BotSession.java` — entity; persisted `@Column activeUseCase`
  (line 49-50); `@Transient previousActiveUseCase` (162-163) + `@Transient driftType` (165-166); existing
  per-session counter columns (`faqMissCount`, `consecutiveDeadlineCount`, `runtimeErrorCount`) + jsonb columns
  (`formContext`, `customerContext`, `listingContext`, `moderationContext`) are the precedent for your new fields.
- `server/src/main/java/com/gumtree/csagent/service/runtime/SessionManager.java:306` — `processMessage` reloads
  BotSession per turn (the reason cross-turn state MUST be persisted, not `@Transient`).
- `server/src/main/resources/db/migration/` — Flyway; latest is `V15__create_bot_turn_llm_calls.sql` → your
  new migration is **V16**. Pattern (V13): `ALTER TABLE bot_sessions ADD COLUMN <name> <type> [NOT NULL DEFAULT …];`.
- `ControlKernel.java` + `ToolEvent.java` carry the S-Auto-13b `paraphrase_suppressed` annotation/flatten — mirror
  it for a DISTINCT `cross_turn_paraphrase_suppressed` marker.
- Workstream B failing tests: `eval_interactive/tests/regression/test_corpus_lint.py` (`sys.executable` subprocess,
  line 36 — conda-python `ModuleNotFoundError`); `eval_interactive/tests/test_agent_client_session_create_timeout.py`
  (`test_session_create_timeout_constants_widened_to_120s`, line 85-88); the `test_case_spec_overrides` test; and the
  `TestRItemClosuresRecordedInActionBank` test (reads `docs/action_bank.md` rows that moved to
  `docs/action_bank_archive.md` in `0323457`).

## Class

- **Layer**: A = `infra` (new BotSession-scoped cross-turn `search_knowledge` suppression gate, alongside — not
  modifying — the within-turn A3 gate / A1 cache). B = `test-infra`/eval-governance housekeeping.
- **§7 stanza**: REQUIRED (embedded below).
- **Codex**: PER-SUB-SPRINT REQUIRED for A, FOLDED INTO the M-Auto-3 milestone-shared close Codex (the
  deliver-agent authors it at close; you only note dispatch status = PENDING in the handoff).

## Goal & acceptance

**A:** cross-turn `PARAPHRASE_STORM` 21→≤3 AND total (within+cross) ≤3, within-turn still 0, on a 3-pass
`bad_cases` rerun (sim/bot temp=0 / 60s / parallel=1, freshly-restarted backend, S-Auto-11 persisted traces),
with negative controls: (a) UC-change cross-turn search NOT suppressed; (b) drift cross-turn search NOT
suppressed; (c) first cross-turn refinement NOT suppressed (budget-1); (d) genuine-miss re-search NOT
suppressed; (e) write tools never gated; (f) IDENTICAL_RETRY / GATING_RACE / ESCALATION_MISSTAMP detectors
unchanged from S-Auto-12/13/14 levels.

**B:** eval_interactive pytest green+documented under `uv run pytest`; every fix test-infra/governance only;
no agent failure masked.

**Baselines:** Java `1202/1/0/2` + new A tests (Failures stays 1); autoloop `276`; 17-fixture `31`; scoring
SHA `35305bd8…`.

## Scope

### Workstream A (A1–A6)

A1. **Flyway V16** (`db/migration/V16__add_cross_turn_faq_hit_state.sql`): add the standing cross-turn state to
   `bot_sessions` (recommended: a UC marker varchar nullable + the standing viable-hit payload jsonb nullable to
   SERVE on suppression + an `INTEGER NOT NULL DEFAULT 0` budget counter — refine the representation as long as
   the invariant is expressible). Mirror V13's `ALTER TABLE bot_sessions ADD COLUMN …` form.

A2. **BotSession** entity: matching PERSISTED `@Column` field(s) + Lombok getters/setters (NOT `@Transient`).

A3. **Cross-turn gate** in `AgentRunLoopImpl.run`: at run start READ the standing state from `session`; add a
   NEW `search_knowledge` gate applying the 4-condition fail-open invariant; on suppression serve the standing
   payload + annotate `cross_turn_paraphrase_suppressed:true` + the originating turn + do NOT re-dispatch/charge
   a step; at run end / on reset events WRITE BACK the updated state/budget/resets to `session`. Runs ALONGSIDE
   the byte-untouched A1 (505-526) + A3 within-turn (558-576) blocks.

A4. **Drift availability**: confirm `driftType`/`previousActiveUseCase` are reliably set on `session` BEFORE the
   dispatch loop (they are `@Transient`, populated during this turn). If NOT → STOP-and-surface; do NOT
   substitute a content heuristic.

A5. **Trace annotation** parity (ToolEvent + ControlKernel flatten) for the distinct `cross_turn_paraphrase_suppressed`.

A6. **Java tests** (`server/src/test/**`): suppress only when all 4 hold; fail-open per missing condition;
   UC-change / drift / genuine-miss / resolution / escalation / record_outcome reset; budget-1 (first refinement
   allowed, second suppressed); write-tool never gated; null/malformed standing-state guard; plus a test that
   within-turn A3 + A1 behaviour are unchanged.

### Workstream B (B1–B5)

B1. **Re-measure under `uv run pytest`** (`cd eval_interactive && uv run pytest`); record TRUE pass/fail;
   decompose each failure (action_bank-split / corpus_lint-interpreter / case_spec_overrides / timeout-constant /
   OTHER). Any agent-attributable failure → STOP-and-surface.
B2. **action_bank-split tests** (`TestRItemClosuresRecordedInActionBank`): repoint at `docs/action_bank_archive.md`
   (do NOT revert the split).
B3. **corpus_lint subprocess** (`test_corpus_lint.py:36` `sys.executable`): resolve the project venv interpreter
   robustly OR pin the `uv run` invocation as the baseline contract — choose the lower-risk fix for a reproducible green.
B4. **timeout constant + case_spec_overrides**: reconcile the `DEFAULT_READ_TIMEOUT_SECONDS` assertion + the 2
   `test_case_spec_overrides` drifts (fix test or constant so they agree; narrate it).
B5. **Document** the green baseline (before/after + per-failure disposition) in handoff §4.

### Shared measurement (C)

C. Restart `:8080` (`mvn spring-boot:run`, no hot-reload), then 3-pass `bad_cases`
   (`cd eval_interactive && uv run eval-interactive run --path case_specs/bad_cases/ --parallel 1`, ×3, temp=0/60s);
   read persisted traces; report ALL §11 detectors + negative controls. This is the §5.6/§11 close evidence.

## Hard fences / STOP conditions

- **GUARDRAIL 0 — ANTI-误杀 BOUNDARY (HARD; ZERO false suppression — the binding boundary of workstream A):**
  - **Default = ALLOW.** Code the gate as "ALLOW unless all 4 conditions PROVE true"; never "suppress unless proven safe." Any null/missing/unreadable signal → ALLOW. No suppression on incomplete evidence.
  - **Drift always wins**: `driftType` non-null & non-"none" OR `activeUseCase != previousActiveUseCase` → gate DISABLED that turn, no exception.
  - **Budget FIXED at 1**: the first cross-turn `search_knowledge` for a UC after a standing hit is ALWAYS allowed; only the 2nd+ is eligible for suppression. Budget < 1 is FORBIDDEN (re-introduces 误杀). Can't reach ≤3 at budget 1 → STOP-and-surface, do NOT lower it.
  - **Read-only only**: ONLY `search_knowledge` may enter the gate; write/side-effect tools structurally excluded.
  - **Zero-false-suppression is a HARD acceptance bar (not just a STOP)**: in the 3-pass run, audit EVERY `cross_turn_paraphrase_suppressed` annotation in handoff §2 (mirror S-Auto-13b §3(g)); each MUST be a same-UC no-new-searchable-intent re-search. ONE confirmed 误杀 (user introduced new searchable intent, gate suppressed) FAILS the sub-sprint → revert/redesign, do NOT ship.
  - **Negative-control unit tests MANDATORY** (not illustrative): (a) UC-change→ALLOW, (b) drift→ALLOW, (c) first refinement budget-1→ALLOW, (d) genuine-miss→ALLOW, (e) write-tool→never gated, (f) all-4-true→SUPPRESS, (g) null/malformed→ALLOW. All must pass.
- **GUARDRAIL 1**: the gate IS the 4-condition fail-open invariant — keyed ONLY on
  `activeUseCase`+`driftType`+`faq_miss`+cardinality budget; NO content/keyword/regex/similarity/per-UC matching;
  ALWAYS fail-open; ONLY `search_knowledge`.
- **GUARDRAIL 2**: A1 `successfulDispatchCache`, A3 within-turn `lastSearchKnowledgeViableHit` gate, and B1
  `resolveMaxStepsReason` are FINALIZED + byte-untouched. A is a SEPARATE additional gate.
- **Hard-fenced (do NOT edit)**: A1/A3 within-turn blocks; `PhaseEvaluator.java`; `skills/**`; `tool-policy.yaml`;
  `user_simulator.py`; the 4 SHA-locked scoring files; `autoloop/loop.py`+`sandbox/**`+`meta_agent/**`+`cli.py`+
  `preflight.py`; `docs/foundational/**`,`docs/current/**`,`runtime_freeze_and_risk_policy.md`,`docs/teams/**`;
  prior archives. No new escalation_reason enum; no new Tier-0; no autoloop-mutable-surface widening; no B2/B3/C.
- **STOP-and-surface**: any observed 误杀 (legitimate search suppressed); `driftType`/`previousActiveUseCase`
  not reliably available at gate time; cross-turn can't reach ≤3 without content matching; cross-turn stays >3
  after the narrow gate; workstream B reveals a real agent-attributable failure; A would require editing the
  A1/A3/B1 paths; any other hard-fenced surface needs editing.
- **No `git add -A`** (stage explicitly); any `autoloop run` only on a clean committed tree; restart backend
  before measurement; local-Mac only; leave the tree CLEAN+committed at end.

## §7 stanza (REQUIRED)

**Target failure layer:** `infra` (new BotSession-scoped cross-turn `search_knowledge` re-search suppression
gate); workstream B is `test-infra`/eval-governance. No agent semantic *decision* changed; A is a structural
cardinality/idempotency backstop on a read-only tool that yields to the LLM-owned drift signal.

**Tier-0 invariant:** none added; stays `infra`, fail-open, existing signals only. Not added to
`runtime_freeze_and_risk_policy.md`. Tier-0 elevation → `human_review_required`, do NOT self-invent.

**Semantic hardcode:** none. Keyed purely on existing `activeUseCase`/`driftType`/`faq_miss` + cardinality
budget (§1.4); no keyword/regex/enum/per-UC/similarity. Fail-open + drift-reset keep the LLM owning the first
search per UC, the first cross-turn refinement, and drift ownership. Soft-signal-first satisfied via the
S-Auto-13 within-turn falsification (same model/shape; no repeat soft round). No new escalation_reason enum.

**Generalization coverage:** target = cross-turn PARAPHRASE_STORM bad_cases subset (21→≤3, 3-pass) / neighbor =
within-turn cases (stay 0) + same-UC multi-turn continuation / negative = (a)-(f) above / shadow = held-out (not
read by dev). Confirmed at handoff via the 3-pass rerun + negative-control table.

## Handoff requirements

`docs/sprints/sprint-071-handoff.md`: §0 summary (both workstreams, commits, final counts); §1 the cross-turn
gate (narrow-invariant walk; V16 + persisted columns; run-start read / write-back; reset rules; ALONGSIDE the
untouched A1/A3); §2 negative-control table (a)-(f) with trace evidence proving zero 误杀; §3 cross-turn
PARAPHRASE_STORM 3-pass before/after + total + within-turn 0 + IDENTICAL_RETRY/GATING_RACE/ESCALATION_MISSTAMP
unchanged; §4 workstream B baseline reconciliation (before/after under `uv run`, per-failure disposition, no
agent failure masked); §5 baselines + §7-stanza self-walk + fence disposition (explicit "A1/A3 within-turn +
B1 byte-untouched"); §6 OQs; §7 self-check. PLUS **M-Auto-3 close-readiness** (all §11 gates now met +
OQ-S68.1/S70.1 resolved + Codex dispatch status = PENDING for the deliver-agent close Codex).

## Commit discipline

Multi-commit OK (V16+entity / gate+tests / workstream B / handoff). Message:
`Sprint 071 / S-Auto-15 / M-Auto-3 — <description>` + `Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>`.
No `git add -A`. Do not push. Leave the tree clean+committed.

## Self-check (verify before claiming done)

- [ ] Gate suppresses ONLY under all 4 conditions; fail-open on every missing/ambiguous one; keyed ONLY on
  `activeUseCase`+`driftType`+`faq_miss`+budget; no content/similarity matching; only `search_knowledge`.
- [ ] Standing state persisted via V16 + BotSession columns (not `@Transient`); read at run start, written back +
  reset on UC-change/drift/genuine-miss/resolution/escalation/record_outcome.
- [ ] `driftType`/`previousActiveUseCase` confirmed reliably populated at gate time (else STOPPED).
- [ ] A1 cache + A3 within-turn gate + B1 resolver byte-untouched; A is a SEPARATE gate.
- [ ] `cross_turn_paraphrase_suppressed` annotation distinct from within-turn `paraphrase_suppressed`.
- [ ] A tests: suppress-case + all negative controls + within-turn/A1 unchanged.
- [ ] B: TRUE baseline under `uv run pytest`; action_bank-split + corpus_lint interpreter + timeout + case_spec_overrides
  fixed; documented green; no agent failure masked.
- [ ] 3-pass bad_cases: cross-turn 21→≤3 + total ≤3 + within-turn 0; negative controls (a)-(f);
  IDENTICAL_RETRY/GATING_RACE/ESCALATION_MISSTAMP unchanged (backend restarted; persisted traces).
- [ ] Java no new failures beyond `1202/1/0/2` (+new A tests); V16 applies clean; autoloop `276`; 17-fixture `31`;
  scoring SHA `35305bd8…`.
- [ ] No `git add -A`; tree clean+committed; autoloop runs only on clean tree; backend restarted; local-Mac.
- [ ] B2/B3/C NOT touched; no new escalation_reason enum; no Tier-0 self-invented; no hard-fenced surface edited.
- [ ] Handoff §0-§7 filled (incl. §2 negative-control table + M-Auto-3 close-readiness); Codex status noted PENDING.
