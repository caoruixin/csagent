---
title: "Sprint 094 / S-Auto-40 — WP1-A: measurement contract → conditional outcome acceptance (two PRIMARY)"
doc_tier: current-runtime
status: current
implementation_status: not_started
source_of_truth: this file
last_reviewed: 2026-06-19
review_cadence: per sprint
supersedes: docs/sprints/sprint-093-objective.md
superseded_by: null
notes: >
  M-Auto-7 WP1-A only. Encodes the 2026-06-19 product-owner outcome-branch decision
  (escalation-after-genuine-help is a VALID terminal when the user remains unresolved;
  resolve when satisfied) as a DECLARATIVE, condition-bound conditional outcome
  acceptance for the two PRIMARY CaseSpecs — NOT an unconditional resolve-OR-escalate
  set. MEASUREMENT-FIRST: deliver verified the Sprint 093 bounded-run traces carry NO
  positive structured UNRESOLVED signal (13/13 escalations end bot_ended; no persisted
  per-turn goal_status; no user-state field) → every escalation is UNKNOWN, which must
  NOT auto-PASS. So Phase 1 instruments + persists a positive structured post-help
  user-state signal, Phase 2 produces decidable traces via a small bounded real-LLM
  run, Phase 3 implements the declarative three-state conditional acceptance validated
  by zero-LLM replay. NO bot/runtime/prompt change; WP1-B (prompt/reason) excluded.
  WP1-B, WP2, objective-alignment annotation, and the M-Auto-7 pilot stay HELD.
  exp-82 WITHDRAWN; canonical baseline pointer UNCHANGED.
---

# Sprint 094 / S-Auto-40 — WP1-A: measurement contract → conditional outcome acceptance

## Class

| Field | Value |
|---|---|
| **Layer (§3.2)** | Phase 1 `infra` (eval-framework simulator/trace emitter); Phase 3 `eval_spec` (declarative conditional outcome acceptance). |
| **§7 stanza** | **REQUIRED** (semantic-evaluation surface) — see §7. |
| **Per-sub-sprint Codex (§4.3/§4.1)** | **REQUIRED** — anti-hardcode kernel on the declarative conditional evaluator + the simulator instrumentation, reviewed against the Phase-3 zero-LLM blast-radius. |

## Goal

Encode the 2026-06-19 product decision — for a UC-A bad-case where the bot delivered
closure-qualified grounded help, **resolve** is correct if the user is satisfied and
**escalate** is correct if the user remains unresolved — as a **declarative,
condition-bound** outcome acceptance for the two PRIMARY cases, on a **positive
structured** user-state signal. **Not** an unconditional `resolve OR escalate` set.

### What this sprint does NOT do (binding)

- No bot/runtime change; no prompt-projection change; no escalation-reason-enum change;
  no handover-decision change (those are WP1-B, a separate fast-follow).
- No free-text/keyword heuristic and no case-id hardcode in the evaluator or signal.
- No unconditional `resolve OR escalate` accepted-outcome set.
- No CaseSpec change beyond the two PRIMARY; Tier-2 neighbors read-only.
- No canonical baseline pointer flip; no pilot resume; no objective-alignment annotation.
- No PRIMARY-majority-flip claim; no CS4-success claim.

## 0. Step 0 — blocking measurement gate (deliver pre-answered: INSUFFICIENT → measurement-first)

Three-state definition (canonical for this sprint):
- **SATISFIED** — reliably read from structured `goal_status=achieved` / `stop_reason=goal_achieved`.
- **UNRESOLVED** — requires a **positive, structured, post-grounded-help** user-state signal.
- **UNKNOWN** — no satisfaction signal **and** no positive unresolved signal.

**Forbidden as an UNRESOLVED proxy:** absence of `goal_achieved`; the bot chose handover;
the escalation reason; user free-text keywords.

**Verified verdict (deliver, read-only over the Sprint 093 bounded-run traces):
INSUFFICIENT.** 13/13 PRIMARY escalations end `bot_ended`; no persisted per-turn
`goal_status`; no `user_state`/satisfaction field anywhere in the trace → every
escalation draw is UNKNOWN. Therefore the declarative-now path is not viable; proceed
measurement-first. The dev re-confirms in Step 0; if (unexpectedly) a positive
UNRESOLVED signal already exists, Phase 1 may reduce to persistence-only.

## 1. Phase 1 — trace-only measurement contract (`infra`, eval-framework)

Persist a **positive structured post-help user-state signal**, sourced from the
simulator's **own per-turn generation** — provenance constraints (all binding):
- The state is produced **in the same simulator call that generates that user turn**
  and written to the trace immediately (priority: persist the structured state the
  simulator already produces in `generate_next`; if `goal_status` alone cannot yield a
  positive UNRESOLVED distinct from neutral/new-goal, enrich the simulator's
  **same-call** output with a structured `user_state` field — still one call).
- Each state carries `turn_id`, `signal_source`, and `schema_version`.
- The state must be **alignable to whether it occurred after the closure-qualified-help
  structural marker** (the grounded-help RESOLVE turn).
- **No** extra LLM call after `bot_ended` to infer state.
- **No** back-inference from containment outcome / handover decision / escalation reason /
  absence of satisfaction.
- **No** carry-forward of a pre-grounded-help state as a post-help state.
- Missing, or unalignable to the correct user turn → uniformly **`UNKNOWN`**.
- `goal_impossible` is **NOT** auto-mapped to `UNRESOLVED`; keep it separate (or map to
  `UNKNOWN`) until the product semantics are confirmed.

Trace-only + eval-only: no bot/runtime/prompt change. Unit tests + a dry check.

## 2. Phase 2 — bounded real-LLM validation (decidable traces)

Small bounded run (2 PRIMARY + controls/neighbors; §5.9 pre-flight GO first; `caffeinate`;
no pilot, no candidate search) to produce **decidable traces** carrying the new signal.

**Evidence floor (else `INCONCLUSIVE`):** ≥1 positive `SATISFIED`; **≥3 positive,
post-help `UNRESOLVED`**; `UNKNOWN`/neutral samples; the handover before/after state
timing represented; no outcome leakage (the user-state signal is not derived from the
bot's outcome/handover). One existing, stable **satisfiable control** may be added
**only** to validate the measurement contract — **not** to implement WP2 early. If
branch coverage is insufficient → mark **`INCONCLUSIVE`**; do **not** enter the formal
conditional re-bless and do **not** resume the pilot.

## 3. Phase 3 — declarative three-state conditional acceptance (`eval_spec`)

A **declarative** schema + a generic evaluator (no case-id hardcode, no free-text)
reading the Phase-1 structured signal, applied to **only**
`cs_uc_a_no_ad_id_ad_specific` + `cs_uc_a_loaded_listing`:

| user-state | bot terminal | verdict |
|---|---|---|
| `SATISFIED` | resolve | **PASS** |
| `SATISFIED` | escalate | **FAIL** |
| `UNRESOLVED` + structural grounded-help precondition | escalate | **`CONDITIONAL_ELIGIBLE / REVIEW_REQUIRED`** |
| `UNRESOLVED` | false resolve | **FAIL** |
| `UNKNOWN` / neutral / unrelated new-goal | escalate | **not auto-PASS** (never auto-accept) |

The user-state must be the **positive structured** Phase-1 signal — no free-text heuristic.

### 3.1 Closure-quality is not structural-marker-only

Tool calls + `source_ids` + a grounded answer prove only the **structural precondition**;
they do **not** prove the answer met the CaseSpec closure criterion. The evaluator
therefore emits `CONDITIONAL_ELIGIBLE / REVIEW_REQUIRED` for the escalate branch and
**never auto-flips a draw to PASS on the structural marker alone**.

### 3.2 Reproducible human closure adjudication (binding)

`CONDITIONAL_ELIGIBLE / REVIEW_REQUIRED` is **not** a final PASS in the automated
evaluator. Final escalation acceptance must reference a **versioned, auditable
adjudication artifact** keyed by trace, recording at least: trace ID/hash;
CaseSpec / closure-criterion version; reviewer verdict; rationale; timestamp. Missing
adjudication → stays `REVIEW_REQUIRED` (no auto-pass). The evaluator must **not** invoke
ad-hoc human judgment at runtime — it reads the recorded adjudication artifact only.

### 3.3 Anti-widen replay/tests (zero-LLM, over the new decidable traces) — must prove

None may enter the unresolved-accepted branch: grounded+`SATISFIED`+resolve → PASS ·
grounded+positive-`UNRESOLVED`+closure-qualified+escalate → `CONDITIONAL_ELIGIBLE`
(PASS only via §3.2 adjudication) · early/lazy escalation (no grounded help) → FAIL ·
escalation-after-satisfaction → FAIL · incomplete/non-grounded-then-escalation → FAIL ·
UC-misclass / STALL / empty-answer / false-resolved → FAIL · generic-policy control
resolve-only · **plus**: user-state unknown after grounded → not accepted · neutral user
reply → not accepted · user asks an unrelated new question without signaling the original
unresolved → not accepted · bot proposed handover but user has not responded → not
accepted · handover-caused missing goal_status → `UNKNOWN`, not accepted ·
satisfaction-then-escalation → FAIL. Evaluator change limited to declarative conditional
acceptance (no case-id hardcode, no free-text heuristic).

## 4. Baseline migration (blocking design item)

Old baseline traces lack the Phase-1 field → they yield only `UNKNOWN`; an old-trace
replay cannot rebuild a valid conditional baseline. The dev must specify + implement,
or STOP:
- missing field → `UNKNOWN`;
- the two PRIMARY get **new-instrumentation, decidable** baseline draws (Phase 2);
- how a complete, **loadable, provenance-bearing run-scoped baseline** is formed;
- whether the other (unaffected) cases reuse their old counts;
- if per-case replacement is needed, **prove** the loader/aggregation supports it
  **without mixed-semantics** contamination;
- if it cannot be safely combined → **STOP** and route to a formal re-bless;
- the **canonical pointer stays unchanged** (flip deferred to milestone close).

## 5. Scope fences

Two PRIMARY only (`cs_uc_a_no_ad_id_ad_specific`, `cs_uc_a_loaded_listing`). Tier-2
neighbors (`cs_uc_fp_loaded_moderation`, `cs_uc_a_lookup_failed`) **read-only
blast-radius review** — no conditional-acceptance expansion without per-case product
confirmation. Generic-policy negative control stays resolve-only.

## 6. STOP conditions

STOP + surface to the human if: no positive structured UNRESOLVED signal can be
produced same-call (Step 0 / Phase 1); the Phase-2 evidence floor is not met
(`INCONCLUSIVE`); the baseline cannot be safely combined without mixed-semantics (§4);
or the only way to determine a branch would require a free-text heuristic, a post-hoc
LLM inference, or back-inference from the outcome/handover/reason.

## 7. §7 Layer-classification + anti-hardcode stanza

**Target failure layer:** `infra` (Phase 1 eval-framework signal) + `eval_spec`
(Phase 3 declarative conditional acceptance).

**Tier-0 invariant:** Adds none. The §5.4 boundary holds — accepting escalation is a
product-owner-authorized correctness fix (escalation-after-genuine-help is correct
behavior), not a widening to mask a bot mistake; the anti-误杀 matrix (§3.3) keeps
early/lazy/over-escalation FAILing.

**Semantic hardcode:** None. The signal is the simulator's same-call structured state;
the acceptance is a declarative conditional read of structured fields. No keyword/regex/
enum/case-id/free-text.

**Generalization coverage:** target/neighbor/negative/shadow — 2 PRIMARY targets; 2
Tier-2 neighbors (read-only blast-radius); generic-policy + the §3.3 edge-case battery
as negative controls; shadow held-out.

## 8. Test / eval requirements

- Phase 1: simulator/trace unit tests (same-call provenance; turn_id/source/schema_version;
  post-help alignment; missing→UNKNOWN; no back-inference; no carry-forward) + a dry check.
- Phase 2: bounded real-LLM run (§5.9 GO) meeting the §2 evidence floor; else INCONCLUSIVE.
- Phase 3: zero-LLM replay over the new decidable traces proving the §3.3 matrix;
  declarative-evaluator unit tests (three-state routing + CONDITIONAL_ELIGIBLE +
  adjudication-artifact consumption).
- Java/Python suites: no new regression.
- §5.7: Phase-2 real-LLM produces the measured signal; Phase-3 conditional LOGIC is
  validated by deterministic replay over the recorded decidable traces.

## 9. Codex review plan (§4.3)

Per-sub-sprint Codex REQUIRED — anti-hardcode kernel on (a) the Phase-1 simulator
instrumentation and (b) the Phase-3 declarative conditional evaluator + the two PRIMARY
CaseSpec changes, reviewed against the §3.3 zero-LLM blast-radius. Verdict →
`docs/codex-findings.md` (§4.2 header). No pilot-resume / WP1-B decision until `pass`.

## 10. Handoff requirements

`docs/sprints/sprint-094-handoff.md` records: the Step-0 re-confirmation; the Phase-1
signal schema + provenance + unit tests; the Phase-2 bounded-run evidence-floor table
(SATISFIED / ≥3 UNRESOLVED / UNKNOWN-neutral / handover-timing / no-leakage; or
INCONCLUSIVE); the declarative schema + evaluator + the §3.3 replay matrix; the
adjudication-artifact format (§3.2); the baseline-migration decision (§4); the Codex
verdict; and an explicit restatement (WP1-A only; no bot/prompt/reason change; pilot +
annotation + WP1-B + WP2 HELD; exp-82 WITHDRAWN; canonical pointer unchanged).

## 11. Commit discipline

Stage explicitly by file (NO `git add -A`). Phase-1 instrumentation, Phase-3 evaluator,
and the CaseSpec changes are separable commits; bounded-run artifacts are gitignored
data. Run any real-LLM step on a clean committed tree.
