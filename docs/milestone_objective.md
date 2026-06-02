---
title: Milestone objective — NEXT MILESTONE TBD (M-Auto-4 candidate selection)
doc_tier: current-runtime
status: proposal
implementation_status: not_started
source_of_truth: this file
last_reviewed: 2026-06-03
review_cadence: per milestone
supersedes: [docs/milestones/M-Auto-3_objective.md]
superseded_by: null
notes: >
  PLACEHOLDER. Milestone M-Auto-3 (Substrate-hygiene) CLOSED 2026-06-03 —
  Class A (all §11 HARD gates met under the three-class PARAPHRASE_STORM
  taxonomy; milestone-shared Codex `pass / 0`). Archived at
  `docs/milestones/M-Auto-3_objective.md` + `M-Auto-3_codex-review.md`.

  No active milestone. The next milestone (M-Auto-4) is pending human scope
  selection. Candidate slate:
  - **Autoloop fitness MEASUREMENT reliability** (k-of-n majority sampling +
    rebaseline + bounded escalation-spec loosening) — Codex-authored draft
    proposal at `docs/proposals/autoloop_fitness_measurement_reliability.md`
    (advisory; human selects scope). Motivated by the verified 0-keep / 67-iter
    finding: even on the cleaned M-Auto-3 substrate the loop short-circuits at
    Layer 0/1 on provider measurement noise that source-pinning cannot remove
    (temp already 0.0; seed/top_p unavailable on DeepSeek/Kimi). Touches the 4
    SHA-locked scoring files → fence-#13 controlled override + per-sub-sprint
    Codex.
  - **M3-B Single Handover Orchestrator** (exactly-once side-effect owner;
    release-gate blocker; `docs/proposals/handover_orchestrator_design.md`).
  - cross-turn rank-1 first-refinement (M-Auto-3 OBSERVATION carry, OQ-S69.1
    residual) — only if a future overnight shows it materially corrupts
    fitness; a product/semantic-policy question, NOT a runtime hardcode.

  **Standing direction**: the first overnight launch on the cleaned M-Auto-3
  substrate is HELD for explicit human instruction. Per the measurement-
  reliability finding, expect it to still 0-keep — that is the milestone's
  intended DIAGNOSABLE result (noise vs propose-quality), not a regression.

  Replace this placeholder with the M-Auto-4 milestone objective once the human
  selects scope and the deliver-agent drafts it (human review before use).
---

# Milestone objective — TBD (M-Auto-4 candidate selection)

No active milestone. See the front-matter `notes` for the M-Auto-3 close
status, the M-Auto-4 candidate slate, and the held overnight launch. The
deliver-agent drafts the M-Auto-4 objective here after the human selects scope.
