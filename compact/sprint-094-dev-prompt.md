# Dev prompt — Sprint 094 / S-Auto-40 (M-Auto-7): WP1-A measurement contract → conditional outcome acceptance

You are the **dev agent for Sprint 094 / S-Auto-40 (M-Auto-7), WP1-A only**. One-line
goal: encode the product decision — for a UC-A bad-case with closure-qualified grounded
help, **resolve if the user is satisfied / escalate if the user remains unresolved** —
as a **declarative, condition-bound** outcome acceptance for the two PRIMARY cases, on a
**positive structured** user-state signal. **Measurement-first** (current traces lack the
signal). **No bot/runtime/prompt/reason change.**

**Read order (minimal):** `AGENTS.md` (auto-loaded) + this prompt. Code anchors:
`eval_interactive/eval_interactive/simulator/session_runner.py` (loop; `bot_ended` break
at ~196-199 is *before* `generate_next` at ~217 → that's why escalation draws carry no
terminal `goal_status`), `simulator/user_simulator.py` (`generate_next` → `goal_status`),
`case_spec/schema.py` (`acceptable_outcomes` is a FLAT set — do NOT use it unconditionally),
`scoring/outcome_checks.py` (`correct_outcome`). Bounded-run traces:
`eval_interactive/results/2026-06-18-*` (Sprint 093, read-only evidence).

## Product decision (binding)
Escalation-after-genuine-help is a VALID terminal when the user remains unresolved; it is
NOT a failure and you must NOT force resolve. This is a product-owner-authorized §5.4
`eval_spec` fix (not a masking widen) — the anti-误杀 matrix below keeps early/lazy/over-
escalation FAILing.

## Step 0 — blocking measurement gate (deliver pre-answered: INSUFFICIENT)
Three states: **SATISFIED** (`goal_status=achieved`/`stop_reason=goal_achieved`);
**UNRESOLVED** (needs a POSITIVE, structured, post-grounded-help user-state signal);
**UNKNOWN** (neither). **Forbidden as UNRESOLVED proxy:** absence of `goal_achieved`,
bot chose handover, escalation reason, user free-text keywords. Deliver verified the
Sprint 093 traces are INSUFFICIENT (13/13 escalations `bot_ended`; no persisted per-turn
`goal_status`; no `user_state` field). Re-confirm; proceed measurement-first.

## Phase 1 — trace-only measurement contract (`infra`, eval-framework)
Persist a positive structured post-help user-state signal from the simulator's OWN
per-turn generation:
- produced **in the same `generate_next` call** that makes that user turn, written to the
  trace immediately (priority: persist the structured state the simulator already
  produces; if `goal_status` can't yield a positive UNRESOLVED distinct from
  neutral/new-goal, enrich the **same call** with a structured `user_state` field);
- carries `turn_id`, `signal_source`, `schema_version`;
- alignable to whether it occurred **after** the closure-qualified-help structural marker;
- **NO** post-`bot_ended` LLM call; **NO** back-inference from outcome/handover/reason/
  absence-of-satisfaction; **NO** carry-forward of pre-help state as post-help;
- missing/unalignable → uniformly `UNKNOWN`; `goal_impossible` NOT auto-mapped to
  `UNRESOLVED` (keep separate / `UNKNOWN`).
No bot/runtime/prompt change. Unit tests + dry check.

## Phase 2 — bounded real-LLM validation (decidable traces)
Small bounded run (2 PRIMARY + controls/neighbors; §5.9 GO first; `caffeinate`; no pilot,
no candidate search) producing decidable traces. **Evidence floor (else `INCONCLUSIVE` →
stop, no re-bless, no pilot):** ≥1 `SATISFIED`; **≥3 positive post-help `UNRESOLVED`**;
`UNKNOWN`/neutral samples; handover before/after timing; no outcome leakage. One existing
stable satisfiable control MAY be added only to validate the measurement contract (NOT
WP2).

## Phase 3 — declarative three-state conditional acceptance (`eval_spec`)
Declarative schema + generic evaluator (no case-id hardcode, no free-text) reading the
Phase-1 signal, applied to ONLY `cs_uc_a_no_ad_id_ad_specific` + `cs_uc_a_loaded_listing`:
- `SATISFIED`+resolve → PASS · `SATISFIED`+escalate → FAIL
- `UNRESOLVED`+structural grounded-help precondition+escalate → `CONDITIONAL_ELIGIBLE /
  REVIEW_REQUIRED`
- `UNRESOLVED`+false resolve → FAIL · `UNKNOWN`/neutral/unrelated-new-goal+escalate →
  not auto-PASS
**Closure-quality:** structural marker (tools+`source_ids`+grounded answer) proves only
the precondition, NOT that closure was met → evaluator emits `CONDITIONAL_ELIGIBLE`,
never auto-PASS on the marker alone. **Reproducible adjudication (binding):**
`CONDITIONAL_ELIGIBLE` ≠ final PASS; final escalate-acceptance must reference a
versioned, auditable adjudication artifact keyed by trace (trace ID/hash; CaseSpec/
closure-criterion version; reviewer verdict; rationale; timestamp). Missing → stays
`REVIEW_REQUIRED`. Evaluator reads the artifact; it must NOT call ad-hoc human judgment
at runtime.

## §3.3 anti-widen replay (zero-LLM, over new decidable traces) — must prove
None may enter the unresolved-accepted branch: grounded+`SATISFIED`+resolve PASS ·
grounded+positive-`UNRESOLVED`+closure-qualified+escalate → `CONDITIONAL_ELIGIBLE`
(PASS only via adjudication) · early/lazy escalation FAIL · escalation-after-satisfaction
FAIL · incomplete/non-grounded-then-escalation FAIL · UC-misclass/STALL/empty/
false-resolved FAIL · generic-policy resolve-only · user-state unknown after grounded →
not accepted · neutral reply → not accepted · unrelated new-goal without original-unresolved
→ not accepted · bot proposed handover, user not responded → not accepted ·
handover-caused missing goal_status → `UNKNOWN`, not accepted · satisfaction-then-escalation
FAIL.

## Baseline migration (blocking)
Old traces lack the field → only `UNKNOWN`; old-replay can't rebuild a conditional
baseline. Specify+implement OR STOP: missing→`UNKNOWN`; the 2 PRIMARY get
new-instrumentation decidable baseline draws; form a complete, loadable,
provenance-bearing run-scoped baseline; decide whether other cases reuse old counts; if
per-case replacement, PROVE loader/aggregation supports it without mixed-semantics; if
not safely combinable → STOP → formal re-bless. Canonical pointer UNCHANGED.

## Hard fences / STOP
WP1-A only; no bot/runtime/prompt/reason-enum/handover change · no unconditional
`resolve OR escalate` · branch on positive structured signal only (no free-text, no
post-hoc LLM, no back-inference) · 2 PRIMARY only; neighbors read-only · generic-policy
resolve-only · no canonical pointer flip · pilot/annotation/WP1-B/WP2 HELD; exp-82
WITHDRAWN. **STOP + surface** if: no same-call positive UNRESOLVED signal possible;
Phase-2 floor unmet (INCONCLUSIVE); baseline not safely combinable; or a branch could
only be decided via free-text/post-hoc-LLM/back-inference.

## §7 stanza
Target layer `infra` (Phase 1) + `eval_spec` (Phase 3); adds no Tier-0; §5.4
product-authorized (anti-误杀 matrix preserved); no semantic hardcode (same-call
structured signal + declarative conditional read); coverage = 2 target / 2 neighbor
(read-only) / generic-policy + §3.3 edge battery negative / shadow held-out.

## Codex review plan
Per-sub-sprint Codex REQUIRED — kernel on the Phase-1 instrumentation + the Phase-3
declarative evaluator + the 2 CaseSpec changes, against the §3.3 blast-radius. Verdict →
`docs/codex-findings.md` (§4.2). No pilot-resume / WP1-B decision until `pass`.

## Handoff (`docs/sprints/sprint-094-handoff.md`)
Step-0 re-confirmation; Phase-1 signal schema + provenance + tests; Phase-2 evidence-floor
table (or INCONCLUSIVE); declarative schema + evaluator + §3.3 replay matrix; adjudication
artifact format; baseline-migration decision; Codex verdict; restatement (WP1-A only;
no bot/prompt/reason change; pilot+annotation+WP1-B+WP2 HELD; exp-82 WITHDRAWN; canonical
pointer unchanged).

## Commit discipline
Stage explicitly by file (NO `git add -A`). Phase-1 / Phase-3 / CaseSpec are separable
commits; bounded-run artifacts gitignored. Real-LLM steps on a clean committed tree.

## Self-check before close
- [ ] Step 0 re-confirmed; measurement-first.
- [ ] Phase-1 signal is same-call, provenance-tagged, post-help-alignable; missing→UNKNOWN; no back-inference/carry-forward/post-hoc-LLM.
- [ ] Phase-2 evidence floor met (≥1 SATISFIED, ≥3 post-help UNRESOLVED, UNKNOWN/neutral, handover timing, no leakage) — else INCONCLUSIVE + STOP.
- [ ] Phase-3 declarative, no case-id/free-text; three-state matrix + §3.3 battery all hold on zero-LLM replay.
- [ ] CONDITIONAL_ELIGIBLE never auto-PASS; adjudication artifact versioned/auditable; evaluator reads it, no runtime human call.
- [ ] Baseline migration decided (or STOP→re-bless); canonical pointer unchanged.
- [ ] 2 PRIMARY only; neighbors read-only; generic-policy resolve-only.
- [ ] Java/Python no new regression; Codex `pass`. Pilot/annotation/WP1-B/WP2 HELD.
