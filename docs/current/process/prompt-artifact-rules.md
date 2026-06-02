---
title: Agent prompt artifact rules (self-containment invariant)
doc_tier: current-runtime
status: current
implementation_status: implemented
source_of_truth: this file
last_reviewed: 2026-06-02
review_cadence: every 3-5 sprints
supersedes: []
superseded_by: null
notes: >
  Extracted from docs/current/iteration_governance.md §9 on 2026-06-02
  as part of the Layer A/B always-loaded split. Original section numbering
  preserved. Cite as "prompt-artifact-rules §9.N".
---

# Agent prompt artifact rules

This process doc receives the agent prompt artifact rules (§9), moved
out of the always-loaded `iteration_governance.md` on 2026-06-02.
Section numbers are preserved against the original file so existing
citations ("prompt-artifact-rules §9.3", "§9.1") continue to resolve.

References to sections that stayed in the always-loaded Layer A read
"iteration_governance §X" (e.g., iteration_governance §4.1) for
unambiguity. References to the milestone schema read
"milestone-framework §8.3".

## 9. Agent prompt artifact rules (2026-05-26 update)

This section codifies the **self-containment invariant** for the
prompt files that the deliver-agent produces for dev and review
agents. The invariant exists so that a fresh dev or review session
can be started by pasting a single prompt file into a new session,
without that session having to read any further repo doc (other
than `AGENTS.md` governance chain, which is auto-loaded).

### 9.1 Invariant

A **prompt artifact** (`compact/sprint-NNN-dev-prompt.md` for dev,
`compact/M<N>-review-prompt.md` for review) is a **self-contained
executable view** of its source-of-truth contract:

- Dev prompt source-of-truth = `docs/sprint_objective.md`
- Review prompt source-of-truth = `docs/milestone_objective.md` + the
  per-sub-sprint objective archives + the per-sub-sprint dev handoffs

**Self-contained** means: a fresh dev / review session, given ONLY
this prompt file (plus `AGENTS.md` governance chain, auto-loaded),
has every piece of information it needs to:

- understand its role and the bounded scope of the session;
- execute the contract end-to-end (write code / run tests / author
  handoff for dev; walk iteration_governance §4.1 kernel + verify
  scope discipline + produce `docs/codex-findings.md` for review);
- self-check that the work is complete before claiming so.

The prompt MUST embed (not reference) all contract content. The
single exception is artefacts that the prompt's consumer is
expected to produce (e.g., per-sub-sprint dev handoff is consumed
by review but produced by dev; review prompt references handoff
paths but cannot embed handoff content because it does not yet
exist at prompt-authoring time).

### 9.2 Embed vs reference rules

| Content | Embed in prompt | Reference only |
|---|---|---|
| Role identity, goal, scope, hard fences, test/eval requirements, §7 stanza, handoff requirements, commit discipline | ✓ | |
| §4.1 nine-question kernel (in review prompt) | ✓ | |
| Sub-sprint cumulative scope claim (in review prompt) | ✓ | |
| Governance chain (Constitution, doc_governance, agent_context_guide, iteration_governance) | — | Via AGENTS.md (auto-loaded) |
| Per-sub-sprint dev handoff (in review prompt) | — | Path reference only (dev produces these AFTER review prompt is authored) |
| Code anchors (specific file:line references the agent needs to read or modify) | — | Path reference (the agent reads them on demand during work) |
| Research-agent solutions in `docs/solutions/` | — | Reference if needed; do NOT embed (proposal-tier, may be out of date relative to milestone scope decisions) |

### 9.3 Source-of-truth synchronization

`docs/sprint_objective.md` is the canonical sub-sprint contract
that the human reviews and approves. `compact/sprint-NNN-dev-prompt.md`
is its self-contained executable view. The same relationship holds
between `docs/milestone_objective.md` and `compact/M<N>-review-prompt.md`.

**Synchronization rules:**

1. The deliver-agent generates objective.md and prompt.md **in one
   pass** (objective first, then prompt as embedded view). Both are
   surfaced for human review together.
2. If the human modifies objective.md during review, the deliver-agent
   regenerates prompt.md from the modified objective before dispatch
   to dev / review.
3. If, during a sub-sprint or milestone, the contract changes
   (e.g., scope adjustment from STOP-and-surface or in-flight downgrade),
   both objective.md and prompt.md are updated together; the
   modification cadence is "objective first, prompt regenerated."
4. At sub-sprint close, the deliver-agent archives the objective.md
   to `docs/sprints/sprint-NNN-objective.md` per existing convention
   (milestone-framework §8.3). The prompt.md file at
   `compact/sprint-NNN-dev-prompt.md` stays in place as the historical
   executable view; it is NOT re-archived elsewhere unless the
   deliver-agent explicitly decides to compress the `compact/`
   directory at a future milestone.

### 9.4 Exemptions

The self-containment invariant is **not required** for:

- The research-agent's `docs/solutions/<name>.md` proposal artefact
  — it is a human-facing proposal, not an agent-execution prompt.
- The deliver-agent's own activation template
  `docs/teams/deliver-activation.md` — it is intentionally minimal
  and points to `docs/teams/deliver-agent.md` for full role definition.
- Cross-session continuity scaffolding in `docs/10-handoff.md` — it
  is structurally a session-handoff log, not an executable prompt.

### 9.5 Backwards compatibility

Pre-2026-05-26 prompts (Sprint 1 through Sprint 53; M1 through M5)
were authored under the older reference-based convention and remain
in their archived form. The self-containment invariant applies
prospectively from the next sub-sprint and the next milestone
review onward.

If a future fold-back pass discovers a historical prompt that
violates this invariant in a way that would meaningfully impair
re-running that session, the deliver-agent SHALL note the issue in
the sprint archive but SHALL NOT retroactively edit the archived
prompt (per `doc_governance.md` "Sprint archives never edited"
rule).

### 9.6 Auto-loop readiness footnote

This section is a prerequisite for the auto-evolution / auto-loop
direction proposed in `docs/solutions/auto_evolution_skill_driven_v1.md`:
a meta-agent driving sub-sprint iterations needs self-contained
prompt artefacts so that each spawned dev / review session is a
deterministic executable unit. The §9.3 synchronization rule
ensures the meta-agent can rely on prompt.md being authoritative
without needing to cross-check objective.md at session-spawn time.

A separate milestone may later evolve §9 into a richer "session
pack" concept (per the deliver-agent proposal options under
discussion 2026-05-26) — bundling prompt + context snapshots + bad
case fixtures into a single archivable directory. §9 as authored
here is the minimum invariant; the session-pack evolution is
additive on top.
