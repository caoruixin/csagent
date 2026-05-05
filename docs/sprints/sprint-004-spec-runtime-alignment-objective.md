# Sprint Objective

Date: 2026-05-05

## Sprint name

Targeted Spec-vs-Runtime Alignment Sprint 4

## Goal

Resolve the remaining high-impact smoke failures where the runtime behaviour is now deterministic enough, but the CaseSpec expectation may be misaligned with transcript evidence, policy, or the approved runtime contract.

Sprint 3 closed the main runtime-reliability blockers:
- C0. Kimi endpoint / credential configuration normalization
- C1. Bot-side LLM retry / timeout robustness
- C2. Replies/Messaging strong-prior carry-forward into the bot-loop
- Sprint 3.1. cs002 already-escalated distress reason reconciliation

Sprint 4 should not broaden eval scope. It should only align a small set of existing smoke expectations or minimal fallback logic where transcript / policy evidence supports the change.

## Baseline

Use this as the current sprint baseline:

`eval_interactive/results/20260504-191137/results.json`

Use this as nondeterminism reference:

`eval_interactive/results/20260504-191541/results.json`

Use this as targeted cs014 reference:

`results/20260504-191028/results.json`

Use these Sprint 3.1 results only as contaminated references, NOT as closure baseline:

- `eval_interactive/results/20260504-201933/results.json`
- `eval_interactive/results/20260504-202424/results.json`

Use this as targeted cs002 Sprint 3.1 proof:

`eval_interactive/results/20260504-201842/results.json`

## Implement only

### E1. cs001 / cs002 escalation-reason expectation alignment

Decide whether the cs001 / cs002 CaseSpecs should accept `user_distress` when the simulator emits distress phrasing, or whether the runtime detector should be narrowed.

Required behaviour:

- Inspect the actual CaseSpec persona, seed messages, hidden facts, and latest transcripts for cs001 and cs002.
- Use transcript evidence and Phase 2 / Phase 5 policy to decide whether `user_distress` is semantically valid.
- If `user_distress` is valid, update the CaseSpecs only through the approved v2 override / audit path.
- Do not directly hand-edit generated YAML as the source of truth.
- If the runtime detector is too broad, add the narrowest runtime fix and focused regression tests.
- Preserve `user_requested` precedence over `user_distress`.
- Preserve `user_distress` precedence over budget reasons.
- Preserve `L1:escalation_reason_consistency = 0`.

Target cases:

- `cs_interactive_001`
- `cs_interactive_002`

### E2. cs029 outcome lift / spec-vs-fallback alignment

Resolve D12: cs029 no longer fails the active_use_case contract, but L2 `correct_uc` still fails because runtime commits UC-D fallback while the spec primary is UC-C.

Required behaviour:

- Inspect the cs029 CaseSpec, source transcript evidence, form context, and runtime trace.
- Decide whether UC-D fallback should be accepted as a secondary / expected fallback, or whether runtime should infer UC-C when the message contains messages / replies / inbox evidence.
- If the CaseSpec is wrong or too narrow, update it through the approved v2 override / audit path.
- If the runtime fallback is wrong, add the narrowest deterministic fallback refinement.
- Semantic escalation reason must remain `user_requested`.
- The original B3 target must still avoid `CONTRACT_VIOLATION:active_use_case`.

Target case:

- `cs_interactive_029`

### E3. Override / audit consistency guard for Sprint 4 changes

Any CaseSpec expectation change in this sprint must be reproducible.

Required behaviour:

- If E1 or E2 changes any generated CaseSpec expected field, the change must be represented in `eval_interactive/case_spec_overrides.yaml`.
- `qa-reports/case-spec-generation-audit.md` must show the override applied.
- `qa-reports/smoke-case-review.md` must agree with the final expected fields.
- Add or update a lightweight regression check that prevents direct generated YAML edits without a matching approved override entry.
- Do not rewrite unrelated smoke / anchor / exploration / promotion cases.

## Do not implement

- cs259 routing / stall stabilisation
- broad anchor / exploration / promotion expansion
- full trace / transcript alignment
- full claim classifier
- full service-outcome taxonomy
- broad rubric rewrite
- production GDPR / moderation / payment / scam / OOS expansion
- L3 judge stabilization
- broad eval redesign
- broad prompt rewrite
- broad routing taxonomy rewrite
- unrelated Kimi / DeepSeek config cleanup
- pre-existing tracked-doc secret scrub

## Target cases

- `cs_interactive_001`
- `cs_interactive_002`
- `cs_interactive_029`

Regression guards:

- `cs_interactive_014` must remain UC-C with the approved cs014 override path intact.
- `cs_interactive_066` must still route to UC-K.
- `cs_interactive_095` must still not route to UC-K.
- `L1:escalation_reason_consistency` must remain 0.
- Sprint 3.1 cs002 already-escalated distress reconciliation must remain green.

## Success metrics

Primary metrics:

- `cs_interactive_001` expected escalation reason is aligned with transcript / persona evidence and runtime behaviour.
- `cs_interactive_002` expected escalation reason is aligned with transcript / persona evidence and runtime behaviour.
- `cs_interactive_029` no longer fails because of UC-D fallback vs spec UC-C mismatch, unless the mismatch is explicitly documented as deferred.
- All CaseSpec expected-field changes go through approved override / audit path.
- `L1:escalation_reason_consistency` remains 0.
- `cs_interactive_014` remains UC-C.
- `cs_interactive_066` remains UC-K.
- `cs_interactive_095` remains not UC-K.

Secondary metrics:

- Smoke pass rate should improve or remain stable.
- Targeted blocker reduction matters more than raw pass count.
- No new P0/P1 blocker should be introduced in Sprint 2 / 2.1 / 3 regression guards.

## Review rule

The next Codex review must only check whether Sprint 4 objective was met.

Codex should not perform a broad review of missing production cases, future groundedness work, service-outcome taxonomy, judge stabilization, large eval expansion, or production policy expansion unless it directly blocks E1, E2, or E3.
