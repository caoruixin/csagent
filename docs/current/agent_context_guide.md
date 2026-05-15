---
title: Agent context guide
doc_tier: current-runtime
status: current
implementation_status: implemented
source_of_truth: this file
last_reviewed: 2026-05-10
review_cadence: every 3-5 sprints
supersedes: []
superseded_by: null
notes: >
  Per-task reading lists for briefing agents (Claude Code, Codex, others)
  and a reusable Context Pack Prompt. Tier and status definitions live in
  `doc_governance.md`. The repo-level overview lives in `../README.md`.
---

# Agent context guide

The `docs/` tree is large and many tiers reuse the same vocabulary
("contract", "policy", "phase", "handover", "fix layer"). An agent that
loads docs in a near-arbitrary order will tend to anchor on whichever doc
arrives first, which is often the wrong tier for the question being asked.

This guide does two things:

1. For common task types, lists which docs and code areas to load *first*
   so the agent starts in the right tier.
2. Ships a reusable **Context Pack Prompt** that asks the agent to return
   an explicit source-of-truth decision, doc-status warnings, and known
   risks before it starts coding.

The reading lists below are starting points, not exhaustive bibliographies.
Treat the named files as illustrative — they are the docs that exist today
in this repo for the relevant subject area. They are not yet marked with
front matter; assume any of them may be partially or fully out of date and
verify against code before relying on a specific implementation claim. Tier
labels in parentheses are the tier the doc *appears* to belong to from its
content; the formal marking will land in a later PR.

## Task type → reading list

### Runtime, phase machine, drift

For changes to the runtime loop, phase transitions, drift detection, or
risk gates.

- `foundational/phase0_normative_freeze.md`,
  `foundational/phase3_detailed_technical_design.md` (foundational) —
  durable architecture and intent.
- `runtime_freeze_and_risk_policy.md` (foundational / durable-connective;
  still at top level — see TODO_REVIEW in `../README.md`) — risk policy
  intent.
- `diagnostics/prompt_context_projection_audit.md` (diagnostic) — known
  drift areas observed in past audits.
- Recent sprint archives under `docs/sprints/` whose objective names
  "runtime", "alignment", "freeze", or "drift" — for the most recent
  delta.
- Code: the runtime / orchestrator modules under `server/`. Treat the
  code as the source of truth for current phase behavior.

### Tool schema, tool policy

For tool-call schema changes, new tools, policy on which tools may run
where.

- `customer_service_tool_spec_v0_2.md` and the matching
  `customer_service_tool_spec_v0_2.yaml` (foundational, **superseded** —
  still at top level for history; see `customer_service_tool_spec_v0_3.md`
  in this directory for the current spec) — durable schema intent.
  Verify the YAML against actual tool definitions in code; the YAML is
  the design intent, not necessarily a faithful mirror of what ships
  today.
- `foundational/phase3_detailed_technical_design.md` (foundational) —
  for the surrounding architecture the tools plug into.
- `foundational/customer_service_agent_tech_spec.md` (foundational) —
  interface-level context.
- Recent sprint archives whose objective names "tool" or "trace
  observability".
- Code: the tool schema and dispatch path under `server/`.

### FAQ retrieval, grounding

For FAQ retrieval, grounding rules, evidence lineage, knowledge index
work.

- `current/faq_grounding_contract.md` (current canonical FAQ
  grounding contract) — grounding contract definition (term
  distinctions, output classes, soft diagnostics).
- `proposals/problem_retrieval_solution_plan_pgvector.md` (proposal) —
  retrieval plan; verify which parts shipped before treating any
  detail as authoritative.
- `FAQ-knowledge_include_help_url.csv` (reference; still at top level)
  — the dataset.
- Recent sprint archives whose objective names "faq", "kb", "evidence",
  or "retrieval".
- Code: retrieval and grounding modules under `server/` and `data/`.

### Intake, handover, Salesforce

For case intake, handover orchestration, and Salesforce integration.

- `10-handoff.md` (working file; still at top level) — durable handoff
  contract.
- `proposals/handover_orchestrator_design.md` (proposal — design
  freeze for the future single-owner handover orchestrator).
- `runbooks/salesforce-part-spec.md` (runbook / reference).
- `case-data-stat.md` (reference; still at top level) — case data
  background.
- Recent sprint archives whose objective names "handover", "intake", or
  "exactly-once".
- Code: intake and handover modules under `server/`.

### Privacy, PII, trace

For privacy, PII handling, and trace/observability.

- `07-engineering-constraints.md` (foundational; still at top level —
  see TODO_REVIEW in `../README.md`) — engineering constraints
  including privacy posture.
- `diagnostics/prompt_context_projection_audit.md` (diagnostic) —
  observed projection / leakage issues.
- Sprint archives whose objective names "trace observability" or "tool
  contract trace".
- Code: tracing, redaction, and logging paths under `server/` and
  `logs/`.

### Eval, governance

For evaluation harness changes, eval datasets, governance of release
gates.

- `foundational/phase5_evaluation_design.md` (foundational) — durable
  eval design.
- `customer_service_agent_eval_spec.md` (foundational; still at top
  level — see TODO_REVIEW in `../README.md`) — eval intent at the
  agent level.
- `current_eval_baseline.md` (current-runtime / diagnostic; still at
  top level — see TODO_REVIEW in `../README.md`) — last recorded
  baseline. Verify the date before trusting any specific number.
- `release_gate.md` (runbook-leaning; still at top level — see
  TODO_REVIEW in `../README.md`) — release gate policy.
- `runbooks/abtesing-policy.md` (runbook) — A/B testing policy.
- Code and configs under `eval/` and `eval_interactive/`.

### Production readiness

For production-readiness tasks: admin operations, runbooks, deployment.

- `runbooks/admin-guide.md` (runbook) — operational reference.
- `release_gate.md` (runbook-leaning; still at top level — see
  TODO_REVIEW in `../README.md`) — gate policy.
- `07-engineering-constraints.md` (foundational; still at top level —
  see TODO_REVIEW in `../README.md`) — non-functional constraints.
- `platform_api_detailed_reference.md` (reference; still at top
  level) — platform API surface.
- Code: deploy / scripts under `scripts/` and config files at the repo
  root.

### Proposal exploration

For exploring or extending a proposal, or evaluating whether a proposal
should be promoted.

- `proposals/autoloop_design.md` (proposal).
- `proposals/skill_orchestration_candidates.md` (proposal).
- `proposals/proposal_uc_topic_subject_alignment.md` (proposal).
- `proposals/interactive_case_spec_generation_plan.md` (proposal).
- `proposals/java_guard_prompt_flexibility_design.md` (proposal).
- The relevant foundational doc for the surrounding subsystem (use the
  task-type sections above).
- Sprint archives that mention the proposal by name — they often record
  what was tried, deferred, or partially shipped.

For any proposal, before treating it as a contract, verify against code
whether the described behavior is `implemented`, `partial`, or
`not_started`. Proposals are forward-looking by design.

## Context Pack Prompt

When briefing an agent on a non-trivial task in this repo, include a
"context pack" step before any plan or code. Paste the prompt below,
adapted to the task. The agent should answer **before** producing a plan
or diff.

```
You are working in this repo. Before proposing a plan or any code change,
build a context pack for the task described below. Do not start coding.

Task: <one-paragraph description of what we want to do>

Read the docs/README.md and docs/current/doc_governance.md first so you
understand the tier model and source-of-truth rules. Then use
docs/current/agent_context_guide.md to find the right reading list for
this task type. Sample the docs and code paths it suggests; you do not
need to read everything end-to-end, but you must read enough to answer
the questions below.

Return your context pack in this exact shape:

1. Relevant docs
   - For each doc you actually read or sampled, give: path, the tier you
     believe it belongs to, your best guess at its status
     (current / proposal / partial / superseded / unknown), and one line
     on why it is relevant to this task.

2. Relevant code paths
   - List the files and directories you sampled and the specific
     functions, classes, or config keys that govern the behavior in
     scope. Cite paths exactly.

3. Doc status warnings
   - For any doc where you suspect drift from code, say so and name the
     specific drift. If a doc looks forward-looking ("intended to",
     "will"), call that out. If two docs disagree on the same point,
     name the disagreement.

4. Source-of-truth decision
   - For the question this task is trying to answer, state which artifact
     is authoritative: a specific code path, a specific doc, or a
     combination. Justify the choice in one or two sentences using the
     rules in doc_governance.md.

5. Implementation status
   - For the behavior in scope, classify as: implemented / partial /
     not_started / historical / unknown. Cite the code path or
     observation that supports the classification.

6. Risks before coding
   - List the top three to five risks of changing this area: hidden
     coupling, stale docs that other readers may rely on, behavior gaps,
     proposals that may be affected, sprint archives whose deltas are
     load-bearing. Be specific; "be careful with X" is not a risk.

Do not edit any files yet. Do not edit `docs/sprints/*` or
`docs/archive/*` under any circumstance. Once the context pack is
returned, wait for confirmation before producing a plan or diff.
```

The context pack is cheap to produce and disproportionately reduces the
chance of an agent anchoring on the wrong tier. Use it for any task that
crosses module boundaries, touches a `current-runtime` contract, or
involves a doc that may be forward-looking.
