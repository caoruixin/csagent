---
title: Architecture-Health Metrics (definitions only)
doc_tier: proposal
status: proposal
implementation_status: not_started
source_of_truth: this file
last_reviewed: 2026-06-02
review_cadence: every 3-5 sprints
supersedes: []
superseded_by: null
notes: >
  Extracted from docs/current/iteration_governance.md §6 on 2026-06-02
  as part of the Layer A/B always-loaded split. Original section numbering
  preserved. Cite as "architecture-health-metrics §6". These four metrics
  are defined only; collection is not implemented (proposal-tier,
  not_started).
---

# Architecture-Health Metrics

This process doc receives the Architecture-Health Metric definitions
(§6), moved out of the always-loaded `iteration_governance.md` on
2026-06-02. Section numbers are preserved against the original file so
existing citations ("architecture-health-metrics §6") continue to
resolve.

References to sections that stayed in the always-loaded Layer A read
"iteration_governance §X" (e.g., iteration_governance §5.1) for
unambiguity.

## 6. Architecture-Health Metrics (definitions only)

These four metrics are defined here and are referenced by the
iteration_governance §5 acceptance bars. Collection lands in a later
sprint; no metric is collected, dashboard'd, or alerted on as of
Sprint 17.

| metric | definition | unit | observation cadence | source artifact | collection_status |
| --- | --- | --- | --- | --- | --- |
| `new_semantic_hardcode_count` | Number of new keyword / regex / if-else / enum entries added to runtime or prompt for a semantic decision in a PR | count per PR | per PR | PR diff + Anti-Hardcode review verdict | not_started |
| `soft_signal_conversion_count` | Number of existing semantic hardcodes downgraded to LLM-projected soft signals | count per sprint | per sprint close | sprint handoff | not_started |
| `planner_ownership_ratio` | Fraction of semantic decisions in the runtime owned by LLM planning vs Java guard / regex | percentage | per sprint close (manual count) | runtime survey | not_started |
| `shadow_disagreement_rate` | Fraction of shadow cases where LLM decision disagrees with the human-labelled expected behaviour | percentage | per shadow run | shadow eval result | not_started |

The direction of health is: `new_semantic_hardcode_count` down,
`soft_signal_conversion_count` up, `planner_ownership_ratio` up,
`shadow_disagreement_rate` down. When collection lands,
iteration_governance §5.1's "Architecture-health metrics not
regressed" bar consults these.
