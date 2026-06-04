# Dev Prompt — Sprint 074 / S-Auto-19 (M-Auto-5, single sub-sprint)

> Self-contained executable view of `docs/sprint_objective.md` (prompt-artifact-rules §9).
> Paste into a fresh dev session. You need NO other doc except `AGENTS.md`
> (auto-loaded) + this prompt. Code-anchor paths in §9 are read on demand.

## 1. Role identity

You are the **dev agent for Sprint 074 / S-Auto-19**, the SINGLE sub-sprint of
Milestone **M-Auto-5 — Eval Verdict Correctness + Trace-Contract Honesty**.

**One-line goal:** correct **5 deterministic measurement artifacts** that make eval
verdicts wrong — on BOTH the eval-harness side (`eval_interactive`) and the runtime
trace-contract side (`server`) — then re-bless. MEASUREMENT/INFRA sprint: NO
prompt / routing / UC-hypothesis / escalation-posture / skill-soft-field / CaseSpec
rubric edit. The runtime edits are **trace-contract completions** (§1.4), not
semantic changes.

## 2. Read order (minimal)

1. `AGENTS.md` — auto-loaded governance (Constitution §1, §1.5/§1.7 anti-hardcode,
   §5 eval acceptance, §5.4 no eval-side override of a real bug, §5.7 real-LLM gate,
   §7 stanza).
2. **This prompt** — the full contract.
3. On demand only, the §9 code anchors.

Do NOT read `docs/sprints/*` or `docs/archive/*`.

## 3. WHY this sub-sprint exists (read before coding)

The S-Auto-17 re-bless (`m-auto-4-baseline-20260604`, 7 draws/case) exposed that the
autoloop 0-keep pinning has a SECOND cause beyond provider noise: **five
deterministic measurement artifacts** that pin cases to FAIL regardless of bot
behaviour. k-of-n majority CANNOT fix a deterministic false-fail — all n draws fail
identically. Your job is to make each per-case VERDICT reflect ACTUAL bot behaviour.

**THE central discipline — anti-误杀 (§1.7 / §5.4):** every structural fix corrects
HOW a check reads the trace, **never WHAT counts as success**. Each structural fix
(#1/#2/#3/#4) MUST ship a **counter-test proving a genuine same-shape failure still
FAILs**. A fix that merely stops a check firing, with no counter-test, is INCOMPLETE.
(#5 PII is the one explicitly-authorized LIGHT relaxation — see below.)

## 4. Class

- **Layer (primary):** `infra` — eval-harness measurement correctness
  (`eval_interactive/.../scoring/`) + runtime trace-contract completion
  (`server/.../runtime/`; §1.4 persistence + trace contract).
- **§7 stanza:** included (§7 below). Technically §7-EXEMPT (measurement-infra), kept
  for rigor (edits gate-contributing checks + re-blesses).
- **Codex review plan (§4.3):** PER-SUB-SPRINT RECOMMENDED (gate-contributing checks
  + server runtime + re-bless). **You do NOT dispatch Codex.** NOT a fence-#13 SHA
  trigger — the autoloop 5-file `scoring_code_baseline_sha` set
  (`tier_evaluator`/`eval_runner`/`baseline_loader`/`gaming`/`aggregate`) is
  UNTOUCHED; you edit `eval_interactive` harness + `server`. Folds into the M-Auto-5
  milestone-shared close.

## 5. Scope

### Execution order
eval fixes → runtime fixes → backend rebuild + restart → eval-only verdict-delta
(re-score captured traces, deterministic, no LLM) → real-LLM re-bless on a clean tree
→ surface the held S-Auto-17 overnight as launchable (do NOT launch).

### Eval side (`eval_interactive/eval_interactive/scoring/`)

**#3 — `accumulated_tool_results` always unions across turns.**
- Bug: `TraceView.accumulated_tool_results` returns the final-turn ATR when non-empty,
  unioning only when the final turn omits the key. A partial final-turn ATR (e.g.
  `{resolve_article}` after `search_knowledge` was evicted) bypasses the union →
  mandatory `search-knowledge-before-faq-answer` false-fails (17/17 gated; search was
  called in every one).
- Target: union ATR across ALL turns unconditionally (presence-preserving).
- Counter-test: a session that NEVER calls `search_knowledge` still FAILs.
- Char-test: early-turn search + different-tool final turn → PASSes.

**#4 — `intake_fields_collected` accepts the dict the backend emits.**
- Bug: returns a tuple only for a list/tuple; the backend emits `fields_collected` as
  a dict/object → `()` → `contains()` always false → `*-intake-complete-before-handover`
  can NEVER pass (0 PASS / 76 applicable; cs066 `intake_complete=true` still FAILs).
- Target: when `fields_collected` is a Mapping, use its keys.
- Counter-test: `anchor_outcome_uc_g_gdpr` (only `registered_email`, missing
  `data_request_type`) still FAILs.
- Char-test: cs066 (both fields) → PASSes.

**#5 — `no_pii_leakage` LIGHT relaxation (NOT a focus).**
- Bug: the email regex flags the first-party system address `noreply@gumtree.com`
  (13/13 fails).
- Target: stop flagging benign first-party / system addresses. Per human direction PII
  detection is to be relaxed anyway — keep it SIMPLE (a small first-party exclusion);
  do NOT build heavy Tier-0 ceremony.
- Single sanity test: a real user/third-party email (e.g. `someone@gmail.com`) still
  flags. No further counter-test burden.

**#1 (eval side) — `trace_minimum` terminal-disposition-aware.**
- Bug: Mode-1 hard-fails on blank `containment_outcome` regardless of WHY. A one-shot
  `goal_achieved` resolve (simulator-ended) never reaches CLOSE → blank → score zeroed
  before L2/judge. 55 goal_achieved + 10 goal_impossible draws.
- Target: when blank, consult the simulator `stop_reason`: `goal_achieved` → valid
  resolved terminal (no Mode-1 fail); `goal_impossible`/`loop_detected`/
  `max_turns_exceeded` → valid measured terminal (Mode-1 does not fire — the case may
  still fail other checks for the right reason); `error`/`contract_violation`/
  `session_create_failed` → genuine partial instrumentation → still FAIL. Mode-2
  (non-empty user turn, blank bot reply, no handover) UNCHANGED.
- Data dependency: `stop_reason` lives on the simulator `SessionResult`
  (session_runner.py:30-32), NOT on `TraceData`. Thread it into
  `HardChecker.run_checks` / `_check_trace_minimum` — find the scoring call site in
  the executor/composite pipeline and pass it through (eval-harness plumbing, no
  server change).
- Counter-test: blank + `stop_reason=error`/`contract_violation` still FAILs; Mode-2
  still FAILs.
- Char-test: cs095-style `goal_achieved` + blank containment + delivered grounded
  answer → PASSes (and L2/judge then run).

**#2 (eval side) — `source_citation_present` session-accumulated grounding.**
- Bug: requires per-turn `source_ids` on each substantive answer turn; per-turn
  `source_ids` come only from the retrieval turn, but the prompt tells the bot to
  answer from accumulated hits on a LATER turn → answer turn empty (16/35 fails are
  artifacts; all 3 visible-suite fails).
- Target: a substantive answer turn is grounded if its own `source_ids` is non-empty
  OR any earlier turn in the session had `source_ids` (session-accumulated union of
  per-turn source_ids — works with existing trace data).
- Counter-test: a session that retrieves on NO turn but emits a substantive factual
  answer still FAILs (genuine ungrounded — shadow never-searched).
- Char-test: alice-style (search+resolve early, grounded answer later) → PASSes.

### Runtime side (`server/.../runtime/`; §1.4 trace contract, NOT semantic)

**#1 runtime — stamp containment on the success terminal.**
- Target: when the agent loop terminates having delivered a substantive answer with no
  escalation/error, stamp `containment_outcome="resolved"` (a complete terminal
  disposition) even without a dedicated record_outcome-only CLOSE turn. Today only
  CLOSE / ESCALATE stamp it.
- Hard constraint (runtime anti-误杀): NEVER stamp "resolved" on a genuinely
  unresolved terminal (`goal_impossible` / `error` / `loop_detected` / escalation).
- Tests (Java): a one-shot grounded-answer terminal records `resolved`; escalation
  still records `escalated`; an error terminal records neither (blank → eval Mode-1
  still catches it).

**#2 runtime — attach resolved source ids to the answer turn.**
- Target: persist the session's resolved source ids on the answer-turn
  `BotTurn.sourceIds` (today built only from the current turn's `knowledgeHits()`), so
  the answer turn carries its grounding for the trace UI + downstream consumers.
- Tests (Java): an answer turn composed from a prior retrieval turn carries the
  resolved source ids; a turn with no session grounding carries none.

### Re-bless (after eval + runtime land + backend restart)
- On a **clean committed tree**, real-LLM, backend up (creds `autoloop/.env.local`),
  run each suite via the existing re-bless tool → new `baseline_dir`
  `m-auto-5-baseline-YYYYMMDD`. **Retain** `m-auto-4-baseline-20260604` (pointer-only
  move, reversible). Expect `suspect_baseline_manipulation` (explain).
- Keep the Mac AWAKE for the full real-LLM run (a sleep-spanned run is uncertifiable
  — kill + re-run fresh).
- Do NOT launch the held S-Auto-17 overnight — only surface it as now launchable.

## 6. Hard fences / STOP conditions

- **No prompt / routing / UC-hypothesis / escalation-posture / skill soft field /
  CaseSpec rubric edit.** Runtime edits are trace-contract only.
- **No rubric widening to accept a bot mistake (§1.7 / §5.4).** Reads corrected;
  success-definition unchanged.
- **Every structural fix (#1/#2/#3/#4) ships its anti-误杀 counter-test.** Incomplete
  without it.
- **#1 runtime stamping** must NOT fire on unresolved terminals.
- **#5 PII**: light first-party relaxation only; one sanity test that real
  user/third-party PII still flags. Not a focus.
- **Re-bless reversible**: fresh dated dir; never overwrite the retained baseline.
- **Run re-bless / eval only on a clean committed tree** (`project_autoloop_dirty_index_hazard`);
  keep the Mac awake.
- **Do NOT launch the overnight.**
- **STOP-and-surface** if: the legitimate first-party PII set cannot be confirmed
  cleanly (do NOT broaden the allowlist); OR a structural fix cannot be made without
  changing what counts as success (that would be a rubric change → out of scope).

## 7. §7 Layer-classification + anti-hardcode stanza

**Target failure layer:** `infra` — eval-harness measurement correctness
(`eval_interactive/.../scoring/`) + runtime trace-contract completion
(`server/.../runtime/`). No `eval_spec` rubric / `semantic_planner` /
`prompt_projection` / routing change.

**Tier-0 invariant:** adds none; preserves Tier-0 families at current strictness
EXCEPT the explicitly-authorized `no_pii_leakage` first-party relaxation (real
user/third-party PII still flags — sanity-tested).

**Semantic hardcode:** none. Structural reads (cross-turn union; dict-key read;
stop_reason-aware terminal disposition; session-accumulated source-id read) + runtime
containment/source_ids emission. First-party address exclusion is declarative
(company's own published system addresses), not a content rule.

**Generalization coverage:** measurement-infra — evidence is the paired
characterization + anti-误杀 counter-tests per structural artifact + the re-bless
verdict-distribution shift over all suites, not target/neighbor/negative/shadow
case-family counts. L4 shadow firewall unchanged.

## 8. Test / eval requirements

- **eval_interactive pytest:** new char + counter-tests green; no regression vs `503/0`
  under `uv run`.
- **Java:** runtime trace-contract change must not regress vs `1213/1/0/2`; new
  unit/integration tests for #1/#2 runtime green.
- **autoloop pytest:** untouched → no regression vs `324`.
- **Eval-only verdict-delta:** re-score the captured `m-auto-4-baseline-20260604` draws
  with the corrected eval checks (deterministic, no LLM) to isolate the eval-side
  contribution before the runtime change.
- **Real-LLM re-bless (§5.7):** required — the authoritative `m-auto-5-baseline`
  artifact + runtime-emission confirmation (containment/source_ids now present in real
  traces). Captured-trace fixtures are valid evidence for eval-side read-correctness.

## 9. Code anchors (verified 2026-06-04; read/modify on demand)

| Anchor | Path | Use |
|---|---|---|
| `accumulated_tool_results` :487-506 | `eval_interactive/eval_interactive/scoring/skill_procedure_check.py` | #3 union-across-turns |
| `intake_fields_collected` :520-537; `contains()` eval :425 | `eval_interactive/eval_interactive/scoring/skill_procedure_check.py` | #4 dict-key read |
| `_check_no_pii_leakage` :489-501; `PII_PATTERNS` :231-235 | `eval_interactive/eval_interactive/scoring/hard_checks.py` | #5 first-party relaxation |
| `_check_trace_minimum` :662-704 | `eval_interactive/eval_interactive/scoring/hard_checks.py` | #1 eval terminal-disposition-aware |
| `_check_source_citation_present` :819-842; `_is_substantive_factual_answer` :128-178 | `eval_interactive/eval_interactive/scoring/hard_checks.py` | #2 eval session-accumulated |
| `_check_correct_outcome` :200-288 (cross-class guard :268-280 already unions `trace.turns`) | `eval_interactive/eval_interactive/scoring/outcome_checks.py` | #1/#2 coupling — confirm it benefits; no separate change expected |
| `stop_reason` :30-32; `goal_achieved` :223 | `eval_interactive/eval_interactive/simulator/session_runner.py` | #1 data dependency (thread into the check) |
| `containment` :417; `source_ids` :544 | `eval_interactive/eval_interactive/trace/collector.py` | how the eval reads the runtime fields |
| `setContainmentOutcome("resolved")` :558 (CLOSE-only); `recordTurn` `sourceIds` :1656-1687 | `server/src/main/java/com/gumtree/csagent/service/runtime/ControlKernel.java` | #1 + #2 runtime |
| `evaluateClose` :1403-1412; `hasOnlyRecordOutcome` :1393 | `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java` | #1 runtime (CLOSE path) |
| `retrievedSourceIds`/`resolvedSourceIds`/`citedSourceIds` :342-348 | `server/src/main/java/com/gumtree/csagent/model/BotSession.java` | #2 runtime source-id surface |
| `sourceIds` :50-51 | `server/src/main/java/com/gumtree/csagent/model/BotTurn.java` | #2 runtime answer-turn field |
| re-bless tool | `autoloop/scripts/rebless_baseline.py` | re-bless to `m-auto-5-baseline-YYYYMMDD` |
| `baseline_dir` :118 | `autoloop/config.yaml` | pointer move (old retained) |

## 10. Handoff requirements (author `docs/sprints/sprint-074-handoff.md`)

MUST include: per-artifact before/after (bug, corrected read/emission, file:line);
the paired characterization + anti-误杀 counter-test for #1/#2/#3/#4 + the #5 sanity
test; the eval-only verdict-delta + the post-re-bless verdict-distribution shift vs
`m-auto-4-baseline-20260604` (per suite + per artifact: false-fails cleared vs genuine
failures now surfaced); the re-bless record (new dir, old retained, human-authorization,
`suspect_baseline_manipulation` note); confirmation the held overnight is now launchable
(NOT launched); the list of genuine failures the corrected measurement surfaces (input
to the later semantic milestone — UC routing misclassification `correct_uc`, escalation
posture incl. the high-risk under-escalations); §7 self-classification; Codex deferral note.

## 11. Commit discipline

Stage only authorized `eval_interactive` + `server` scope (NOT `git add -A`). One commit
per fix where practical (eval fixes / runtime fixes / re-bless artifact + pointer). Run
re-bless / eval only on a clean committed tree; keep the Mac awake for the real-LLM run.
Do NOT launch the overnight. Deliver-agent-owned files are bundled by the human at close.

## 12. Self-check checklist (complete before claiming done)

- [ ] #3 ATR unions across all turns; never-searched still FAILs; early-search +
      other-final-turn PASSes.
- [ ] #4 `intake_fields_collected` reads dict keys; cs066 PASSes; uc_g_gdpr still FAILs.
- [ ] #5 first-party exclusion (`noreply@gumtree.com` + confirmed set); real
      user/third-party email sanity-test still FAILs (light — not a focus).
- [ ] #1 eval `trace_minimum` terminal-disposition-aware; goal_achieved/goal_impossible
      blank not Mode-1-fail; error/contract_violation blank + Mode-2 still FAIL;
      `stop_reason` threaded.
- [ ] #2 eval `source_citation_present` session-accumulated; alice-style PASSes;
      never-searched still FAILs.
- [ ] #1 runtime stamps `containment_outcome=resolved` on the success terminal; never on
      unresolved terminals; Java tests green.
- [ ] #2 runtime attaches resolved source ids to the answer turn; Java tests green.
- [ ] Backend rebuilt + restarted; eval-only verdict-delta produced.
- [ ] Real-LLM re-bless on a clean tree → `m-auto-5-baseline-YYYYMMDD`; old retained;
      Mac kept awake; `suspect_baseline_manipulation` explained.
- [ ] Java / autoloop / eval_interactive baselines no regression.
- [ ] No semantic / routing / CaseSpec-rubric edit; overnight NOT launched.
- [ ] Handoff written (per-artifact before/after + paired tests + verdict-delta +
      re-bless record + surfaced-genuine-failures list).
