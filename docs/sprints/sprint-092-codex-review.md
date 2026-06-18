# Sprint 092 / S-Auto-38 — Codex review (archived)

> Per-sub-sprint anti-hardcode review for the inserted blocker S-Auto-38
> (escalation_compliance tier-0 split). Archived verbatim from
> `docs/codex-findings.md` at the S-Auto-38 sub-sprint close (2026-06-18). The
> live `docs/codex-findings.md` retains this verdict until M-Auto-7 milestone
> close (then folds into `docs/milestones/`).

## Sub-sprint Review Decision — S-Auto-38 (Sprint 092, escalation_compliance tier-0 split)
decision: pass
blocking_count: 0
summary: Kernel verdict `approve`: WP-A removes the implicit eval-internal escalation-reason-family-to-tier-0 binding while preserving the deterministic escalation behaviour floor, and WP-B adds a narrow, explicit, human-reviewable override schema without adding active registry entries or changing runtime/tool surfaces.

Reviewed: WP-A `57cd93c5` (gate split) + WP-B `8ed65cc6` (override schema), against the zero-LLM replay evidence (`analysis/out/replay_s_auto_38_output.txt`). Prompt: `compact/sprint-092-codex-review-prompt.md`. Read-only `codex exec` (model_reasoning_effort=high); verdict recorded verbatim below.

Kernel verdict: `approve`.

Justification: WP-A keeps `escalation_compliance` as Part-1 in `_TIER0_PY_FAMILY`, moves Part-2 to `escalation_reason_family_match` with advisory default severity, and the replay evidence shows 43/43 tier-0 flips are exactly `PART2_DEMOTION`, with 0 regressions and the high-risk should-escalate negative control still discarded. WP-B's override path is scoped to eval scoring, requires `accepted_reasons` XOR `accepted_families`, restricts enforcement levels, and rejects tier-0 re-elevation unless approved, safety-critical, and cited. No prompt if-else, visible-eval phrase pin, runtime tool-schema change, PII-floor weakening, or grounding-floor weakening found.

Per-question concerns: none blocking. The only semantic enum/allow-list surface is the explicit eval override schema, not a runtime hardcode; currently there are zero approved escalation bindings in the active registry.

## Binding-gate disposition at close

| gate | status at close |
|------|-----------------|
| 0 pre-dev zero-LLM sweep | ✅ recorded (handoff §4) |
| 1 WP-A + WP-B implemented (independent commits/tests) | ✅ `57cd93c5` / `8ed65cc6` |
| 2 zero-LLM OLD/NEW replay (43/43 PART2_DEMOTION, 0 regressions, neg-control discards, flip-elim, other-invariant byte-identical, override-takes-effect) | ✅ handoff §5 |
| 3 Codex §4.1 (this verdict) | ✅ `approve` / `pass` / 0 |
| 4 human blast-radius sign-off | ✅ launch record §2–§3 (5 composite flips all PART2_DEMOTION; cs76s01/02 correctly stay fail) |
| 5 new dated re-bless dir (run-scoped Option-R) | ✅ `m-auto-7-prepilot-baseline-20260618-s_auto_38_split_full` (run-scoped via `config.pilot-s-auto-38.yaml`; canonical pointer flip deferred to milestone close) |
| 6 pilot-resume go/no-go | gate-trust validated by the exp-86 `-n1` smoke (PASS); **full pilot tranche stays HELD** pending the OQ-S86b.3 runtime corrective sub-sprint |

Verdict consumed at the S-Auto-38 sub-sprint close. The milestone-shared §4.3 review still fires at M-Auto-7 close over the cumulative range.
