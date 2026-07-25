---
title: Agent Collaboration & Scheduling Guide
doc_tier: current-runtime
status: current
source_of_truth: this file
last_reviewed: 2026-05-26
review_cadence: per 3-5 milestones
notes: >
  Consolidated from deliver-agent role definition and
  iteration_processes_only_for_human_reference.md. This is the
  single entry point for understanding how the four agent roles
  collaborate. Loaded by human and agents alike.
---

# Agent Collaboration & Scheduling Guide

## 1. Agent Role Overview

Four agent roles collaborate under human orchestration. Each role has
a dedicated entry doc; all share the governance chain loaded via
`AGENTS.md`.

| Role | Entry doc | Spawned by | Primary responsibility |
|------|-----------|------------|----------------------|
| **Dev agent** (Claude Code) | `compact/sprint-NNN-dev-prompt.md` (per sub-sprint) | Human paste | Implement sub-sprint contract; run tests/eval; author handoff |
| **Deliver agent** | `docs/teams/deliver-agent.md` (via `docs/teams/deliver-activation.md`) | Human paste | Plan milestones + sub-sprints; orchestrate close; maintain bad-case suite |
| **Review agent** (Codex) | `compact/M<N>-review-prompt.md` (per milestone) | Human / deliver agent | Anti-hardcode review at milestone close; targeted PR review |
| **Research agent** | `docs/teams/research-agent.md` | Human paste | Investigate proposals + bad-case root-cause; produce deliver-consumable solutions |

### Responsibility boundaries

- **Research agent** proposes; does NOT decide scope or write code.
- **Deliver agent** plans and orchestrates; does NOT write business code or do code review.
- **Dev agent** implements; does NOT expand scope beyond the sub-sprint contract.
- **Review agent** reviews; does NOT edit code or expand scope.
- **Human** selects proposals, approves contracts, makes final close decisions.

## 2. Context Passing Rules

Agents do NOT share chat history. All context passes through repo
docs, eval results, git diff, handoffs, and review findings.

| Context type | Carrier | Owner |
|-------------|---------|-------|
| Cold-start state | `docs/10-handoff.md` §0 (structured table) + §1 (narrative) | Deliver agent updates at close |
| Current milestone | `docs/milestone_objective.md` | Deliver agent drafts; human approves |
| Current sub-sprint | `compact/sprint-NNN-dev-prompt.md` — **the prompt is the contract** (prompt-artifact-rules §9.3.1, amended 2026-07-26). `docs/sprint_objective.md` is optional scoping scratch, not a second contract | Deliver agent drafts; human approves |
| Review brief | `compact/<review-scope>-review-prompt.md`, authored **after** delivery against the actual diff + handoff (§9.3.2). Findings are adjudicated before becoming work (§9.3.3) | Deliver agent authors |
| Sub-sprint result | `docs/sprints/sprint-NNN-handoff.md` | Dev agent authors |
| Review findings | `docs/codex-findings.md` | Review agent authors |
| Backlog | `docs/action_bank.md` | Deliver agent maintains R-items |
| Bad-case suite | `eval_interactive/case_specs/bad_cases/` | Deliver agent + human co-maintain |
| Governance chain | `AGENTS.md` → `doc_governance.md` → `agent_context_guide.md` → `iteration_governance.md` | Auto-loaded for all agents |

## 3. Two Input Paths

The agent's evolution is driven by two distinct input paths. Both
converge on the same downstream loop (research-agent proposal →
deliver-agent milestone planning → dev/review/close per
`docs/current/process/milestone-framework.md` §8).

### Path 1 — Research-driven (forward-looking)

**Trigger**: Human has an architectural idea, a strategic direction,
or wants to consume a matured R-item from `docs/action_bank.md`.

```
human idea / strategic direction
  ↓
human briefs research agent(s)
  → input: idea statement + codebase area pointers
  ↓
research agent(s) produce proposal
  → output: docs/solutions/<name>.md
  ↓
human reviews + selects proposal
  ↓
selected proposal → deliver agent
  ↓
deliver agent plans milestone / sub-sprint per process/milestone-framework.md §8
  ↓
[downstream loop: dev → review → close]
```

### Path 2 — Bad-case-driven (backward-looking)

**Trigger**: Real-session bad case observed (human use / colleague /
sprint-surfaced).

```
real session surfaces a failure
  ↓
human + deliver agent triage: load-bearing? (process/badcase-lifecycle.md §5.6 criteria)
  → if NO: discard / log only
  → if YES: proceed
  ↓
research agent(s) — bad-case mode
  → MUST produce all 4: root-cause + coverage check
    + compounding analysis + deliver-consumable proposal
  ↓
human reviews proposal
  ↓
deliver agent encodes bad case + routes to 4-route fit:
  (a) fits current milestone
  (b) fits future milestone
  (c) new R-item needed
  (d) emergency (Tier-0 only)
  ↓
[downstream loop: dev → review → close]
```

### Anti-patterns

- Skipping the research-agent step on Path 2 to "save time"
- Treating Path 1 proposals as binding (proposals are suggestions)
- Path 2 without coverage check (non-optional)
- Encoding every bad case as `core` tier (bloats regression suite)
- Mid-milestone scope expansion (wait for next planning round per `process/milestone-framework.md` §8.5)
- Fixing symptoms without fixing causes (compounding-effect analysis is non-optional)

## 4. Milestone Loop

```
Human gives scope / direction
  ↓
Deliver agent drafts:
  - milestone_objective.md
  - first sub-sprint contract (sprint_objective.md)
  - dev prompt (compact/sprint-NNN-dev-prompt.md)
  ↓
Human reviews + approves
  ↓
Dev agent implements sub-sprint 1
  → runs tests + eval + authors handoff
  ↓
Deliver agent + human assess sub-sprint:
  A. Clean PASS → next sub-sprint
  B. Findings need fix → fix-iteration sub-sprint
  C. In-flight downgrade → stop milestone, replan
  D. Acceptance bar met early → skip to milestone close
  ↓
... repeat sub-sprints 2..N ...
  ↓
Milestone close trigger
  ↓
Review agent does milestone-level review (process/milestone-framework.md §4.3)
  → updates docs/codex-findings.md
  ↓
Deliver agent + human:
  1. Manual review bad-case suite (primary gate per process/badcase-lifecycle.md §5.6)
  2. Classify codex findings:
     A. No blockers → close milestone
     B. P0/P1 in scope → fix-iteration
     C. Scope expansion → push back to review agent
     D. Multiple rounds fail → human review required
  ↓
Archive + plan next milestone
```

## 5. Acceptance Gates

Canonical gate definitions: `iteration_governance.md` §5.5 (hard-gate
list + smoke demotion) and `docs/current/process/badcase-lifecycle.md`
§5.6 (curated bad-case primary gate). In short — curated bad-case suite
manual review is the **HARD primary gate**; Codex §4.1 kernel, Java
no-regression, safety floor, and grounding floor are **HARD gates**;
smoke composite_score / pass-rate and architecture-health metrics are
**OBSERVATION** (recorded, do not block close).

## 6. File Topology

```
AGENTS.md (constitution — auto-loaded)
  ↓ loads
docs/current/{doc_governance, agent_context_guide, iteration_governance}.md

docs/teams/
  ├── collaboration-guide.md          (this file)
  ├── deliver-agent.md                (deliver agent role definition)
  ├── deliver-activation.md           (deliver agent cold-start template)
  └── research-agent.md               (research agent role guide)

docs/current/
  ├── iteration_governance.md         (Layer A: §1 Constitution, §3 fix-layer, §5 eval-accept, §7 stanza)
  ├── process/                        (Layer B — on-demand process docs)
  │   ├── milestone-framework.md      (§8 milestone framework + §4.3 Codex cadence)
  │   ├── prompt-artifact-rules.md    (§9 dev/review prompt self-containment)
  │   ├── badcase-lifecycle.md        (§5.6 bad-case suite + lifecycle)
  │   └── architecture-health-metrics.md (§6 metric definitions)
  ├── governance-examples.md          (§2 + §7.2 worked examples)
  ├── anti-hardcode-review-kernel.md  (§4.1 nine-question kernel)
  ├── doc_governance.md               (tier model + decision rules)
  └── agent_context_guide.md          (per-task reading lists)

compact/                              (working directory for per-sprint artifacts)
  ├── sprint-NNN-dev-prompt.md        (deliver agent creates per sub-sprint;
  │                                    IS the contract, self-contained;
  │                                    see process/prompt-artifact-rules.md §9.3.1)
  └── <review-scope>-review-prompt.md (deliver agent creates AFTER delivery,
                                       scoped by the actual diff + handoff;
                                       see process/prompt-artifact-rules.md §9.3.2)

docs/milestone_objective.md           (current milestone contract)
docs/sprint_objective.md              (current sub-sprint contract)
docs/10-handoff.md                    (cross-session state §0/§1/§2)
docs/action_bank.md                   (R-item backlog)
docs/codex-findings.md               (review agent findings)
```
