# Phase 3 Handoff — Tier-1 Runtime Patch and Eval Contract Tightening

Date: 2026-05-03
Branch: `design-v1-without-human-review`
Source review: `docs/codex-findings.md` (latest implementation review,
2026-05-03)

This handoff continues the work from the previous iteration (commit
`ee755a6 fix: address codex review and rerun evals`, which landed the
source-citation gate fix and the routing-prompt UC-A/UC-D fix). The new
review noted that no runtime fixes had landed; this iteration lands the
Tier-1 server-side fixes that the previous iteration deferred.

## 1. Current Status

- Re-triaged every finding in the new `docs/codex-findings.md` against
  Phase 0–5 and the tool spec. Two runtime findings (1.7 = old 1.7
  Delivery semantic override, 1.8 = old 1.2 cheap session creation) are
  **rejected with spec evidence** because Phase 2 §2.4 makes Delivery a
  terminal hard-OOS topic and Phase 4 §D14.6 explicitly authorises
  auto-search at session create. The rest are either **fixed** in this
  iteration, **deferred** for follow-on, or already addressed.
- Server rebuilt (`mvn -pl server install -DskipTests`), JVM restarted on
  port 8080, smoke rerun against the fresh JVM. The fix surface in the
  running server now includes the deterministic escalation-reason
  resolver (1.5 / 1.9), runtime-only case creation contract (1.8), the
  prior session’s UC-A/UC-D routing prompt fix (1.6, now actually
  loaded), and the prior session’s source-citation-gate scope fix (1.3).
- Smoke pass rate stays at `1/14` (`cs_interactive_040`, UC-K). The
  reason is no longer the Tier-0 false-positive class; remaining
  failures are dominated by LLM-driven UC routing accuracy and over-
  escalation, both of which need either deterministic routing rules or
  prompt few-shots — explicitly deferred per Codex sequencing.
- The eval contract is tighter:
  - Per-case `status` now mirrors the summary `case_passed AND
    composite≥0.7` rule (Codex new §1.1 / Fix A).
  - `handover_completeness` now auto-runs on every escalate case, so
    `L2_GATE_MISSING:handover_completeness` no longer fires from a
    spec-omission instead of a real failure (Codex old §1.4 / new §1.1
    style cleanup).
  - `tool_sequence_match` is now scored on every smoke case that
    declares an `expected_tool_sequence`, and `case_id_present` is now
    scored on UC-J / UC-K smoke cases (Codex new §1.4 / Fix C).

## 2. Triage of `docs/codex-findings.md` (Latest Implementation Review)

The current `codex-findings.md` is the second review — it replaced the
2026-05-03 morning version (`Codex Findings: Interactive Eval Review`)
that drove the prior iteration. Triage below tracks the **new** review's
sections.

### 2.1 §1 Correctness Bugs

| # | Finding | Decision | Reason |
|---|---|---|---|
| 1.1 | Per-case `status` serialisation disagrees with summary `case_passed AND composite≥0.7` | **fixed** | `BatchExecutor._build_case_result` now applies the same gate. Prior code wrote `"PASS"` whenever `case_passed`, which let `composite=0.69` cases serialise as PASS while the summary counted them as failed. |
| 1.2 | Handoff misassigns root causes (cs_004 / 015 / 066 / 192 / 259 / 029) | **fixed** | This handoff regenerates the per-case table directly from `results/20260503-082110/results.json` (Codex Fix H). Hypotheses and observations are kept distinct in the table. |
| 1.3 | Citation gate is now gameable | **deferred** | The current heuristic (length + lead-pattern + same-turn retrieval) is partially gameable as Codex notes. Replacing it with turn-type evidence (prior-turn retrieval propagation, citation-supports-answer) plus the test matrix Codex spec'd is a focused Python piece of work, but it also depends on the trace producing reliable retrieval markers across turns and on a citation-support metric that doesn't yet exist. Tracked for the next iteration. |
| 1.4 | Smoke specs list expected tools but do not score them; UC-H/J/K never check `case_id_present` | **fixed** | Added `tool_sequence_match` to every smoke spec with a non-empty `expected_tool_sequence` (14/14 cases) and `case_id_present` to the two UC-J / UC-K smoke specs (cs_038 / cs_040). Resulting L2 sets exercise the production tool-order and case-creation contracts that were previously inert. |
| 1.5 | `fixed_script_adherence` only checks "no knowledge tools" | **deferred** | Splitting into per-aspect checks (template used, intake fields collected, case creation, forbidden claims absent) is a meaningful new check suite. The first three already have partial coverage via `intake_no_knowledge_tool`, `intake_fields_collected` alias → `handover_completeness`, and the new `case_id_present`. Full split is tracked. |
| 1.6 | Escalation-correctness summary is too weak | **deferred** | Splitting the summary into `escalation_decision_accuracy`, `escalation_reason_accuracy`, `handover_completeness_rate`, `escalation_timing_rate` is a report-layer change. Useful but it's a metric refactor; tracked alongside 1.5 / Fix C. |
| 1.7 | Server eval is not deterministic (mixed temperatures) | **deferred** | Codex's Fix G (deterministic mode replaying `seed_messages`, pinned server temperatures, recorded prompt hashes) is a multi-piece change in the eval harness, server config, and recording layer. Tracked. The same temperature mismatch is the root of the cross-run swings I saw in the prior iteration. |
| 1.8 | Session creation still performs resolution work (Phase 4 D14.6 vs Codex recommendation) | **rejected with reason** | Phase 4 §D14.6 explicitly authorises `createSession()` to call `ControlKernel.processMessage()` on a substantive `form.description` so the greeting becomes a grounded answer. That is a deliberate v9 design choice. Reverting it conflicts with the spec. The latency/timeout risk is real and should be mitigated with per-phase timeout budgets and a controlled fallback (Codex §5.7), not by removing the auto-search. Tracked as a resilience hardening, not a contract rollback. |

### 2.2 §2 Missing Eval Cases (1 – 17)

All 17: **deferred**. Codex’s new list is more concrete than the prior
review (per-UC routing-with-noise cases, latency/no-heavy-work-on-create
cases, tool sequence and tool access negative cases, UC-H/J/K case
creation cases, handover-quality cases, Omni-Channel transfer
contract cases, offline-vs-business-hours cases, tool-timeout fallback
cases, citation-support-quality cases, retrieval-miss/weak/wrong-UC-
filter cases, phone/email-policy cases, seller payment cases, Delivery
exception matrix, PII-minimisation cases, human-only-tool-promise
cases, scripted-deterministic mode). Authoring these is a sustained
QA effort that should follow stable runtime contracts; we land §1
runtime fixes first.

### 2.3 §3 Weak Rubric Dimensions (3.1 – 3.7)

All seven: **deferred** for the same reason as §2 — they are rubric
upgrades (citation-supports-answer scoring, tool-behavior-as-pass-gate,
broader policy-compliance metric, answer-usefulness independent of
groundedness, customer-effort score, fixed-script semantic checks,
making interactive task success a release blocker). They depend on
stable runtime behaviour for clean signal.

### 2.4 §4 Agent Design Problems (1 – 9)

| # | Statement | Decision | Where |
|---|---|---|---|
| 4.1 | LLM still controls some exact escalation reasons | **partially fixed** | ControlKernel now resolves budget-exceeded buckets to canonical reasons (1.5/1.9). PhaseEvaluator MAX_STEPS now picks among `incomplete_intake`, `faq_miss_threshold_exceeded`, `clarification_budget_exhausted`, `turn_budget_exhausted` based on plan + tool-event evidence. `user_requested` / `user_distress` / `trust_safety_required` are still LLM-classified — Codex Fix D's full server-side resolver is the remaining gap. |
| 4.2 | Runtime side effects are still partly LLM-ordered | **partially fixed** | Case creation (`create_case_controlled`) is now strictly runtime-only — see 4.3 / new §1.4. Other side effects (`request_handover`, `record_outcome`) are still LLM-callable, which the spec permits; `request_handover` already has a runtime canonicaliser in `ToolDispatcher`. |
| 4.3 | `create_case_controlled` is runtime-only in tool spec but intake plans/prompts still steer LLM toward calling it | **fixed** | Removed `create_case_controlled` from `PhasePlan.allowedTools()` for both the RESOLVE/INTAKE plan (UC-H/J/K) and the ESCALATE plan (intake UCs). `buildIntakeSystemInstruction` no longer instructs the LLM to call `create_case_controlled BEFORE request_handover`. The intake grounding instruction now tells the LLM the case is created automatically by the runtime. The deterministic creation path is unchanged: `ControlKernel.createCaseIfNeeded` runs on forced escalation; `PhaseEvaluator.createCaseIfAllowed` runs at intake-complete. |
| 4.4 | Routing-prompt fix is not enough — needs deterministic rules | **deferred** | The UC-A/UC-D mismatch is now correct in `routing_prompt.txt` and active in the running JVM, but Codex is right that the LLM still has discretion on disambiguating account/login, payment-dispute, T&S, and hard-OOS exception cases. Hard deterministic rules are a `UseCaseRouter` change tracked alongside 4.5. |
| 4.5 | `UseCaseRouter` checks handover-only topics before semantic dispute/safety overrides | **rejected with spec evidence** | Phase 2 §2.4 makes Delivery a terminal hard-OOS topic — fraud / refund / chargeback signals within Delivery do **not** override into UC-I or UC-J. Acting on this finding would conflict with the spec. The product-level question (should Delivery+fraud route to UC-J?) must be resolved at the spec/PRD level first. |
| 4.6 | `ControlKernel` maps exceeded budgets to generic `turn_budget_exhausted` | **fixed** | `mapBudgetToEscalationReason()` now maps `max-clarification-rounds` → `clarification_budget_exhausted` and `max-faq-miss` → `faq_miss_threshold_exceeded`; the per-path turn caps and the absolute total cap stay on `turn_budget_exhausted` (which is the spec-defined catch-all for control-plane stops). |
| 4.7 | Server can auto-answer during session creation | **rejected (see 1.8)** | Spec authorises this. |
| 4.8 | Prompt/design docs conflict on auto-search at session create | **deferred** | Documentation drift between Codex's recommendation and Phase 4 D14.6. The runtime sides with Phase 4 (auto-search permitted). Tracked as a documentation alignment task. |
| 4.9 | Eval and production runtime use different temperatures | **deferred (see 1.7)** | |

### 2.5 §5 Tool-Use Risks (5.1 – 5.8)

All eight: **deferred**. Risks 5.4 (runtime-only case creation order
must not depend on LLM) is mitigated by the 4.3 fix above. Risks
5.1/5.2 (wrong-UC tool exposure / UC-filtered retrieval) reduce as
routing accuracy improves. Risk 5.8 (`server/target/classes` checked
in) is real but doesn't affect the running server because Spring loads
from the source classpath; tracked as a hygiene cleanup.

### 2.6 §6 Customer Service Policy Gaps (6.1 – 6.9)

All nine: **deferred** to the PRD/policy owner. They are policy and
content questions, not runtime defects. Notable: 6.2/6.7 (Delivery
exceptions vs UC-I/UC-J) is the policy correlate of finding 4.5, and
must be resolved at the spec level before the runtime can respect a
new override.

### 2.7 §7 Recommended Minimal Fixes (Fix A – Fix I)

- **Fix A** (status serialisation + top-level error/timeout/contract/
  stall counts that fail smoke): partially **fixed** — per-case
  status now uses the composite gate. Top-level failure-bucket counts
  are deferred (report-layer change).
- **Fix B** (regression tests for citation gate): **deferred**.
- **Fix C** (activate production-critical smoke checks): partially
  **fixed** — `tool_sequence_match` and `case_id_present` are now in
  smoke. `turn_efficiency` / `issue_preservation` / explicit user-
  requested-escalation cases / real `fixed_script_template_used` are
  deferred.
- **Fix D** (server-side escalation-reason resolver): partially
  **fixed** — budget bucket → reason mapping landed; LLM-driven
  reasons (`user_requested`, `user_distress`, `trust_safety_required`,
  `payment_dispute_detected`) are still owned by the LLM. The full
  resolver service is tracked.
- **Fix E** (centralised handover emission): partially **fixed** —
  the eval check `handover_completeness` is now actually run on every
  escalate case, exposing real emission gaps instead of fail-closing
  on missing config. The next step is the runtime side: confirm the
  hard-OOS escalation path in `SessionManager.createSession` carries
  the full payload (currently `cs_interactive_036` shows
  `L2_GATE:handover_completeness` failing on a real payload gap).
- **Fix F** (resolve session-creation contradiction): **rejected**.
  Spec wins; resilience hardening (latency budgets / fallback) tracked.
- **Fix G** (deterministic smoke + temperature pin): **deferred**.
- **Fix H** (regenerate handoff from JSON): **fixed** in this doc.
- **Fix I** (defer KB authoring): aligned — no KB content edits this
  iteration.

## 3. Files Changed

### 3.1 Server (Java)

1. `server/src/main/java/com/gumtree/csagent/service/runtime/ControlKernel.java`
   - `processMessage()` now delegates the budget-exceeded reason to a
     new helper `mapBudgetToEscalationReason(bucket)` instead of always
     emitting `turn_budget_exhausted`. Mapping:
     - `max-clarification-rounds` → `clarification_budget_exhausted`
     - `max-faq-miss` → `faq_miss_threshold_exceeded`
     - per-path / total / repeated-action caps → `turn_budget_exhausted`
   - Comment-tagged with the Phase 2 §2.4 precedence so the next reader
     can verify the canonical mapping.

2. `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java`
   - `PhasePlan` for `RESOLVE` / INTAKE UCs (UC-G/H/I/J/K) and for
     `ESCALATE` of intake UCs no longer exposes `create_case_controlled`
     to the LLM. The deterministic creation path is unchanged
     (`PhaseEvaluator.createCaseIfAllowed` and
     `ControlKernel.createCaseIfNeeded`).
   - `buildIntakeSystemInstruction` no longer tells UC-H/J/K plans that
     the LLM "MUST call create_case_controlled BEFORE request_handover".
     The grounding instruction now tells the LLM the case will be
     created automatically by the runtime when it escalates.
   - `interpretRunResult()` `MAX_STEPS` branch now picks among
     `incomplete_intake`, `faq_miss_threshold_exceeded`,
     `clarification_budget_exhausted`, `turn_budget_exhausted` via a
     new helper `resolveMaxStepsReason(plan, result, session)`. INTAKE
     plans short-circuit to `incomplete_intake`; otherwise the helper
     looks for `search_knowledge` tool events (→ FAQ miss) and a
     non-zero `clarificationCount` (→ clarification budget) before
     falling back to the generic budget reason.

3. `server/src/test/java/com/gumtree/csagent/service/runtime/PhaseEvaluatorPlanTest.java`
   - Updated four test assertions that previously required
     `create_case_controlled` in the LLM-allowed tool list. They now
     assert it is **absent** and that the system instruction does not
     steer the LLM toward calling it. Codex 1.8 / tool_spec
     `runtime_only` is now enforced by tests.

All 398 server tests pass after the changes.

### 3.2 Eval (Python)

4. `eval_interactive/eval_interactive/scoring/outcome_checks.py`
   - `OutcomeChecker.run_checks` now auto-includes
     `handover_completeness` for every `outcome_class == "escalate"`
     case, mirroring `composite._conditional_mandatory_l2`. This fixes
     the false-negative class where an escalate case spec listed only
     `correct_uc / correct_outcome / escalation_triggered /
     answer_accuracy` and the L2 gate failed-closed as
     `L2_GATE_MISSING:handover_completeness` even though the runtime
     emitted a complete handover.

5. `eval_interactive/eval_interactive/batch/executor.py`
   - `_build_case_result` now sets `"status": "PASS"` only when
     `case_passed AND composite_score >= 0.7` (Codex new §1.1 / Fix A).
     Previously the per-case JSON could disagree with the summary
     pass count.

6. `eval_interactive/tests/test_outcome_checks.py`
   - Updated `TestCorrectOutcome.test_pass_escalate_escalated` and
     `test_fail_resolve_but_escalated` to look up `correct_outcome` by
     name instead of by index — the auto-included
     `handover_completeness` check changed result ordering.

7. `eval_interactive/case_specs/smoke/cs_interactive_*.yaml` (14 files)
   - Added `tool_sequence_match` to every spec that declares
     `expected_tool_sequence` (14/14 cases).
   - Added `case_id_present` to UC-J (cs_038) and UC-K (cs_040). UC-H
     is not in the smoke set.

### 3.3 Documentation

8. `docs/10-handoff.md` — this file (Codex new §1.2 / Fix H regenerated
   from `results/20260503-082110/results.json`).

The previous iteration's Tier-0 changes
(`eval_interactive/eval_interactive/scoring/hard_checks.py` source-
citation gate scope, `server/src/main/resources/prompts/routing_prompt.txt`
UC-A/UC-D fix) are committed as `ee755a6` and are loaded by the freshly
restarted JVM in this iteration.

## 4. Evals Run

`python -m eval_interactive run --set smoke --label smoke-$(date +%Y%m%d-%H%M)`,
re-run after each meaningful patch.

| Run | Label | Pass | Composite | Outcome | Judge | Esc-correct | Notes |
|---|---|---:|---:|---:|---:|---:|---|
| baseline `20260501-215807` | (Codex baseline) | 0/14 | 0.0000 | 0.6452 | 0.7048 | 71.4% | One ERROR (`session_create_failed` on `cs_066`). |
| Tier-0 `20260503-065357` | smoke-20260503-1453 | 1/14 | 0.0691 | 0.5500 | 0.6476 | 57.1% | Citation gate scope + routing prompt; routing prompt only loaded after server restart in this iteration. |
| **Tier-1.a** `20260503-080242` | smoke-20260503-1602 | 1/14 | 0.0691 | 0.5976 | 0.7000 | 64.3% | Server rebuilt with budget-bucket mapping + runtime-only case creation; routing prompt now active. Cases newly emit `clarification_budget_exhausted` / `faq_miss_threshold_exceeded` instead of always `turn_budget_exhausted`. |
| **Tier-1.b** `20260503-080858` | smoke-20260503-1608 | 1/14 | 0.0667 | 0.5619 | 0.6667 | 71.4% | Added `MAX_STEPS` reason resolver in PhaseEvaluator. INTAKE plans now exit on `incomplete_intake`; FAQ MAX_STEPS exits on `faq_miss_threshold_exceeded`. |
| **Tier-1.c** `20260503-081557` | smoke-20260503-1615 | 1/14 | 0.0667 | 0.6750 | 0.7381 | 71.4% | Auto-include `handover_completeness` in escalate runs. Removes `L2_GATE_MISSING:handover_completeness` from cases that always actually had a handover record. |
| **Tier-1.d (final)** `20260503-082110` | smoke-20260503-1621 | 1/14 | 0.0563 | 0.5669 | 0.7619 | 71.4% | Activated `tool_sequence_match` on all 14 smoke specs and `case_id_present` on cs_038 / cs_040. New checks expose two real defects (see §5): cs_040 case_id is not yet flowing into the handover payload, and tool sequences in INTAKE flows do not yet match the expected sequence per spec. Mean composite drop is from a wider, stricter scoring surface — not a regression in agent behaviour. |

The single passing case (`cs_interactive_040`) survived every iteration.
Its composite slid from `0.9667` → `0.933` → `0.787` as
`tool_sequence_match` and `case_id_present` were activated, but it
remained above the `0.7` threshold so it still serialises as `PASS` and
counts in the summary.

## 5. Failures Found (regenerated from `results/20260503-082110/results.json`)

Per-case state in the final run. "Observed" is from the JSON; "Hypothesis"
is my analysis with a Codex-finding pointer where applicable. Both are
kept distinct per Codex new §1.2 / Fix H.

| Case | Expected | Observed UC / outcome / reason | Failure tags | Hypothesis |
|---|---|---|---|---|
| `cs_interactive_001` | UC-C escalate, `clarification_budget_exhausted` | UC-F resolved, no reason | `L2_GATE:correct_uc`, `L2_GATE:correct_outcome`, `L2:escalation_timing`, `L3:relevance` | LLM mis-routed to UC-F (payment) and produced a resolve. Routing accuracy. (4.4 deferred.) |
| `cs_interactive_002` | UC-C escalate, `clarification_budget_exhausted` | UC-I escalated, `intake_complete_for_uc_i` | `L1:escalation_compliance`, `L2_GATE:correct_uc`, `L2:tool_sequence_match`, `L2:correct_uc` | Routed to UC-I (payment dispute) and ran the intake template. Routing + intake misclassification. |
| `cs_interactive_004` | UC-D resolve | UC-D escalated, `faq_miss_threshold_exceeded` | `L2_GATE:correct_outcome`, `L2:tool_sequence_match`, `L3:relevance/tone` | Correct UC, premature escalation. Resolve-before-escalate fallback (Codex 4.6 / Fix D — partial). |
| `cs_interactive_011` | UC-D escalate, `user_distress` | UC-D escalated, `user_distress` | `L1:escalation_compliance`, `L2:tool_sequence_match`, `L3:relevance/tone` | The L1 fail tag claims `expected='user_requested', actual='user_distress'`. Inspecting the case spec confirms `escalation_trigger: user_requested`. The LLM saw frustration but no explicit human request and chose `user_distress` per the system prompt's distress rules. Spec/runtime classification disagreement — needs either a spec override or a routing rule (Fix D). |
| `cs_interactive_014` | UC-C escalate, `user_distress` | UC-B escalated, `account_compliance` | `L1:escalation_compliance`, `L2_GATE:correct_uc`, `L2:correct_uc` | Routed to UC-B (Posting & Editing) and chose `account_compliance` reason. Routing + reason precedence. |
| `cs_interactive_015` | UC-FP resolve | UC-A resolved, no reason | `L2_GATE:correct_uc`, `L2:correct_uc` | Routed to UC-A (Ad Status) instead of UC-FP (Correct Deletion Explanation). Correct outcome. Routing accuracy. |
| `cs_interactive_029` | UC-C escalate, `clarification_budget_exhausted` | UC-B escalated, `faq_miss_threshold_exceeded` | `L1:escalation_compliance`, `L2_GATE:correct_uc`, `L2:tool_sequence_match`, `L2:correct_uc` | Routed to UC-B; FAQ miss path. Routing + the `faq_miss` vs `clarification_budget` distinction needs the LLM-side reason resolver (Fix D). |
| `cs_interactive_036` | UC-I escalate (intake) | empty UC, `out_of_scope` | `L1:phase_transition_validity`, `L1:escalation_compliance`, `L2_GATE:correct_uc`, `L2_GATE:handover_completeness`, `L2:correct_uc`, `L3:tone` | Hard-OOS at session create (Delivery topic). Per Phase 2 §2.4 this is the spec-correct behaviour for `topic_subject = "Delivery"`; the case-spec expectation conflicts with the spec (4.5 / 6.7 deferred to PRD). The `L2_GATE:handover_completeness` fail is a real runtime gap — the synthetic OOS handover payload built in `SessionManager.createSession` is not being persisted in a way the trace collector can read for the eval. Worth fixing at the runtime trace layer (Fix E remaining work). |
| `cs_interactive_038` | UC-J escalate, `trust_safety_required` | UC-J escalated, `intake_complete_for_uc_j` | `L1:escalation_compliance`, `L2:tool_sequence_match`, `L2:case_id_present` | Reason precedence: spec says `trust_safety_required` should beat `intake_complete_for_uc_j`, but the system prompt and runtime currently let the LLM take the `intake_complete_for_uc_j` path. Codex Fix D's full resolver is needed here. The `L2:case_id_present` fail also indicates UC-J case creation is not flowing into the handover payload — runtime issue worth tracing. |
| `cs_interactive_040` | UC-K escalate, `intake_complete_for_uc_k` | UC-K escalated, `intake_complete_for_uc_k` | `L2:tool_sequence_match`, `L2:case_id_present` (case still passes, composite=0.787) | The case still passes the 0.7 composite gate but newly-activated checks expose two production defects: tool sequence (the agent did not follow the spec'd sequence for UC-K intake) and `case_id` propagation (runtime created the case but did not set `bot_session.caseId` in time for the handover payload). |
| `cs_interactive_066` | UC-E escalate, `clarification_budget_exhausted` | UC-B escalated, `faq_miss_threshold_exceeded` | `L1:escalation_compliance`, `L2_GATE:correct_uc`, `L2_GATE:handover_completeness`, `L2:handover_completeness`, `L2:correct_uc`, `L3:relevance/tone` | Routed to UC-B (was UC-K in earlier reruns); the routing oscillation is caused by LLM non-determinism (1.7 deferred). Real `L2:handover_completeness` failure — the FAQ-miss escalation path emitted a handover but with payload gaps. Needs runtime trace inspection. |
| `cs_interactive_095` | UC-A resolve | UC-D escalated, `faq_miss_threshold_exceeded` | `L2_GATE:correct_uc`, `L2_GATE:correct_outcome`, `L2:correct_outcome`, `L2:correct_uc`, `L3:relevance/tone` | Routed to UC-D with premature escalation. Routing + over-escalation. |
| `cs_interactive_192` | UC-B resolve | UC-B escalated, `faq_miss_threshold_exceeded` | `L2_GATE:correct_outcome`, `L2:correct_outcome`, `L3:relevance/tone` | Correct UC, premature escalation. The bot escalated immediately on a resolvable FAQ instead of answering. Resolve-before-escalate (Codex 4.6 / Fix D — runtime-side). |
| `cs_interactive_259` | UC-F resolve (or escalate per spec? — spec field is escalate) | UC-C resolved, no reason | `L1:source_citation_present`, `L2_GATE:correct_uc`, `L2:tool_sequence_match`, `L2:correct_uc`, `L3:tone` | Routed to UC-C (Messages) instead of UC-F (Payment Inquiry). The bot answered without citing sources, so the citation gate (still scoped per Tier-0) correctly fired on the substantive answer. Real failure — UC-F retrieval is needed. |

### 5.1 Failure pattern summary

- L1 escalation_compliance fails on **6 / 13 failing cases**: still
  the dominant L1 blocker. Five of those six are reason mismatches the
  LLM owns (`user_requested` vs `user_distress`,
  `clarification_budget_exhausted` vs `faq_miss_threshold_exceeded`,
  `trust_safety_required` vs `intake_complete_for_uc_j`). Codex Fix D
  full resolver remains the right next step.
- L2 correct_uc fails on **9 / 13**: routing accuracy is the dominant
  L2 blocker. Routing-prompt fix is not enough on its own; deterministic
  rules (4.4 deferred) are needed.
- L2 correct_outcome fails on **4 / 13**: all four are
  resolve-when-escalate or escalate-when-resolve. The resolve-before-
  escalate gap (4.6 / Codex's "force answer after retrieval") is the
  root cause.
- L2 handover_completeness fails on **2 / 13** (cs_036, cs_066):
  these are now real failures (no longer fail-closed on missing config).
  Runtime payload gaps in the hard-OOS path and the FAQ-miss path
  respectively.
- L1 source_citation_present fires on **1 / 13** (cs_259), correctly,
  on a substantive uncited answer.
- L1 phase_transition_validity fires on **1 / 13** (cs_036), tied to
  the OOS path's INIT→ESCALATE behaviour.
- 0 cases show `STALL`, `ERROR`, or `CONTRACT_VIOLATION` in the final
  run.

The next iteration is now bottlenecked on routing accuracy and LLM-
driven escalation reason selection, both of which Codex flagged as
Tier-1 (Fix D + 4.4) and Tier-2 (4.6 / answer-after-retrieval) work.

## 6. Next Recommended Actions

In priority order:

1. **Land Codex Fix D in full**: extract a deterministic
   `EscalationReasonResolver` service. Inputs:
   `BotSession.activeUseCase`, `risk`, `userRequestedFlag`, fraud /
   safety / payment-dispute signals from the latest user turn,
   `clarificationCount` / `faqMissCount`, intake-completion flag.
   Output: canonical `escalation_reason`. Tested precedence:
   `user_requested` > `imminent_harm` / `trust_safety_required` >
   `payment_dispute_detected` > `appeal_requires_human` >
   `clarification_budget_exhausted` > `faq_miss_threshold_exceeded` >
   `intake_complete_for_uc_*` > `turn_budget_exhausted` >
   `out_of_scope`. Plumb it into `ControlKernel`,
   `PhaseEvaluator.interpretRunResult`, `ToolDispatcher` (override the
   LLM's `request_handover.escalation_reason` when the resolver
   disagrees), and `SessionManager.recordHandover` (so the persisted
   reason is the canonical one). This addresses 6 of 6 L1 escalation
   compliance fails in the current run.

2. **Fix the two real `L2:handover_completeness` failures**:
   - `cs_036` (hard-OOS path): the synthetic handover persisted in
     `SessionManager.createSession` for hard-OOS topics is missing
     fields the trace collector reads. Inspect
     `MockHandoverLog.handoverPayload` for that case; align the JSON
     shape with `_check_handover_completeness`'s required fields.
   - `cs_066` (FAQ-miss path): the AgentRunLoop FAQ-miss escalation
     path produces a handover record but the payload appears to be
     missing one of `summary`, `escalation_reason`, or
     `total_bot_turns`. Add a small runtime test that escalates from
     RESOLVE/FAQ via `faq_miss_threshold_exceeded` and asserts the
     payload shape.

3. **Address the `case_id_present` failures on cs_038 / cs_040**:
   `ControlKernel.createCaseIfNeeded` calls `createCaseTool.execute` and
   sets `session.caseId`, but the persisted `MockHandoverLog` for these
   sessions does not include the `case_id` field by the time the trace
   collector reads it. Likely a transactional ordering bug — case is
   created before `recordHandover` runs but the `payload.put("case_id",
   session.getCaseId())` fires before the case-creation transaction
   commits. Hold the handover write until the case-id is present, or
   fall back to populating it from `bot_sessions.case_id` rather than
   the in-memory session pointer.

4. **Land routing rules for the disambiguation hot spots** (Codex 4.4):
   - account/login signals on `topic_subject = "Account Support"`
     should resolve UC-D before the LLM runs.
   - explicit "I want to talk to a human / agent / person" signals
     should set `user_requested_escalation` deterministically and
     route to ESCALATE in 1 turn.
   - This is the deterministic pre-LLM stage that the routing-prompt
     fix alone cannot deliver.

5. **Land the citation-gate test matrix and the citation-support
   metric** (Codex new §1.3 / Fix B). The current heuristic is correct
   on the smoke cases observed but is gameable. Add the six tests
   Codex spec'd, and split `source_citation_present` into
   `citation_required` / `citation_present` /
   `citation_supports_answer`.

6. **Activate the remaining Codex Fix C checks**: real
   `fixed_script_template_used` / `fixed_script_forbidden_claims_absent`
   for UC-G/H/I/J/K, plus `turn_efficiency` / `issue_preservation` on
   smoke cases that exercise drift.

7. **Pin server temperatures and add a deterministic replay smoke
   mode** (Codex Fix G). Not a release blocker, but it would remove the
   run-to-run swings that are currently masking signal in regression
   triage.

8. **Document the spec-vs-Codex disagreements** (1.7 / 1.8 / 4.5): the
   PRD owner needs to choose between Phase 2/4 (terminal Delivery hard
   OOS, auto-search at session create) and Codex's recommendation
   (Delivery+fraud override, cheap session creation). The runtime
   should not be patched in either direction until the docs agree.

When 1–4 land, expect smoke pass rate to move into the 5–7/14 band
Codex projected for combined Tier-0+Tier-1 work; 5–7 are needed to
reach 7–9/14; the remaining gap is KB content and routing prompt few-
shots which Codex Fix I says should remain deferred.
