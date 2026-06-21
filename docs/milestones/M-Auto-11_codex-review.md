# M-Auto-11 — Codex review (archived)

**Milestone:** M-Auto-11 — Loop-convergence / viable-hit utilization (characterization-first).
**Archived:** 2026-06-22 (at milestone close).
**Scope reviewed:** the single committed sub-sprint WP1 (Sprint 101 / S-Auto-49) — a
characterization sub-sprint whose only non-doc artifact is the new TARGET CaseSpec
`cs_uc_a_viable_hit_loop_nonconvergence` (+ a `_manifest.md` row + a mechanical count-anchor
test bump). Per `iteration_governance.md` §4.1, WP1 is anti-hardcode-kernel **EXEMPT** as a
characterization-test sub-sprint; the human nonetheless directed the milestone-shared Codex
anti-hardcode review to run, focused on the new TARGET CaseSpec.

## Dispatch note

Read-only `codex exec` (deliver-dispatched per the deliver-can-dispatch pattern), gpt-5.5,
`--sandbox read-only`. The default `model_reasoning_effort=high`/`xhigh` runs hung at the
`aicodewith` `wire_api=responses` gateway (two attempts, ~2h and ~1h24m, 0 output — a
gateway reasoning-budget stall, not a request defect; `wire_api=chat` is no longer
supported by the CLI). The review completed on the supported `responses` endpoint with
`model_reasoning_effort=minimal`. Verdict recorded verbatim below.

## Milestone Review Decision — M-Auto-11 (WP1 TARGET CaseSpec)

```
## Codex Review Decision (M-Auto-11 WP1 TARGET CaseSpec)
decision: approve
blocking_count: 0
summary: (1) No: persona is mild/no-drift with two natural seed messages and hidden_facts
only disclosed if asked; closure explicitly forbids scripting/baiting repeated search.
(2) No: wording defines a characterization failure for manual adjudication, not a forced
scored outcome. (3) No: expected outcome remains resolve, conditional acceptance requires
satisfied resolve, escalation is never auto-pass, and scoring only has correct_uc/
correct_outcome with safety in hard_checks; no avoidable-MAX_STEPS scored mask. (4) Yes:
`allow_bot_resolution: 'true'` is schema-conformance mapping from blessed `full`, because
schema defines only `Literal["true","false","partial"]`. (5) No: no keyword/regex/if-else/
enum semantic hardcode or runtime/prompt/scoring/simulator touch found in the reviewed
CaseSpec; manifest row exists for this single new bad case.
```

## §4.2 milestone-close header

```
## Milestone Review Decision — M-Auto-11
decision: pass
blocking_count: 0
summary: The single committed sub-sprint (WP1 characterization) ships only one additive
TARGET CaseSpec (+ manifest row + a mechanical count-anchor test bump). The Codex
anti-hardcode review of that CaseSpec returned approve / 0: no scripted retrieval, no
benchmark-engineered wording, no oracle weakening (scoring stays correct_uc + correct_outcome
+ safety floor; avoidability is a manual adjudication), the allow_bot_resolution full→true is
a forced schema-conformance mapping, and no forbidden runtime/prompt/scoring/simulator
surface is touched. No blocking findings.
```
