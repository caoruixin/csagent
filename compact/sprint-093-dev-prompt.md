# Dev prompt — Sprint 093 / S-Auto-39 (M-Auto-7): RESOLVE→CONFIRM/CLOSE phase-transition corrective

You are the **dev agent for Sprint 093 / S-Auto-39 (M-Auto-7)**. One-line goal:
**break the RESOLVE→CONFIRM/CLOSE deadlock so a completed grounded resolve answer
can reach a confirmable/recordable state and `record_outcome(resolve)` lands —
without relaxing the premature-resolve guard.**

**Read order (minimal):** `AGENTS.md` (auto-loaded) + this prompt. Code anchors
to inspect: `server/.../runtime/skill/SkillGuardrailDispatcher.java`
(`handlePrematureResolveOutcomeGuard`), `server/.../runtime/ResolveDispositionEvaluator.java`
(`shouldRejectPrematureResolveOutcome`, `evaluate`), `server/.../runtime/ControlKernel.java`
(the terminal-stamp arms ~585–660 + the phase-transition path ~663–713). Do NOT
start coding until Step 0 is done.

## Root cause (confirmed read-only 2026-06-18)
`record_outcome(resolve)` is correctly rejected outside CONFIRM/CLOSE, but
`ResolveDispositionEvaluator.evaluate()` only returns `READY_TO_CONFIRM` when a
`record_outcome` already succeeded this run → a grounded UC-A resolve answer can
stall in RESOLVE (trace: phase=RESOLVE for 7 turns, `record_outcome(FAIL)` at
T0+T5 → `max_turns`/stale-resolved; or `bot_ended` blank at 3 turns) → blank/stale
containment, turn-budget exhaustion, or mis-attributed escalation.

## Class
- **Layer (§3.2):** `skill_state` (runtime multi-turn phase machine). Escalate to
  `human_review_required` only under the §STOP triggers.
- **§7:** REQUIRED (stanza below). **Per-sub-sprint Codex:** REQUIRED (binding gate
  before any pilot-resume).

## Fix-direction priority (binding order)
1. **PRIMARY — repair the formal RESOLVE→CONFIRM/CLOSE transition.** When a
   grounded FINAL_ANSWER has formed AND a subsequent user turn has occurred, the
   runtime must be able to enter CONFIRM/CLOSE so `record_outcome(resolve)` is
   permitted and lands. Trigger must be **structural** (grounded FINAL_ANSWER +
   subsequent user turn), never a user-message keyword/content heuristic.
2. **SECONDARY (only if PRIMARY is proven not minimally fixable)** — a bounded
   extension of `isResolvedSuccessTerminal`, WITH an in-handoff justification that
   the phase transition could not be minimally repaired. The terminal stamp must
   NOT be a shortcut to bypass the phase machine; do NOT stack further local
   resolved-stamp patches.

## Scope (ordered)
- **Step 0 — Pre-dev escalation attribution (read-only, NO LLM).** Over the exp-86
  + `m-auto-7-prepilot-baseline-20260608` PRIMARY traces, per draw classify each
  `faq_miss_threshold_exceeded` / `turn_budget_exhausted` / `user_requested` as
  **deadlock-downstream runtime mis-stamp** vs **genuine LLM escalation**. Output a
  separation list (draw → class → evidence: phase sequence, record_outcome guard
  hits, stop_reason).
- **Step 1 — Failure Brief** under `docs/diagnostics/failure-briefs/` + §3
  classification (confirm `skill_state` or escalate per §STOP).
- **Step 2 — Minimal runtime fix** per the priority order (no user-message content
  heuristic).
- **Step 3 — Java characterization tests:** deadlock shape now reaches
  CONFIRM/CLOSE + records; a genuinely premature record (RESOLVE, no subsequent
  user turn) still rejects; no double-record, no early-CLOSE, no false-resolved stamp.
- **Step 4 — Bounded NON-pilot real-LLM validation run** (below).
- **Step 5 — Zero-LLM attribution cross-check** (below).

## Acceptance — qualifying-event metric (NOT PRIMARY majority-pass)
**Qualifying draw shape:** UC/plan entered RESOLVE + grounded FINAL_ANSWER formed
+ a subsequent user turn occurred. For qualifying draws, accept iff **all**:
(1) not permanently stuck in RESOLVE; (2) `record_outcome(resolve)` succeeds OR a
product-contract-allowed resolved terminal lands; (3) the same shape no longer
repeatedly trips `progressive_resolve_record_outcome_premature`; (4) blank/stale
containment + deadlock loops disappear or drop markedly.
**Power floor:** <3 qualifying draws in the bounded run → conclusion
**`INCONCLUSIVE`** (add more samples of the SAME bounded set only; does NOT
authorize pilot resume).
**Anti-误杀:** genuine over-escalation, UC-misclassification, genuine STALL, and the
genuine-escalation control must still fail / still escalate; negative control no regression.

## Step 4 — Bounded run (eval_interactive; backend rebuilt on the fix; §5.9 pre-flight first; `caffeinate`)
| case | role | n |
|---|---|---|
| `cs_uc_a_no_ad_id_ad_specific` | PRIMARY | 11 |
| `cs_uc_a_loaded_listing` | PRIMARY | 11 |
| `cs_uc_a_generic_policy_question` | neg-control (anti-over-correction) | 5 |
| `cs_uc_fp_loaded_moderation` | neighbor | 5 |
| `cs_uc_a_lookup_failed` | neighbor | 5 |
| genuine-escalation high-risk control (candidate `cs11g02_uc_d_explicit_distress`, `should_escalate=true`; confirm baseline-stable escalation) | NEW neg-control (anti-false-resolve) | 5 |

No `autoloop run`, no candidate search, no full pilot.

## Step 5 — Zero-LLM cross-check (evidence boundary — do not conflate)
1. **OLD-trace replay** under the unchanged evaluator → proves evaluator/CaseSpec/
   scoring **semantics unchanged**. Alone does NOT validate the runtime change.
2. **NEW-trace attribution** → for each newly-passing qualifying draw, show from
   phase transition + tool result + containment + failure tags that the success
   comes from the deadlock being lifted.
3. **Pin + report** evaluator / CaseSpec / scoring hashes (`scoring_code_baseline_sha`
   + CaseSpec file hashes) OLD vs validation; demonstrate byte-identical.

## §7 stanza
**Target layer:** `skill_state` (escalate `human_review_required` per §STOP).
**Tier-0:** adds none; PRESERVES the premature-resolve guard + §1.4 trace-contract;
modifies the frozen `ResolveDispositionEvaluator`/`isResolvedSuccessTerminal`
surface under this contract's authorization; no `runtime_freeze_and_risk_policy.md`
invariant weakened. **Semantic hardcode:** none (structural transition; no
content heuristic). **Generalization coverage:** target/neighbor/negative/shadow =
2/2/2/(held-out).

## Hard fences / STOP
No relaxing the premature guard · no lowering `record_outcome` · no CaseSpec edit ·
no evaluator pass-criterion edit · no PRIMARY/UC/case-specific exception · no
user-message content heuristic · no `max_turns` increase or faked resolved stamp ·
over-escalation/UC-misclass/STALL/genuine-escalation-control must still fail-or-escalate ·
terminal-stamp extension is SECONDARY only (needs the proven-not-minimally-fixable
justification). **STOP + `human_review_required`** if the minimal fix would require:
relaxing the premature `record_outcome` constraint; weakening a Tier-0/frozen
invariant semantic; a user-message keyword/content heuristic; a PRIMARY/UC/case
exception; or masking the deadlock via a faked stamp / max_turns bump.
**Pilot stays HELD; objective-alignment annotation NOT implemented; exp-82 WITHDRAWN.**

## Codex review plan
Per-sub-sprint Codex REQUIRED — §4.1 kernel on the runtime diff + Step-3 tests,
reviewed against Step-0 attribution + the §4 bounded-run + §5 cross-check (incl.
pinned hashes). Verdict → `docs/codex-findings.md` (§4.2 header). No pilot-resume
discussion until `pass`.

## Handoff requirements
`docs/sprints/sprint-093-handoff.md`: Step-0 separation list; Failure Brief + §3
class; runtime diff + commit sha (+ SECONDARY-path justification if used); Java
tests; §4 results with qualifying-draw count + per-draw verdict (or `INCONCLUSIVE`
if <3); §5 OLD-replay + NEW-attribution split with pinned hashes; genuine-escalation
control result; Codex verdict; explicit restatement that no PRIMARY-majority flip is
required, no CS4 success claimed, pilot HELD, exp-82 WITHDRAWN.

## Commit discipline
Stage explicitly by file (NO `git add -A`). Runtime fix + tests = one commit;
bounded-run/cross-check artefacts are gitignored data. Run any real-LLM step on a
clean committed tree.

## Self-check before close
- [ ] Step 0 attribution list produced (read-only, no LLM) before any code.
- [ ] PRIMARY phase-transition fix attempted first; SECONDARY only with justification.
- [ ] No user-message content heuristic; premature guard intact; no CaseSpec/evaluator edit.
- [ ] Java characterization tests green (deadlock recovers; premature still rejects; no double/early/false stamp).
- [ ] §5.9 pre-flight go before the bounded run; run on a clean committed tree.
- [ ] Qualifying-draw count reported; `INCONCLUSIVE` if <3 (no pilot resume).
- [ ] §5 OLD-replay vs NEW-attribution split; hashes pinned + reported.
- [ ] Genuine-escalation control still escalates; over-escalation/UC-misclass/STALL still fail.
- [ ] Handoff complete; Codex `pass` before any pilot-resume talk.
