---
title: Sprint 093 / S-Auto-39 handoff — RESOLVE→CONFIRM/CLOSE deadlock corrective
doc_tier: sprint-archive
status: current
implementation_status: implemented
source_of_truth: server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java (mapFinalAnswer RESOLVE branch + priorGroundedResolveAnswerDelivered); ControlKernel.java (signal stash); commit 033abaee
last_reviewed: 2026-06-18
review_cadence: per sub-sprint
supersedes: []
superseded_by: null
notes: >
  Sub-sprint of M-Auto-7. PRIMARY structural phase-transition repair so a
  grounded UC-A RESOLVE answer can reach CONFIRM. Bounded real-LLM run
  shows the structural deadlock is repaired (CONFIRM reachable) but
  end-to-end resolve-RECORDING remains capped by a downstream CONFIRM-phase
  issue (record_outcome non-landing + reason mislabel) → new OQ. No CS4
  success claimed; pilot HELD; exp-82 WITHDRAWN.
---

# Sprint 093 / S-Auto-39 handoff

**Goal:** break the RESOLVE→CONFIRM/CLOSE deadlock so a completed grounded
resolve answer can reach a confirmable/recordable state and
`record_outcome(resolve)` lands — without relaxing the premature-resolve
guard.

**Layer (§3.2):** `skill_state`. **§7 stanza:** in `sprint_objective.md`.
**Per-sub-sprint Codex:** see §Codex below.

---

## §1 Step-0 separation list (read-only, NO LLM)

Full artifact: `docs/diagnostics/sprint-093-step0-escalation-attribution.md`.

Over exp-86 deep PRIMARY UC-A traces (46 draws across 5 attempts + 8
oversample + representative) classified per draw by phase sequence,
`record_outcome` premature-guard hits, grounding, containment, reason,
and L2 `correct_outcome`:

| class | count | meaning |
|---|---|---|
| **DEADLOCK_DOWNSTREAM_MISSTAMP** | 12 | grounded + premature-guard hit + escalated + `co=0.0` → mis-attributed reason |
| **DEADLOCK_STALL_NO_RECORD** | 1 | grounded + premature + blank containment, no record |
| OUTCOME_CREDITED | 24 | resolved/accepted-escalation; ≥6 only "lucky" via `max_turns` `isResolvedSuccessTerminal` despite premature hits |
| ESC_NO_GUARD_HIT | 5 | escalated, grounded, never tripped guard — **anti-误杀 boundary, NOT to be force-converted** |
| INFRA_TIMEOUT | 3 | `stop=timeout`/`turns=0` |
| OTHER | 1 | grounded, blank, no guard hit |

**13/46 deadlock-attributable.** baseline-20260608 shallow corroborates
(8 `user_requested` + 5 `faq_miss` + 2 `clarification_budget` + 1
`turn_budget` = 16 deadlock-shape PRIMARY escalations). Example evidence:
`alice_uc_a_uc_h_misclass` stamped `user_requested` with **no user request
in the transcript** (pure runtime mis-stamp downstream of the deadlock).

---

## §2 Failure Brief + §3 classification

`docs/diagnostics/failure-briefs/sprint-093-resolve-confirm-deadlock.md`.
Layer **`skill_state`** (§3.2 Q4 — multi-turn phase machine loses the
ability to progress to the phase where the outcome can be recorded). No
§STOP trigger: the minimal fix repairs the phase transition WITHOUT
relaxing the premature guard, weakening a Tier-0/frozen invariant, adding
a user-message content heuristic, adding a UC/case exception, or faking a
stamp.

---

## §3 Runtime diff + commit

**Commit `033abaee`** — `M-Auto-7 S-Auto-39 — break RESOLVE→CONFIRM/CLOSE
deadlock (structural promotion)`.

Root cause: `ResolveDispositionEvaluator.evaluate()` returns
`READY_TO_CONFIRM` only when a `record_outcome` **succeeded** this run, but
`shouldRejectPrematureResolveOutcome` rejects `record_outcome(resolve)`
outside CONFIRM/CLOSE — a circular deadlock. A grounded answer maps to
`ANSWERED_SUBTASK`/RESOLVE, the loop re-answers and eventually
`request_handover`s, overwriting an earlier transient `resolved` stamp
with `escalated` + a mis-attributed reason.

**PRIMARY fix** (`PhaseEvaluator.mapFinalAnswer`, RESOLVE/FAQ branch):
when a grounded FINAL_ANSWER was delivered on a **prior RESOLVE turn** (a
persisted `BotTurn` with `phase_after == RESOLVE` and non-empty
`source_ids` — `priorGroundedResolveAnswerDelivered`) AND this subsequent
user turn again produces a confident grounded non-slot answer
(`ANSWERED_SUBTASK`), promote RESOLVE→CONFIRM
(`progressive_resolve_confirmable`). A `record_outcome` rejected **only**
by the premature guard is excluded from the Sprint 9 §O1
`record_outcome_failed_retry` loop **only** when prior grounding exists.
Trigger is purely structural (turn record); **no user-message content
heuristic**. Structural signal derived in `ControlKernel` from persisted
history and threaded via a `@Transient BotSession` field (no
`interpretRunResult` signature change → existing mock callers unaffected).

PRESERVED: the premature guard, the `READY_TO_CONFIRM` path,
genuine-failure retry, slot-request stay, and first-answer
premature-collapse protection (first grounded answer with no prior
grounding stays in RESOLVE). No SECONDARY terminal-stamp extension was
needed (PRIMARY was minimally fixable).

Files: `PhaseEvaluator.java`, `ControlKernel.java`, `BotSession.java`
(transient field), `Sprint93ResolveConfirmDeadlockTest.java`.

---

## §4 Java characterization tests

`Sprint93ResolveConfirmDeadlockTest` (11 tests, all green):

- deadlock shape (prior grounded answer + premature-rejected record OR
  bare re-answer) → **CONFIRM** `progressive_resolve_confirmable`;
- first grounded answer / DISCOVER-search-only prior turn → stays RESOLVE
  (premature-collapse protection);
- premature guard intact: `record_outcome(resolve)` still rejected in
  RESOLVE, permitted in CONFIRM/CLOSE;
- slot-request answer with prior grounding stays RESOLVE;
- genuine (non-premature) record failure still retries in RESOLVE;
- successful record keeps the unchanged `READY_TO_CONFIRM` → `answer_provided`;
- promotion targets CONFIRM never CLOSE (no early-close); helper truth-tables.

**Full Java suite: 1394 run, 1 failure, 0 errors, 2 skipped → 0 NEW
regressions.** Sole failure = pre-existing
`SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly`
(OQ-S41.5), **reproduced identically on a clean tree** (stash verified) —
a system-prompt anchor assertion, out of scope (this diff touches no
prompt resource). §5.5 Java-suite hard gate: **PASS.**

---

## §5 Step-4 bounded real-LLM validation run

**Pre-flight (§5.9): GO** — backend fresh-booted with the fix on the
classpath (`Started CsAgentApplication`); Flyway V17 applied
(`success=t`, matches highest migration file); no proxy env; CaseSpecs
self-contained; smoke (n=1 over the 6 target cases) confirmed the fix
**fires** (`cs_uc_a_loaded_listing`: T1 RESOLVE 1st answer → T2 **CONFIRM**
promotion → `resolved`). No NO-GO anomalies (A1–A11): `search_knowledge`
hits non-empty, no `(temp)` leakage, Java baseline matches
`1394/1/0/2`-shape sole-inherited-failure.

**Run:** `eval_interactive run --path <subset>` repeated (PRIMARY ×11,
others ×5) under `caffeinate`, backend `localhost:8080`, bot=deepseek-v4-flash,
simulator=moonshot, temp 0.0. **42 draws.** Per-draw data:
`/tmp/s39_matrix_manifest.txt` → `eval_interactive/results/*` (gitignored).

### Qualifying-draw metric (power floor: ≥3 → not INCONCLUSIVE)

PRIMARY qualifying draws = **22** (≥3 → conclusive). Qualifying =
entered RESOLVE + grounded FINAL_ANSWER + ≥2 turns (subsequent user turn).

| case | role | draws | qual | resolved(co=1.0) | reached CONFIRM | escalated |
|---|---|---|---|---|---|---|
| `cs_uc_a_loaded_listing` | PRIMARY | 11 | 11 | 3 | 5 | 6 |
| `cs_uc_a_no_ad_id_ad_specific` | PRIMARY | 11 | 11 | 4 | 6 | 7 |
| `cs_uc_a_generic_policy_question` | neg-ctrl | 5 | 5 | 3 | 0 | **0** |
| `cs_uc_a_lookup_failed` | neighbor | 5 | 5 | 5 | 4 | 3 |
| `cs_uc_fp_loaded_moderation` | neighbor | 5 | 5 | 2 | 1 | 3 |
| `cs11g02_uc_d_explicit_distress` | genuine-esc | 5 | (3) | — | 0 | **5 (co=1.0)** |

### What the fix ACHIEVED (proven)

1. **Permanent RESOLVE-stuck deadlock broken.** 11/22 PRIMARY qualifying
   draws now reach CONFIRM (0 in the deadlocked baseline shape). The
   structural transition fires exactly as designed (smoke + characterization
   tests + per-turn traces, e.g. `loaded_listing` T0/T1 RESOLVE → T2 CONFIRM).
2. **Blank/stale containment dropped**; sessions no longer loop in RESOLVE
   re-answering the same grounded content until turn exhaustion.
3. **Anti-误杀 INTACT.** Genuine-escalation control `cs11g02_uc_d_explicit_distress`:
   **5/5 escalated, 5/5 `co=1.0`** (correct escalate). Neg-control
   `cs_uc_a_generic_policy_question`: **0 escalations** (no over-correction
   into escalation; no new false positives). UC-FP / `lookup_failed`
   neighbors: no escalation regression vs the deadlock baseline.

### What the fix did NOT achieve (honest shortfall → OQ)

1. **`record_outcome(resolve)` lands in 0/42 draws.** Even in CONFIRM the
   bot does not successfully call `record_outcome`. Acceptance criterion
   (2) is met ONLY via the alternative "product-contract-allowed resolved
   terminal" (the `isResolvedSuccessTerminal` grounding stamp), NOT via a
   landed `record_outcome`. The resolved PRIMARY draws split:
   `loaded_listing` reaches resolved via CONFIRM→terminal stamp (fix path);
   `no_ad_id` resolved draws reach it via `max_turns` stamp in RESOLVE with
   `conf=0` (the OLD lucky path — the bot keeps ASKING for the ad id
   (`ASKED_FOR_SLOT`), so the promotion legitimately does not fire and the
   premature loop persists, up to `prem=7`).
2. **PRIMARY resolved-rate 7/22 (32%)** — not a majority flip (none
   required per the prompt). 13/22 PRIMARY draws still escalate. Of those
   13: ~2 are CLEARLY genuine (the bot exhausted grounded help, OFFERED to
   escalate, and the user explicitly accepted — "Yes, please. I'd
   appreciate speaking to someone"; escalation is the CORRECT outcome the
   CaseSpec scores `co=0.0`); the other ~11 are the bot escalating after the
   bad-case persona restated a genuinely-hard problem (ad active-but-not-showing
   / no-views-despite-all-tips) — defensible CS behaviour on an
   unresolvable bad-case, but carrying a **mislabeled `user_requested`
   reason** (the documented runtime reason-mislabel pattern,
   `[[project_faq_overescalate_maxsteps_misstamp]]`).

### Verdict

The **structural deadlock (RESOLVE→CONFIRM transition unreachable) is
repaired and validated** (conditions 1/3/4 substantially met; CONFIRM now
reachable; deadlock loops + blank containment gone; anti-误杀 intact). The
fix is **necessary-but-not-sufficient** for end-to-end resolve-RECORDING
on these PRIMARY bad-cases: condition (2) `record_outcome(resolve)` does
not land in any draw, capped by a **downstream CONFIRM-phase issue**
(the bot escalates / terminal-stamps instead of recording, and the
escalation reason is mislabeled). **No CS4 success claimed; no PRIMARY
majority flip claimed (none required); pilot HELD; exp-82 WITHDRAWN.**

**→ OQ-S93.1 (new, follow-up):** in CONFIRM, a grounded UC-A session that
the persona keeps engaging neither lands `record_outcome(resolve)` nor is
credited via a clean confirm — it escalates with a mislabeled
`user_requested` reason. The deadlock had a SECOND layer beyond the phase
transition. Candidate layers: `semantic_planner` (CONFIRM-phase
record-vs-handover decision) and `infra`/reason-resolver (the
`user_requested` mislabel). Route via research before any encoding.

---

## §5b Step-5 zero-LLM cross-check (evidence boundary)

1. **Evaluator/CaseSpec semantics unchanged (5.1 + 5.3).** This sub-sprint
   touches ONLY server-side runtime Java + docs + one Java test; it edits
   no `eval_interactive/` evaluator code and no CaseSpec. `git diff
   a428aa18..HEAD` over `eval_interactive/` + the 6 CaseSpec paths is
   **empty** → the evaluator + CaseSpecs are **byte-identical** between the
   exp-86-era OLD commit and the validation HEAD; a separate OLD-trace
   replay is moot because the scorer is a different process/language the
   runtime change cannot reach. (NOTE: `baseline-20260608` @ `92c40761`
   used OLDER scoring — diff non-empty — so it is **forensic-only**, not the
   byte-identity reference.)
2. **NEW-trace attribution (5.2).** For each newly-CONFIRM-reaching draw,
   the per-turn trace shows the success path: prior RESOLVE grounded turn →
   this turn's grounded `ANSWERED_SUBTASK` → `phase_after=CONFIRM`
   (`progressive_resolve_confirmable`) → CONFIRM-phase terminal. The
   deadlock-lift is attributable to the phase transition, not an evaluator
   change.
3. **Pinned hashes (5.3).**
   - CaseSpec sha256 (validation == OLD, unchanged):
     - `cs_uc_a_no_ad_id_ad_specific.yaml` `6e602566…`
     - `cs_uc_a_loaded_listing.yaml` `544b8816…`
     - `cs_uc_a_generic_policy_question.yaml` `2373ee0d…`
     - `cs_uc_fp_loaded_moderation.yaml` `de55e126…`
     - `cs_uc_a_lookup_failed.yaml` `e25860d2…`
     - `cs11g02_uc_d_explicit_distress.yaml` `fe77aeab…`
   - eval_interactive evaluator sha256 (validation == OLD `a428aa18`):
     `composite.py` `79b21361…`, `outcome_checks.py` `f101eb5c…`,
     `hard_checks.py` `3d93b1a2…`, `skill_procedure_check.py` `06bd7cd3…`,
     `escalation_reason_match.py` `f8390315…`.
   - Recorded autoloop `scoring_code_baseline_sha` (config.yaml main):
     `f2f983cc…` (5-file set; not edited by this sub-sprint).

---

## §6 Self-check / fences

- [x] Step 0 attribution produced (read-only, no LLM) before any code.
- [x] PRIMARY phase-transition fix attempted first; SECONDARY not needed.
- [x] No user-message content heuristic; premature guard intact; no
      CaseSpec/evaluator edit; no `max_turns` bump; no faked stamp; no
      UC/case exception.
- [x] Java characterization tests green; full suite 0 new regressions.
- [x] §5.9 pre-flight GO before the bounded run; run on a clean committed tree.
- [x] Qualifying-draw count reported (22 PRIMARY ≥3 → conclusive, not
      INCONCLUSIVE).
- [x] §5 OLD-byte-identity vs NEW-attribution split; hashes pinned.
- [x] Genuine-escalation control still escalates (5/5 co=1.0);
      over-escalation neg-control no regression (0 new escalations).
- [ ] Codex `pass` — see §Codex (pending dispatch). No pilot-resume talk
      until `pass`.

**Restatement:** no PRIMARY-majority flip is required and none is claimed;
no CS4 success claimed; the structural deadlock is repaired and validated
while end-to-end resolve-recording remains capped by OQ-S93.1; pilot stays
HELD; objective-alignment annotation NOT implemented; exp-82 WITHDRAWN.
