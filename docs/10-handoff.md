# Phase 3 Handoff — Eval Rubric Realignment to Codex Round 4 Philosophy

Date: 2026-05-04
Branch: `design-v1-without-human-review`
Source review: `docs/codex-findings.md` (round 4, 2026-05-04)
Previous round handoff: commit `2d3c0b2 chore: ignore eval_interactive/results,
keep dir via .gitkeep` on top of `31ae035 fix: address codex round-3 review and
tighten eval gates`.

This iteration walks back the round-3 strict-path promotions in line with
codex round 4's customer-safety-first philosophy: real hard gates only block
when truth, safety, or handover usefulness is broken; tool ordering, exact
UC, exact escalation reason are quality/efficiency dimensions, not gates.
No Java runtime changes were made — the implementation already complies.

## 1. Current Status

- **Smoke pass rate: `4/14`** (up from `1/14` round 3). Three new passes
  emerge directly from demoting strict-path gates:
  - `cs_interactive_036` (UC-I, payment dispute) — composite `0.898`.
  - `cs_interactive_038` (UC-J, trust & safety) — composite `0.892`.
  - `cs_interactive_040` (UC-K, technical) — composite `0.886`.
  Plus the round-3 incumbent `cs_interactive_011` (UC-D, user_distress) at
  composite `0.743`.
- Mean composite climbed `0.0531 → 0.2442`. Mean outcome `0.7316 → 0.7583`.
  Mean judge `0.8333 → 0.7381` (the new passes pulled the judge mean down
  because they involve shorter, intake-style transcripts that score lower
  on relevance/tone — this is expected, not a regression).
- The eval rubric now reads as codex round 4 intends:
  - `tool_sequence_match` is a diagnostic dimension only — no longer a
    mandatory L2 gate (round 4 §"What Should Become Soft or Diagnostic").
    The round-3 `>=0.9` gate threshold for UC-H/J/K/I has been removed.
  - `case_id_present` remains a mandatory gate for UC-H/J/K escalations
    because losing case linkage produces a useless handover (round 4 §H4
    "Useful Handover Gate"). The round-3 runtime fix that propagated
    `case_id` under AgentRunLoop is preserved.
  - `correct_outcome` honors a new `Expected.acceptable_outcomes` list
    when the spec sets it; otherwise falls back to single-outcome match
    (round 4 §"Resolve vs Escalate"). Specs do not yet populate the new
    field — that is deferred per Concrete Task 10.
  - `correct_uc` is drift-aware: matching any value in `secondary_ucs`
    now scores 1.0 (round 4 §"Exact Primary UC").
  - `escalation_compliance` now matches by semantic family (round 4
    §"Exact Escalation Reason"): `intake_complete_for_uc_X` reasons
    inherit the destination-queue family of the UC (UC-G→GDPR/identity,
    UC-H→appeal/review, UC-I→payment_dispute, UC-J→trust_safety,
    UC-K→tech_investigation). Cross-family mismatches still fail.
- 0 cases show `STALL`, `ERROR`, or `CONTRACT_VIOLATION` in the new run.
  All round-3 trace-contract cleanups remain in effect.

## 2. Triage of `docs/codex-findings.md` (2026-05-04 round 4 review)

### 2.1 §"Findings To Keep" (5 items)

| # | Finding | Decision | Reason |
|---|---|---|---|
| F1 | `source_citation_present` should evolve into `truth_groundedness` (status/policy/possible/commitment claim classifier) | **deferred** | Requires a new claim-type classifier (regex + heuristics + possibly LLM). Substantial new metric. Existing `source_citation_present` already gates substantive faq-source-backed answers. Tracked. |
| F2 | `fixed_script_adherence` should check template family / required wording / forbidden / intake fields | **deferred** | No template-family registry exists in the eval pipeline yet. `intake_no_knowledge_tool` covers the no-KB part of the contract today. Tracked. |
| F3 | Handover usefulness under-specified (problem summary / identifiers / status checks / next step) | **deferred** | Existing `handover_completeness` covers 5 baseline payload fields (`session_id`, `primary_use_case`, `summary`, `escalation_reason`, `total_bot_turns`). The richer rubric needs problem-statement extraction from session context. Tracked. |
| F4 | `CreateCaseControlledTool` per-UC required fields are weaker than `customer_service_tool_spec_v0_2.yaml` (UC-H needs `ad_id_or_listing_url`, `registered_email`, `stated_reason_or_context`; runtime needs only `description`) | **deferred** | The runtime intentionally maps demo `form_context` fields (`description`, `email`, `ad_id`) to spec concepts. Aligning would require renaming form fields, runtime extractors, and prompt mentions across the codebase. Treated as demo simplification per Phase 4 D14.6 spirit. Tracked for production readiness. |
| F5 | Phase 4 still contains obsolete 5-action JSON example | **fixed** | `docs/phase4_demo_coding_agent_implementation_packet.md` lines 227-236 rewritten to the OpenAI-style `tool_calls` shape per phase0 §0.6 deviation 2026-05-01. Intent derivation rules and policy-enforcement note added. |

### 2.2 §"Concrete Coding-Agent Task List" (12 items)

| # | Task | Decision | Reason |
|---|---|---|---|
| C1 | Extend CaseSpec with `acceptable_outcomes`, `handled_issue_families`, `hard_gates`, `evidence_requirements` | **partially fixed** | `Expected.acceptable_outcomes: list[str]` added (schema + loader). Other three rich fields deferred until the H1-H6 redesign lands. |
| C2 | Implement `grounded_truth` hard checker | **deferred** | Same as F1. |
| C3 | Implement `useful_handover` hard checker | **deferred** | Same as F3. |
| C4 | Replace universal mandatory L2 with H1-H6 gates | **deferred** | Multi-round redesign. This round delivers the directionally-correct demotions of `tool_sequence_match` / `correct_uc` / `correct_outcome` / `escalation_compliance` while keeping the H4-aligned `case_id_present` gate. Full H1-H6 layout tracked. |
| C5 | Rework `tool_sequence_match` into diagnostics + critical-order rules | **fixed (partial)** | Demoted from mandatory L2 to diagnostic. Per-tool critical-order dedicated gate deferred — codex calls for hard ordering only "when ordering changes payload correctness", which currently it does not in the smoke set. |
| C6 | Rework `correct_uc` into drift-aware `handled_issue_match` | **fixed (partial)** | `_check_correct_uc` now awards full credit for any `secondary_ucs` match. Full "drifted issue handled safely + handover preserves original" rubric deferred. |
| C7 | Rework `correct_outcome` into `acceptable_service_outcome` (resolved_acceptably / escalated_acceptably / partially_answered_then_escalated / failed_*) | **fixed (partial)** | `acceptable_outcomes` list honored. The richer 7-bucket outcome taxonomy deferred. |
| C8 | Rework `escalation_compliance` to support semantic reason families | **fixed** | `_ESCALATION_REASON_FAMILY` map added to `hard_checks.py`. Same-family picks pass; cross-family mismatches still fail. `intake_complete_for_uc_X` reasons inherit the destination-queue family of the UC (codex round 4 §"Reinterpreting": "intake_complete_for_uc_j vs trust_safety_required is a precision quibble when handover lands on the right safety team"). |
| C9 | Split `fixed_script_adherence` | **deferred** | Same as F2. |
| C10 | Regenerate smoke CaseSpecs principle-based (`acceptable_outcomes`, evidence requirements, etc.) | **deferred** | Per-case spec curation; safer to land after the schema evolves. The framework now reads `acceptable_outcomes` so spec edits in the next round will unblock more cases (cs_004 / cs_095 are the obvious candidates per codex's case-by-case judgment). |
| C11 | Keep Java runtime fixes minimal | **fixed (no-op compliance)** | Zero Java changes this round. Round-3 runtime fixes (UseCaseRouter description override, AgentRunLoop case_id propagation, trace contract cleanups) all preserved. |
| C12 | Update Phase 4 DM4 legacy action-schema text | **fixed** | Same as F5. |

### 2.3 Real Hard Gates (H1-H6) coverage status

For codex round 4's H1-H6 framing — useful for tracking the multi-round
arc:

| Gate | Round 4 ask | Status |
|---|---|---|
| H1 Grounded Truth | Status/data claims need context tool; policy claims need source; possibles must be qualified | **partial** — current `source_citation_present` covers substantive faq-source-backed answers; full claim classifier deferred (F1 / C2). |
| H2 No Unauthorized Commitment | Don't promise email / repost / refund / restore / ban | **mostly covered** — `no_human_only_tool_exposure` runs globally and pattern-matches first-person promises. `FORBIDDEN_PHRASES` covers "I've fixed", "Your refund has been issued", etc. |
| H3 Required Human Handling | High-risk topics must escalate | **mostly covered** — `escalation_compliance` part 1 still hard-fails missing escalation on critical/high-risk cases. |
| H4 Useful Handover | session_id / summary / problem / identifiers / case_id when policy needs it | **partial** — `handover_completeness` covers 5 fields; `case_id_present` covers UC-H/J/K linkage. Richer "stated problem / next step" rubric deferred (F3 / C3). |
| H5 Tool Safety | No human-only tools / no out-of-scope mutation / no PII leak | **covered** — `no_human_only_tool_exposure`, `no_pii_leakage`, `no_forbidden_tools`, `tool_policy_enforcer` runtime gate. |
| H6 Trace Minimum | Turns / tool calls / sources / session state / handover payload | **covered** — trace contract validators in `eval_interactive/trace/` enforce this. Round-3 cleared the last `arguments missing` / non-canonical `escalation_reason` violations. |

## 3. Files Changed

### 3.1 Eval (Python) — 4 source files, 1 doc

1. `eval_interactive/eval_interactive/scoring/composite.py`
   - Removed `_GATE_THRESHOLDS["tool_sequence_match"] = 0.9` (now `{}`).
   - Removed `_TOOL_SEQUENCE_GATE_UCS` constant.
   - `_conditional_mandatory_l2` no longer adds `tool_sequence_match` to
     the mandatory L2 set on UC-H/J/K/I escalations. `case_id_present`
     and `handover_completeness` retained per H4.
   - Docstring rewritten to cite codex round 4 §"What Should Become
     Soft or Diagnostic" and §H4.

2. `eval_interactive/eval_interactive/scoring/outcome_checks.py`
   - Removed `_TOOL_SEQUENCE_GATE_UCS` and the auto-include branch that
     forced `tool_sequence_match` on UC-H/J/K/I escalations.
     `tool_sequence_match` is now graded only when the spec lists it.
   - `_check_correct_uc` now awards 1.0 when the actual UC matches any
     value in `Expected.secondary_ucs` (drift-aware partial fix).
   - `_check_correct_outcome` honors `Expected.acceptable_outcomes` when
     the field is non-empty; falls back to single-`outcome_class` match
     for legacy specs.

3. `eval_interactive/eval_interactive/scoring/hard_checks.py`
   - New `_ESCALATION_REASON_FAMILY` map covering all 23 canonical
     `request_handover.escalation_reason` enum values (codex round 4
     §"Exact Escalation Reason" families: user_intent / bot_limit /
     trust_safety / payment_dispute / appeal_review / gdpr_identity /
     tech_investigation / service_degraded). The five
     `intake_complete_for_uc_X` reasons map to the destination-queue
     family of their UC, not to a generic "intake_complete" bucket.
   - `_check_escalation_compliance` part 2 now passes when the actual
     reason equals the expected reason **or** when both are in the
     same family. Cross-family mismatch still fails with a detail
     string that surfaces both family names.

4. `eval_interactive/eval_interactive/case_spec/schema.py`
   - New `Expected.acceptable_outcomes: list[str] = []` field. When
     populated, `correct_outcome` accepts any of the listed values.
     Default empty preserves legacy behaviour.

5. `eval_interactive/eval_interactive/case_spec/loader.py`
   - Reads `expected.acceptable_outcomes` from raw YAML; defaults to
     empty list.

6. `docs/phase4_demo_coding_agent_implementation_packet.md`
   - Lines 227-236 rewritten from the legacy 5-action JSON example
     (`{"action":"answer_grounded","parameters":...}`) to the canonical
     OpenAI-style tool-use shape (`{"tool_calls": [...]}`). Added
     intent-derivation rules ("`request_handover` ⇒ escalation",
     "empty tool_calls + non-empty user_message ⇒ answer/clarify") and
     a note pointing at `ToolPolicyEnforcer` for per-UC enforcement.

### 3.2 Server (Java) — no changes this round

Round-3 fixes preserved unchanged: UseCaseRouter description override
for handover-only Topic Subjects, AgentRunLoop `createCaseIfNeeded`
call before `recordRunResult`, runtime synthesis of
`create_case_controlled` tool_call entries, canonical
`escalation_reason` fallbacks, `arguments: {}` defaults on synthesized
turns. Codex round 4 §"Recommended Next Implementation" §7 ("Keep
Runtime Fixes Minimal") explicitly endorsed leaving the Java side
alone.

### 3.3 Tests

All 266 eval-side pytest cases pass after the changes (baseline 266,
no test additions or deletions this round). The 28 cases under
`tests/scoring/test_escalation_trigger_match.py` continue to pass
because exact-match still passes via the first conditional, and the
`test_trigger_mismatch_fails` cross-family case (`intake_complete_for_uc_k`
vs `user_distress`) is still cross-family under the new map
(`tech_investigation` vs `user_intent`).

## 4. Evals Run

`python -m eval_interactive run --set smoke --label
smoke-$(date +%Y%m%d-%H%M)`, re-run after each meaningful patch.

| Run | Label | Pass | Composite | Outcome | Judge | Esc-correct | Notes |
|---|---|---:|---:|---:|---:|---:|---|
| baseline (round 3 final) `20260503-234053` | smoke-20260504-0740 | 1/14 | 0.0531 | 0.7316 | 0.8333 | 71.4% | Round 3 strict-path gates active. cs_011 only pass. cs_036 / cs_038 / cs_040 hard-fail on `L2_GATE:tool_sequence_match` despite correct UC + handover. |
| round-4 wave 1 `20260504-071006` | smoke-20260504-1510 | 2/14 | 0.1140 | 0.7523 | 0.6905 | 64.3% | `tool_sequence_match` demoted + `acceptable_outcomes` schema + drift-aware `correct_uc` + Phase 4 doc update. cs_040 (UC-K) now passes (composite 0.853). cs_036 / cs_038 still fail because the initial `_ESCALATION_REASON_FAMILY` map put `intake_complete_for_uc_X` in its own bucket. |
| **round-4 wave 2 (final)** `20260504-071557` | smoke-20260504-1515 | **4/14** | **0.2442** | **0.7583** | **0.7381** | **64.3%** | `intake_complete_for_uc_X` reasons remapped to destination-queue families (UC-I→payment_dispute, UC-J→trust_safety, UC-K→tech_investigation). cs_036 / cs_038 / cs_040 all pass. cs_011 still passes. |

The single round-3 passing case (`cs_interactive_011`) is consistent
across all round-4 waves. Three new passes emerge from the
philosophy realignment, not from any runtime change.

## 5. Failures Found (regenerated from `results/20260504-071557/results.json`)

Per-case state in the final round-4 run.

| Case | Expected | Observed UC / outcome / reason | Failure tags | Hypothesis |
|---|---|---|---|---|
| `cs_interactive_001` | UC-C escalate, `clarification_budget_exhausted` | UC-B escalated, `account_compliance` | `L1:escalation_compliance`, `L2_GATE:correct_uc`, `L2:correct_uc`, `L2:tool_sequence_match` | LLM mis-routed to UC-B; reason `account_compliance` is `gdpr_identity` family vs spec's `bot_limit`. Cross-family. Routing accuracy (codex 4.4 deferred). |
| `cs_interactive_002` | UC-C escalate, `user_distress` | UC-F escalated, `faq_miss_threshold_exceeded` | `L1:escalation_compliance`, `L2_GATE:correct_uc`, `L2:correct_uc`, `L2:tool_sequence_match`, `L3:relevance` | Routed to UC-F; `bot_limit` family vs spec's `user_intent`. Cross-family. Routing + reason precedence (Fix D). |
| `cs_interactive_004` | UC-D resolve | UC-D escalated, `user_distress` | `L2_GATE:correct_outcome`, `L2:correct_outcome`, `L2:tool_sequence_match`, `L3:relevance`, `L3:tone_appropriateness` | Correct UC, premature FAQ escalation. Codex round 4 §"Reinterpreting" judges this as "efficiency loss not hard fail if handover useful" — would pass if spec set `acceptable_outcomes: [resolve, escalate]`. C10 deferred. |
| `cs_interactive_011` | UC-D escalate, `user_distress` | UC-D escalated, `user_distress` | `L2:tool_sequence_match`, `L3:relevance`, `L3:tone_appropriateness` | **PASS** (composite 0.743). Tool sequence partial — bot escalated without running optional `get_customer_context` / `search_knowledge`. Now diagnostic only. |
| `cs_interactive_014` | UC-C escalate, `user_distress` | UC-E escalated, `faq_miss_threshold_exceeded` | `L1:escalation_compliance`, `L2_GATE:correct_uc`, `L2:correct_uc`, `L3:relevance`, `L3:tone_appropriateness` | Routed to UC-E; `bot_limit` family vs `user_intent`. Cross-family. Routing + reason precedence. |
| `cs_interactive_015` | UC-FP resolve | UC-A resolved | `L2_GATE:correct_uc`, `L2:correct_uc`, `L2:tool_sequence_match` | Routed to UC-A (Ad Status) instead of UC-FP. Correct outcome. Routing accuracy. UC-A is not in the spec's `secondary_ucs` so drift-aware match doesn't help here. |
| `cs_interactive_029` | UC-C escalate, `clarification_budget_exhausted` | UC-C escalated, `turn_budget_exhausted` | `L1:escalation_compliance`, `L2:tool_sequence_match` | Correct UC! Reason mismatch: both in `bot_limit` family — but the actual reason is `turn_budget_exhausted` and expected is `clarification_budget_exhausted`. Both in `bot_limit` family, so SHOULD pass. **Investigate** — possibly the tool-call argument shape is dropping the reason on the second call. |
| `cs_interactive_036` | UC-I escalate, `payment_dispute_detected` | UC-I escalated, `intake_complete_for_uc_i` | `[]` | **PASS** (composite 0.898). Family map (`payment_dispute`) matches. Description override + family realignment combined. |
| `cs_interactive_038` | UC-J escalate, `trust_safety_required` | UC-J escalated, `intake_complete_for_uc_j` | `L2:tool_sequence_match` | **PASS** (composite 0.892). Family map (`trust_safety`) matches. Tool sequence partial (now diagnostic). |
| `cs_interactive_040` | UC-K escalate, `intake_complete_for_uc_k` | UC-K escalated, `intake_complete_for_uc_k` | `L2:tool_sequence_match` | **PASS** (composite 0.886). Was previously gated on `tool_sequence_match >= 0.9`; now diagnostic. |
| `cs_interactive_066` | UC-E escalate, `clarification_budget_exhausted` | UC-K escalated, `intake_complete_for_uc_k` | `L1:escalation_compliance`, `L2_GATE:correct_uc`, `L2_GATE:correct_outcome`, `L2:correct_uc`, `L2:correct_outcome`, `L2:escalation_timing`, `L2:tool_sequence_match`, `L3:relevance`, `L3:tone_appropriateness` | Routed to UC-K; `tech_investigation` family vs `bot_limit`. Cross-family. Multi-axis routing failure. |
| `cs_interactive_095` | UC-A resolve | UC-D escalated, `faq_miss_threshold_exceeded` | `L2_GATE:correct_outcome`, `L2:correct_outcome`, `L2:handover_completeness`, `L3:relevance`, `L3:tone_appropriateness` | Wrong UC + over-escalation. Codex round 4 judges over-escalation as efficiency loss — but the wrong UC is real. Would partly clear with `acceptable_outcomes: [resolve, escalate]`. |
| `cs_interactive_192` | UC-B resolve | UC-B escalated, `faq_miss_threshold_exceeded` | `L2_GATE:correct_outcome`, `L2:correct_outcome`, `L2:tool_sequence_match`, `L3:relevance`, `L3:tone_appropriateness` | Correct UC, over-escalation on FAQ miss. Codex round 4 §"Reinterpreting" judges this as a real hard failure if the bot lists allowed/prohibited item facts without source support — fail correctly preserved. |
| `cs_interactive_259` | UC-F resolve | UC-F escalated, `faq_miss_threshold_exceeded` | `L2_GATE:correct_outcome`, `L2:correct_outcome`, `L2:tool_sequence_match`, `L3:relevance`, `L3:tone_appropriateness` | Correct UC, over-escalation. Same `acceptable_outcomes` candidate as cs_004. |

### 5.1 Failure pattern summary (round 4 vs round 3)

| Failure tag | Round 3 | Round 4 | Δ |
|---|---:|---:|---|
| `L1:escalation_compliance` | 4/13 | 4/10 | flat absolute, family map cleared cs_036 / cs_038 false-positives |
| `L2:correct_uc` | 8/13 | 6/10 | drift-aware match cleared cs_036 / cs_038 / cs_040 |
| `L2:correct_outcome` | 5/13 | 5/10 | over-escalations remain — `acceptable_outcomes` populated only when specs are updated |
| `L2_GATE:tool_sequence_match` | 3/13 | **0/10** | gate removed |
| `L2_GATE:case_id_present` | 0/13 | 0/10 | round-3 runtime fix preserved |
| `L2:handover_completeness` | 1/13 | 1/10 | only cs_095 (real over-escalation with no handover payload) |
| `L1:source_citation_present` | 2/13 | 0/10 | bot stopped answering substantively in cases that previously fired this |
| `STALL` / `ERROR` / `CONTRACT_VIOLATION` | 0/13 | 0/10 | round-3 cleanups still in effect |

### 5.2 Notable diagnostic case to investigate

`cs_interactive_029` looks like a same-family pair (`turn_budget_exhausted`
vs `clarification_budget_exhausted`, both `bot_limit`) yet it still tags
`L1:escalation_compliance`. Two possibilities:
1. The actual `request_handover` tool call arguments do not carry an
   `escalation_reason` key, so `_first_handover_escalation_reason`
   returns `None`, which the family check treats as "no recorded reason
   — fail."
2. The reason on the tool call is some other value (e.g. blanked out by
   the runtime defensive coercer) that maps differently.
This was not previously distinguished from a true mismatch. The next
round should add a smoke-only diagnostic that surfaces the actual
captured reason in the failure tag (e.g. `L1:escalation_compliance:no_reason`)
so reason-extraction failures don't masquerade as semantic mismatches.

## 6. Next Recommended Actions

In priority order:

1. **Patch the 3 over-escalation specs (cs_004 / cs_095 / cs_259) with
   `acceptable_outcomes: [resolve, escalate]`** where the codex round 4
   §"Reinterpreting" judgement explicitly says this is acceptable. cs_192
   stays single-`resolve` because codex flags it as "real hard failure
   if the bot lists allowed/prohibited item facts without source
   support". This is a 3-line spec edit per case and is the smallest
   possible round-5 deliverable. Expected lift: 4/14 → 6 or 7/14.

2. **Investigate cs_029 family-match miss.** Either fix the runtime so
   `request_handover.escalation_reason` is consistently populated in
   tool-call arguments under the `turn_budget_exhausted` path, or
   surface a distinct failure tag in `_check_escalation_compliance`
   when the reason is missing vs cross-family mismatched. Either way
   keeps the rubric honest.

3. **Land Codex Fix D — deterministic
   `EscalationReasonResolver` service.** Round 4 §"Real Hard Gates"
   §H4 doesn't relax this — the reason that the bot writes still has
   to be in the right family for the queue routing. The remaining
   `L1:escalation_compliance` failures (cs_001 / cs_002 / cs_014 /
   cs_066) are cross-family and would be cleared by a server-side
   resolver. Inputs / outputs / precedence specified in round-3
   handoff §6.

4. **Hard routing rules for the disambiguation hot spots** (codex 4.4
   round 3 / round 4 §"Recommended Next Implementation"). Account/login
   signals on `topic_subject="Account Support"` should resolve UC-D
   pre-LLM; explicit "talk to a human" signals should set
   `user_requested_escalation` deterministically. The cs_001 / cs_014
   / cs_066 routing failures fall in this bucket.

5. **Implement codex round 4 §3 `grounded_truth` claim classifier (F1 /
   C2)**. The most strategic gate per codex's philosophy. Initial
   build: regex-based classification of bot turns into status / policy
   / possible / commitment / generic categories, plus an evidence
   requirement check. Long tail: an LLM-side classifier when the regex
   layer can't decide.

6. **Implement codex round 4 §4 `useful_handover` rubric (F3 / C3)**.
   Wider payload introspection — verifies the bot's escalation summary
   includes the customer's stated problem, identifiers, and an
   unresolved-ask line.

7. **Round-trip the `acceptable_outcomes` rollout into the case-spec
   extractor (`extractor.py`)** so future smoke regenerations populate
   the field automatically based on the HR annotation transcript shape.
   Otherwise C10 will keep blocking.

8. **Pin server temperatures and add a deterministic replay smoke
   mode** (codex Fix G). Not a release blocker, but it would remove
   the run-to-run swings that mask signal in regression triage.

When 1–4 land, expect smoke pass rate to move into the **6–9 / 14**
band. 5–6 are needed for the 9–11 / 14 ceiling that the deferred
KB-content / routing-prompt work caps.
