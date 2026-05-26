# Repo Constitution

This repository builds an LLM-first customer-service agent for an online
classifieds marketplace. The governing principles — LLM-vs-Runtime
ownership boundary, iteration rules, and forbidden list — are defined
in `docs/current/iteration_governance.md` §1.

The constitution and the doc-governance rules live under
`docs/current/`. Foundational architecture, phase specs, and durable
freezes live under `docs/foundational/`. Active sprint scope is named
in `docs/sprint_objective.md`; closed sprints live under
`docs/sprints/` as immutable archives. Per-task reading lists for
agents are in `docs/current/agent_context_guide.md`.

Sprint scope is decided by the human + deliver agent through
`docs/sprint_objective.md`. A sprint that touches a semantic surface
must include the **Layer-classification + anti-hardcode stanza**
defined in `docs/current/iteration_governance.md` §7. Pure infra,
docs-only, config-governance, and characterization-test sprints are
exempt from the stanza.

## Agent role registry

Every agent working in this repo shares the governance chain below.
Each role has a dedicated entry doc that defines its responsibilities,
operational procedures, and handoff format.

| Role | Entry doc | Spawned by | Primary responsibility |
|------|-----------|------------|----------------------|
| **Dev agent** (Claude Code) | `compact/sprint-NNN-dev-prompt.md` (per sub-sprint) | Human paste | Implement sub-sprint contract; run tests/eval; author handoff |
| **Deliver agent** | `docs/teams/deliver-agent.md` (via `docs/teams/deliver-activation.md`) | Human paste | Plan milestones + sub-sprints; orchestrate close; maintain bad-case suite |
| **Review agent** (Codex) | `compact/M<N>-review-prompt.md` (per milestone) | Human / deliver agent | Anti-hardcode review at milestone close; targeted PR review |
| **Research agent** | `docs/teams/research-agent.md` | Human paste | Investigate proposals + bad-case root-cause; produce deliver-consumable solutions |

Role-specific entry docs reference governance sections by `§` number;
they do not duplicate governance content. All context passes through
repo docs, not chat history.

## Constitution chain

The three governance docs below are loaded transitively for every
agent that respects this file via `@AGENTS.md`. Read them in this
order on a cold start: doc-governance first (tier model + decision
rules), agent-context-guide second (which reading list to load for the
task at hand), iteration-governance last (the operational gates that
shape what you write).

@docs/current/doc_governance.md

@docs/current/agent_context_guide.md

@docs/current/iteration_governance.md

## How to use this constitution

Every agent (dev, deliver, review, research) that loads `CLAUDE.md`
or references `@AGENTS.md` transitively loads this file, and through
this file loads the three governance docs above. That means:

- The doc front-matter schema, source-of-truth rules, and fold-back
  cadence in `doc_governance.md` apply to every docs PR.
- The per-task reading lists and the Context Pack Prompt in
  `agent_context_guide.md` apply before any non-trivial task.
- The Constitution (§1), the Failure Brief Template (§2), the Fix
  Layer Classification Checklist (§3), the Anti-Hardcode Review
  Prompt (§4), the Eval Acceptance Rules (§5 incl. §5.5 smoke
  demotion and §5.6 curated bad-case suite as human-judgment primary
  gate), the Architecture-Health Metric definitions (§6), the required
  sprint-objective stanza (§7), and the Milestone framework (§8) in
  `iteration_governance.md` apply to every change that touches the
  agent's behaviour.

Sprint-specific scope lives in `docs/sprint_objective.md` (current
sub-sprint contract) and `docs/milestone_objective.md` (current
milestone north star). Both are replaced when a new sub-sprint or
milestone is promoted. The constitution itself is not edited per
sprint; it is folded back on the cadence specified in each governance
doc's `review_cadence` front-matter field.

**Note on iteration processes** (2026-05-17 governance update): the
two input paths by which the agent's evolution is driven (Path 1
research-driven; Path 2 bad-case-driven) are operationalized in the
deliver-agent's role definition at `docs/teams/deliver-agent.md`
"Workflow inputs" section. The conceptual overview of both paths
lives in `docs/teams/collaboration-guide.md` §3. Dev and review
agents execute per `docs/sprint_objective.md` and do not need to
know which input path produced it.
