# Phase 3 Handoff — Eval Rubric Honesty + Trace-Minimum Gate (round 5)

Date: 2026-05-04
Branch: `design-v1-without-human-review`
Source review: `docs/codex-findings.md` (round 5, 2026-05-04)
Previous round handoff: commit `bb39363` on top of `2d3c0b2`.

This iteration closes the round-5 honesty bugs that the round-4 rubric
relaxation exposed: missing form turn in eval transcripts, raw
`acceptable_outcomes` accepting non-answer escalations, unconditional
secondary-UC credit, and risk-only `escalation_compliance`. No Java
runtime changes; the smoke pass count is flat at `4/14` because the
fixes raised the floor, not the ceiling — newly-exposed real failures
balance against the new degradations the framework now catches.

## 1. Current Status

- **Smoke pass rate: `4/14` (28.6%)** — same four cases as round 4
  (`cs_interactive_011` UC-D, `cs_interactive_036` UC-I,
  `cs_interactive_038` UC-J, `cs_interactive_040` UC-K). Mean composite
  `0.2394` vs round 4 `0.2442` (essentially flat). Mean outcome
  `0.7688` vs `0.7583`. Mean judge `0.7333` vs `0.7381`. Mean turns
  jumped to `2.9` from `1.9` — this is *expected*: we now correctly
  count the create-session form turn in `total_turns`.
- **The flat pass count is a quality improvement, not a regression.**
  Round 5 raised the floor (real stalls now flag, raw
  acceptable_outcomes can no longer pass non-answers, missing bot
  replies hard-fail) and the gains from round-4 demotions stay intact.
  Two cases that *would have* passed under a permissive
  acceptable_outcomes interpretation (cs_004, cs_259) correctly stay
  failing because the bot produced a non-answer escalation.
- The new instrumentation shows real conversations end-to-end. For
  example `cs_interactive_192`'s transcript now records the original
  form question ("I want to give away free items. Can I do this on
  your site?…") plus the bot's session-create answer, rather than
  starting at the follow-up "OK. What items are allowed?".
- Round-3 / round-4 gate adjustments preserved: `tool_sequence_match`
  diagnostic, `case_id_present` mandatory for UC-H/J/K, family-aware
  `escalation_compliance`.

## 2. Triage of `docs/codex-findings.md` (2026-05-04 round 5 review)

### 2.1 P0 / P1 / P2 correctness bugs

| # | Severity | Finding | Decision | Reason |
|---|---|---|---|---|
| F-1 | P0 | Session-create form turn + `bot_greeting` missing from transcripts (executor + L3 judge see incomplete conversation) | **fixed** | `SessionRunner.run_session` now prepends `turn_index=0` user (form description) + bot (greeting) entries when `form_context.description` is non-empty. Carries `source: form_context` / `source: session_create` markers so consumers can distinguish them. |
| F-2 | P0 | `acceptable_outcomes` accepting raw `resolve`/`escalate` lets non-answer escalations pass | **fixed** | `_check_correct_outcome` adds a cross-class quality guard: spec=resolve / actual=escalate requires handover summary ≥ 10 chars and a non-empty `escalation_reason`; spec=escalate / actual=resolve requires at least one source-cited turn. Otherwise scores `0.5` — clears the L2 gate threshold (1.0) only when the alternative path is genuinely useful. |
| F-3 | P1 | Round-4 handoff misdiagnoses `cs_interactive_029` (claimed expected `clarification_budget_exhausted`; actual spec is `user_requested`) | **fixed** | This handoff §5 row for cs_029 is corrected (spec = `user_requested`, actual `turn_budget_exhausted` = cross-family, L1 fail is correct). |
| F-4 | P1 | `_check_escalation_compliance` only hard-enforces missing escalation for `risk_level in {high, critical}` | **fixed** | New global L1 check `_check_required_escalation` complements `escalation_compliance`: when `should_escalate=true` AND `allow_bot_resolution="false"`, missing escalation hard-fails regardless of risk. Safe alongside `correct_outcome` relaxation. |
| F-5 | P1 | Tool spec YAML enum out of sync with eval schema (missing `runtime_error_threshold`) | **fixed** | Added to `customer_service_tool_spec_v0_2.yaml` line 458 with a comment pointing back to phase0 §0.6 deviation 2026-05-01 and codex round 5 §P1. |
| F-6 | P1 | Incomplete final turns / blank `containment_outcome` not hard-gated | **fixed** | New global L1 `_check_trace_minimum`: blank `containment_outcome` at terminal state hard-fails; non-empty `user_message` with empty `bot_response` and no handover tool call hard-fails. |
| F-7 | P2 | Comment in `hard_checks.py:487` contradicts implemented family map (says `intake_complete_for_uc_j` vs `trust_safety_required` is cross-family but the map treats them as `trust_safety` family) | **fixed** | Comment rewritten: same-family example now correctly notes that `intake_complete_for_uc_j` and `trust_safety_required` both live in `trust_safety` because both route to the same trust-and-safety queue. |

### 2.2 Weak rubric dimensions

| # | Finding | Decision | Reason |
|---|---|---|---|
| W-1 | `correct_uc` gives unconditional full credit for any secondary UC | **fixed** | Now requires drift evidence: full credit only when actual UC is a `secondary_ucs` member AND primary UC family is preserved in `candidate_use_cases`. Otherwise scales to `0.5` (below the mandatory L2 gate threshold of `1.0`). |
| W-2 | `correct_outcome` is not service-outcome aware (resolved_acceptably / partially_answered_then_escalated / escalated_with_useful_handover / failed_non_answer) | **deferred** | Rich service-outcome taxonomy is a multi-round schema redesign. Round-5 P0 quality guard is a partial mitigation: cross-class fallback now requires a quality minimum, so the most dangerous "raw escalate passes resolve-expected" path is closed. |
| W-3 | `policy_compliance_rate` looks perfect while service fails | **deferred** | Report-layer metric split. Tracked. |
| W-4 | `handover_completeness` too generic (does not check problem statement, identifiers, source/status checks) | **deferred** | Full rubric needs problem-extraction NLP. Tracked since round 4. |

### 2.3 Agent design problems

| # | Finding | Decision | Reason |
|---|---|---|---|
| A-1 | FAQ miss fallback too eager | **deferred** | Java runtime change. Codex round 4 §"Recommended Next Implementation" §7 explicitly endorsed leaving runtime alone for now. |
| A-2 | Session-create answer + follow-up handling not aligned | **partially mitigated** | Eval-side instrumentation now sees both turns, so L3 / L1 / L2 judge them as one conversation. The runtime split itself (auto-answer at session create per Phase 4 D14.6) is preserved. |
| A-3 | Reason precedence weak | **deferred** | Codex Fix D — multi-piece resolver, since round 1. |
| A-4 | Routing fragile across UC-A/B/C/D/E/F | **deferred** | Codex 4.4 — hard pre-LLM routing rules. |
| A-5 | Generic escalation copy on resolve-capable FAQs | **deferred** | LLM behaviour / KB content; codex Fix I says defer KB authoring. The new acceptable_outcomes guard penalizes this in scoring even without a runtime change. |

### 2.4 Tool-use risks / customer-service policy gaps

| # | Finding | Decision | Reason |
|---|---|---|---|
| T-1 | Trace and transcript are not the same artifact | **fixed** | F-1 above lands the eval-side fix. Trace-side server-issued tool calls are already in `trace_data.turns`. |
| T-2 | Eval did not hard-fail missing bot replies / blank containment | **fixed** | F-6 above. |
| T-3 | Relaxed tool sequence has no replacement evidence gate | **deferred** | F1 grounded_truth gate from round-4. Tracked. |
| T-4 | Enum drift YAML / schema / runtime | **fixed** | F-5 above. |
| T-5 | No tests added for `acceptable_outcomes`, secondary-UC, semantic family, transcript inclusion | **partially fixed** | New regression tests: 5 for `acceptable_outcomes` quality guard, 2 for secondary-UC drift evidence, 3 for `required_escalation`, 4 for `trace_minimum`. Family-test suite already in place (28 cases). |
| Policy gaps (free items, payment phrasing, email follow-up phrasing, refund/restoration commitments, business-hours) | **deferred** | PRD-owner decisions and KB-content work; codex Fix I keeps these deferred. |

### 2.5 Recommended minimal fixes (10 items)

| # | Recommendation | Decision | Notes |
|---|---|---|---|
| R-1 | Fix eval instrumentation first (form text + bot_greeting in transcript/results/L3 input + trace-minimum gate) | **fixed** | F-1 + F-6 |
| R-2 | Don't mark cs_259 as accepting escalation | **fixed** | No spec edit added; documented here. |
| R-3 | Replace raw `acceptable_outcomes` with service outcomes / gate alternatives with `answer_or_useful_handover` | **partial** | Quality guard implemented (F-2). Full taxonomy deferred (W-2). |
| R-4 | Add `required_escalation` L1 gate independent of risk | **fixed** | F-4 |
| R-5 | Replace unconditional secondary-UC full credit with `handled_issue_match` | **partial** | W-1 — preservation evidence required. Full transcript-driven drift detection deferred. |
| R-6 | Trace-minimum gate for missing bot replies / blank containment / mismatched turn counts | **partial** | F-6 covers blank containment + missing bot reply. Mismatched turn counts (transcript vs trace) deferred. |
| R-7 | Sync escalation reason enums across YAML / eval schema / runtime + automated test | **partial** | YAML aligned (F-5). Automated cross-source test deferred. |
| R-8 | Fix `cs_interactive_029` handoff diagnosis + reason-precedence test | **partial** | Handoff text fixed (F-3). Reason-precedence test deferred to a follow-up that exercises the resolver. |
| R-9 | Add targeted smoke cases (GDPR / appeal / T&S / payment FAQ / payment dispute / OOS / tool errors) | **deferred** | Spec curation. |
| R-10 | Add tests for every relaxation introduced in `HEAD~1..HEAD` | **partial** | 14 new test cases land this round (5 acceptable_outcomes, 2 secondary-UC, 3 required_escalation, 4 trace_minimum). Coverage-everywhere deferred. |

**Rejected:** none.

## 3. Files Changed

### 3.1 Eval (Python)

1. `eval_interactive/eval_interactive/simulator/session_runner.py`
   - After `create_session` returns, prepend `turn_index=0` user
     (form_context.description) + bot (bot_greeting) entries to
     `result.transcript`. Each entry carries `source: form_context` /
     `source: session_create` so downstream consumers (HTML report,
     L3 judge, regression tools) can distinguish create-session turns
     from simulator-driven turns. Skipped when the form has no
     description (topic_subject alone is metadata, not a user-visible
     question).

2. `eval_interactive/eval_interactive/scoring/outcome_checks.py`
   - `_check_correct_outcome` adds the cross-class quality guard:
     spec=resolve / actual=escalate requires `summary` ≥ 10 chars +
     non-empty `escalation_reason` in the handover payload (else
     `0.5`); spec=escalate / actual=resolve requires at least one
     source-cited turn (else `0.5`). Same-class match always full
     credit; legacy single-`outcome_class` path preserved when
     `acceptable_outcomes` is empty.
   - `_check_correct_uc` adds the secondary-UC preservation gate:
     full credit only when actual UC is in `secondary_ucs` AND
     primary UC family is in `candidate_use_cases`. Otherwise
     `0.5` (below mandatory L2 gate threshold).

3. `eval_interactive/eval_interactive/scoring/hard_checks.py`
   - New `_check_required_escalation` global L1 check: when
     `should_escalate=true` AND `allow_bot_resolution="false"`,
     hard-fail if outcome is not `escalated` and no `request_handover`
     tool call exists. Independent of `risk_level` so it complements
     rather than overlaps `escalation_compliance`.
   - New `_check_trace_minimum` global L1 check: hard-fail on
     blank `containment_outcome` at terminal state OR a turn with
     non-empty `user_message`, empty `bot_response`, and no
     `request_handover` tool call.
   - `ALL_CHECKS` extended with `required_escalation` and
     `trace_minimum`; `global_checks` set in `run_checks` extended
     to include both so per-case spec config can not silently drop
     them.
   - Misleading comment in `_check_escalation_compliance` rewritten
     to match the implemented family map (intake_complete_for_uc_j
     and trust_safety_required are now correctly described as
     same-family).

4. `eval_interactive/tests/test_hard_checks.py`
   - `TestRunChecksFiltering.GLOBAL_L1` set extended with the two
     new globals.
   - New `TestRequiredEscalation` (3 cases) and `TestTraceMinimum`
     (4 cases) classes.

5. `eval_interactive/tests/test_outcome_checks.py`
   - `_make_case_spec` accepts `acceptable_outcomes`.
   - New `TestCorrectOutcomeAcceptableOutcomes` (5 cases) and
     `TestCorrectUcSecondaryDriftEvidence` (2 cases) classes covering
     the round-5 quality guards.

### 3.2 Spec / docs

6. `docs/customer_service_tool_spec_v0_2.yaml` (line 458)
   - Added `runtime_error_threshold` to the `escalation_reason`
     enum so the YAML, eval schema, and runtime canonical-23 set
     are consistent. Cited phase0 §0.6 deviation 2026-05-01 and
     codex round 5 §P1.

7. `docs/10-handoff.md` (this file)
   - Rewritten for round 5. The cs_029 row in §5 is corrected (spec
     expects `user_requested`, not `clarification_budget_exhausted`).

### 3.3 Server (Java) — no changes

Codex round 4 §"Recommended Next Implementation" §7 ("Keep Runtime
Fixes Minimal") still applies. All round-3 runtime fixes preserved.

### 3.4 Tests

`280 passed` (266 baseline + 14 new). New test classes:
- `TestRequiredEscalation` — 3 cases covering the L1 gate
  independent of risk.
- `TestTraceMinimum` — 4 cases (blank containment, missing bot
  reply, handover-only carve-out, normal trace).
- `TestCorrectOutcomeAcceptableOutcomes` — 5 cases covering both
  cross-class fallback paths with and without quality evidence.
- `TestCorrectUcSecondaryDriftEvidence` — 2 cases covering
  preserved-vs-dropped primary-UC family.

## 4. Evals Run

`python -m eval_interactive run --set smoke --label
smoke-$(date +%Y%m%d-%H%M)` from `eval_interactive/`.

| Run | Label | Pass | Composite | Outcome | Judge | Esc-correct | Notes |
|---|---|---:|---:|---:|---:|---:|---|
| baseline (round 4 final) `20260504-071557` | smoke-20260504-1515 | 4/14 | 0.2442 | 0.7583 | 0.7381 | 64.3% | Round-4 family realignment + tool_sequence_match demoted. cs_011/036/038/040 pass. Stall rate 0%. |
| **round-5 final** `20260504-081853` | smoke-20260504-1618 | **4/14** | **0.2394** | **0.7688** | **0.7333** | **71.4%** | Form turn in transcript, acceptable_outcomes quality guard, required_escalation + trace_minimum gates, secondary-UC preservation. Same 4 cases pass. New stall_rate 7.1% on cs_015 (placeholder-without-followup correctly surfaced — round 4 missed it because turn 0 was hidden). New `L1:no_forbidden_tools` flag on cs_259 (bot mis-routed UC-J and called create_case_controlled — round 4 missed because of secondary-UC unconditional credit). |

The pass count is flat by design — the round-5 changes raise the
floor, not the ceiling. Codex round 5 explicitly noted that an over-
permissive `acceptable_outcomes` could turn cs_004 / cs_259 into
false-passes; the quality guard prevents that.

## 5. Failures Found (regenerated from `results/20260504-081853/results.json`)

Per-case state in the final round-5 run.

| Case | Expected | Observed UC / outcome / reason | Failure tags | Hypothesis |
|---|---|---|---|---|
| `cs_interactive_001` | UC-C escalate, `clarification_budget_exhausted` | UC-B escalated, `turn_budget_exhausted` | `L2_GATE:correct_uc`, `L2:correct_uc`, `L2:tool_sequence_match` | LLM mis-routed to UC-B. Both `turn_budget_exhausted` and `clarification_budget_exhausted` are `bot_limit` family so L1 escalation_compliance now passes (round-4 family map). The remaining gap is routing accuracy (codex 4.4 deferred). |
| `cs_interactive_002` | UC-C escalate, `user_distress` | UC-C escalated, `faq_miss_threshold_exceeded` | `L1:source_citation_present`, `L1:escalation_compliance`, `L3:relevance` | Reason cross-family (`bot_limit` vs `user_intent`). Substantive answer without source_ids fired the citation gate. Reason-precedence resolver (Fix D) would fix the L1. |
| `cs_interactive_004` | UC-D resolve | UC-E escalated, `user_requested` | `L2_GATE:correct_uc`, `L2_GATE:correct_outcome`, `L2:correct_outcome`, `L2:correct_uc`, `L2:tool_sequence_match`, `L3:relevance`, `L3:tone_appropriateness` | Routing landed on UC-E; over-escalation. Round 4 listed cs_004 as a candidate for `acceptable_outcomes: [resolve, escalate]` — but the round-5 quality guard would still reduce the over-escalation to `0.5` if the handover is short, which it is here. No spec edit yet. |
| `cs_interactive_011` | UC-D escalate, `user_distress` | UC-D escalated, `user_distress` | `L2:tool_sequence_match`, `L3:relevance`, `L3:tone_appropriateness` | **PASS** (composite 0.743). Tool sequence partial — diagnostic only. |
| `cs_interactive_014` | UC-C escalate, `user_distress` | UC-E escalated, `faq_miss_threshold_exceeded` | `L1:escalation_compliance`, `L2_GATE:correct_uc`, `L2:correct_uc` | Routed to UC-E; `bot_limit` family vs `user_intent`. Cross-family. Routing + reason-precedence. |
| `cs_interactive_015` | UC-FP resolve | UC-A resolved | `L1:no_stall`, `L2_GATE:correct_uc`, `L2:correct_uc`, `STALL:PLACEHOLDER_WITHOUT_FOLLOWUP` | **New visibility from round-5 transcript fix.** Round 4 hid the placeholder because turn 0 was missing. Bot's "I'm looking into this for you" greeting was followed by a clarifying question rather than the answer the user asked for ("how do I change it?"). Stall correctly surfaced. |
| `cs_interactive_029` | UC-C escalate, **`user_requested`** (not `clarification_budget_exhausted` as round-4 handoff stated) | UC-D escalated, `turn_budget_exhausted` | `L2_GATE:correct_uc`, `L2:tool_sequence_match` | **Round 4 misdiagnosed this.** Spec correctly expects `user_requested` (user_intent family); actual `turn_budget_exhausted` (bot_limit) is cross-family — the L1 fail in round 4 was correct, the handoff text was wrong. Reason-precedence resolver (Fix D) is the right fix. |
| `cs_interactive_036` | UC-I escalate, `payment_dispute_detected` | UC-I escalated, `intake_complete_for_uc_i` | `[]` | **PASS** (composite 0.864). |
| `cs_interactive_038` | UC-J escalate, `trust_safety_required` | UC-J escalated, `intake_complete_for_uc_j` | `L2:tool_sequence_match` | **PASS** (composite 0.892). |
| `cs_interactive_040` | UC-K escalate, `intake_complete_for_uc_k` | UC-K escalated, `intake_complete_for_uc_k` | `L2:tool_sequence_match` | **PASS** (composite 0.853). |
| `cs_interactive_066` | UC-E escalate, `clarification_budget_exhausted` | UC-K escalated, `intake_complete_for_uc_k` | `L1:no_forbidden_tools`, `L1:escalation_compliance`, `L2_GATE:correct_uc`, `L2:correct_uc`, `L2:tool_sequence_match` | Routed to UC-K; cross-family + `create_case_controlled` now firing L1:no_forbidden_tools (was masked by round-4 secondary-UC unconditional credit). |
| `cs_interactive_095` | UC-A resolve | UC-D escalated, `faq_miss_threshold_exceeded` | `L2_GATE:correct_outcome`, `L2:correct_outcome`, `L2:handover_completeness`, `L3:relevance`, `L3:tone_appropriateness` | Wrong UC + over-escalation with incomplete handover payload. Quality guard correctly keeps `correct_outcome` low. |
| `cs_interactive_192` | UC-B resolve | UC-B escalated, `faq_miss_threshold_exceeded` | `L2_GATE:correct_outcome`, `L2:correct_outcome`, `L2:tool_sequence_match`, `L3:relevance`, `L3:tone_appropriateness` | Correct UC; over-escalation on FAQ miss. Codex round 5 §P0 explicitly: "still a real hard failure if the bot lists allowed/prohibited item facts without source support" — preserved as fail. **Transcript instrumentation fix now shows the full conversation** including the original form question and the bot's session-create answer. |
| `cs_interactive_259` | UC-F resolve | UC-J escalated, `service_degraded` | `L1:no_forbidden_tools`, `L2_GATE:correct_uc`, `L2_GATE:correct_outcome`, `L2:correct_uc`, `L2:correct_outcome`, `L2:tool_sequence_match`, `L3:relevance`, `L3:tone_appropriateness` | **New visibility:** bot routed to UC-J and invoked `create_case_controlled` — both round-4 false-pass risks under the relaxed rubric. Codex round 5 §P0 explicitly required this case to fail until the agent answers the seller-payment question or produces a useful, issue-specific handover. Confirmed failing. |

### 5.1 Failure pattern summary (round 4 → round 5)

| Failure tag | Round 4 | Round 5 | Δ |
|---|---:|---:|---|
| `L1:escalation_compliance` | 4/10 | 2/10 | round-4 family map + cs_029 family overlap explanation cleaned up |
| `L1:source_citation_present` | 0/10 | 1/10 | now firing on cs_002 substantive uncited answer (transcript fix exposed the answer turn) |
| `L1:no_forbidden_tools` | 0/10 | 2/10 | cs_066 / cs_259 — bot called `create_case_controlled` outside UC-H/J/K; round-4 secondary-UC credit was masking |
| `L1:no_stall` (with `STALL:PLACEHOLDER_WITHOUT_FOLLOWUP`) | 0/10 | 1/10 | cs_015 placeholder uncovered by transcript fix |
| `L2:correct_uc` | 6/10 | 7/10 | secondary-UC drift evidence gate downgraded cs_029 / cs_066 from "credit" to fail |
| `L2:correct_outcome` | 5/10 | 5/10 | quality guard not triggered yet because no spec lists `acceptable_outcomes` |
| `L2_GATE:tool_sequence_match` | 0/10 | 0/10 | gate removed in round 4 |
| `L2:handover_completeness` | 1/10 | 1/10 | unchanged |
| `STALL` / `ERROR` / `CONTRACT_VIOLATION` | 0/10 | 1/10 | cs_015 stall (real, surfaced by transcript fix) |

## 6. Next Recommended Actions

In priority order:

1. **Land the deterministic `EscalationReasonResolver` (codex Fix D).**
   The remaining `L1:escalation_compliance` failures (cs_002, cs_014)
   plus the cs_029 cross-family case all reduce to LLM-side reason
   picks the server should override. Round-3 handoff §6 specifies
   inputs / outputs / precedence. Expected lift: 5–6/14.

2. **Hard pre-LLM routing rules for account-login / explicit-human-
   request signals (codex 4.4).** Account/login + topic
   `Account Support` should resolve UC-D pre-LLM; phrases like "talk
   to someone" / "call me" should set `user_requested_escalation`
   deterministically. Targets cs_001 / cs_002 / cs_014 / cs_015 /
   cs_029 / cs_066 routing accuracy.

3. **Patch over-escalation specs with `acceptable_outcomes`** *only
   for cases where codex round 5 explicitly judges the alternative
   acceptable*. Concretely: skip cs_259 (codex P0 explicitly says no)
   and cs_192 (codex round 4 §"Reinterpreting": real hard failure).
   cs_004 / cs_095 / cs_066 are over-escalation but with weak
   handovers — the round-5 quality guard would score them 0.5 even
   with the spec edit, so the edit alone doesn't unblock them.

4. **Fix the `cs_interactive_015` runtime placeholder-without-followup
   bug.** The bot says "I'm looking into this for you" then asks a
   clarifying question instead of returning the lookup result. The
   stall detector now correctly catches this (round 5). Either the
   `progress_placeholder` runtime capability is firing without a
   subsequent answer turn, or the runtime is dropping the lookup
   result. Targeted log inspection on session
   `<from results 20260504-081853 cs_015 session_id>` should
   pinpoint which.

5. **Sync escalation reason enums end-to-end with an automated
   cross-source test.** Round 5 fixed YAML alignment; the test that
   walks the YAML, eval schema, and runtime constant set is still
   missing.

6. **Implement codex round 4 §3 `grounded_truth` claim classifier
   (F1 / C2 from round 4).** Most strategic gate per codex's
   philosophy. Initial regex+heuristic build; LLM classifier is
   future work.

7. **Round-trip the `acceptable_outcomes` and `bot_handling_pattern`
   fields into the case-spec extractor (`extractor.py`)** so future
   smoke regenerations carry both correctly. Otherwise C10 will
   keep blocking.

8. **Add the deferred service-outcome taxonomy** (resolved_acceptably
   / partially_answered_then_escalated / escalated_with_useful_handover
   / failed_non_answer). The round-5 cross-class quality guard is a
   stop-gap; the taxonomy is the principled solution.

When 1–2 land, expect smoke pass rate to move into the **6–7 / 14**
band. 3–4 are needed for 8–9/14. Items 5–8 are infrastructure for
the next ladder.
