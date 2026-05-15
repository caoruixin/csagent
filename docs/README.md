# Docs system

This repo's `docs/` tree mixes several kinds of writing produced over many
sprints: foundational design specs, current runtime contracts, sprint
archives, future-looking proposals, diagnostics, and runbooks. The same word
("contract", "policy", "phase") can mean different things depending on which
tier a document belongs to. This README sets the rules so humans and agents
can quickly tell which docs are authoritative, which are historical, and
which describe future intent.

The detailed governance rules (front matter schema, decision rules, fold-back
cadence, PR checklist) live in
[`current/doc_governance.md`](current/doc_governance.md). Read that file when
you are reconciling docs against code or deciding how to mark a document.

When you need to brief an agent before coding or analysis, use
[`current/agent_context_guide.md`](current/agent_context_guide.md). It maps
common task types to the docs and code paths worth loading first, and ships a
reusable Context Pack Prompt.

The current runtime and tool contracts live in `docs/current/`:

- [`current/runtime_contract.md`](current/runtime_contract.md) —
  delivered runtime: components, phase model, PhasePlan, tool-use
  model, allowed-tool enforcement, projection fields, and known
  deviations from older specs.
- [`current/customer_service_tool_spec_v0_3.md`](current/customer_service_tool_spec_v0_3.md)
  — code-grounded tool spec (agent-visible and runtime-only). Supersedes
  `customer_service_tool_spec_v0_2.md` / `.yaml`, which are now stamped
  as archived/historical.

## Directory layout

The `docs/` tree is being reorganized so each tier has a home directory.
The current layout:

- `docs/current/` — current runtime / durable-connective contracts.
  Entry points: `runtime_contract.md`,
  `customer_service_tool_spec_v0_3.md`, `faq_grounding_contract.md`,
  `doc_governance.md`, `agent_context_guide.md`. Anything here is
  expected to track reality.
- `docs/foundational/` — durable architecture, business intent,
  normative freezes, foundational specs (phase0–phase5, BRD, PRD,
  agent tech spec). Edited only on the fold-back cadence.
- `docs/proposals/` — forward-looking or partially-implemented designs
  (autoloop, skill orchestration, problem-retrieval pgvector,
  interactive case-spec, UC/topic/subject alignment, Java guard
  prompt flexibility, handover orchestrator). Read as "what we plan",
  not "what runs".
- `docs/diagnostics/` — audits, post-mortems, and finding logs
  (prompt-context projection audit, fix-layer taxonomy, codex
  findings).
- `docs/runbooks/` — operational references (admin guide, Salesforce
  part spec, A/B testing policy).
- `docs/sprints/` — immutable per-sprint archives. Never edited after
  the sprint closes.
- `docs/archive/` — historical snapshots that fall outside the
  per-sprint archive (see `docs/archive/current-docs/README.md`).
- `docs/bak/` — local backup copies of older spec versions retained
  for diff history. Not part of any active tier; do not link from
  current docs.
- `docs/design-extract/` — generated design-extraction artefacts
  (HTML/CSS/screenshots) used by the design tooling. Not part of any
  active tier; treated as a build/data artefact directory.
- `docs/` (top level) — files not yet sorted into one of the
  directories above; see TODO_REVIEW below.

When you cite a doc, use its tier directory in the path so readers can
tell which tier they are reading at a glance.

## TODO_REVIEW (directory reorganization follow-ups)

The first reorganization pass placed only docs whose tier was
unambiguous. Several files were left at the top level pending a
governance decision — they fit more than one tier or carry working-file
semantics that need to be confirmed before moving:

- `07-engineering-constraints.md` — engineering constraints: likely
  `foundational/`, but partially overlaps with `runbooks/` for
  non-functional / privacy posture. Decide tier and move.
- `10-handoff.md`, `action_bank.md`, `sprint_objective.md` —
  latest-only working files (see `archive/current-docs/README.md`).
  Decide whether they belong in `current/` (working state) or stay
  at the top level as a separate "working files" zone. Either way,
  capture the decision in `current/doc_governance.md`.
- `case-data-stat.md`, `customer_service_agent-Common-Phrases.md`,
  `FAQ-knowledge_include_help_url.csv`,
  `platform_api_detailed_reference.md` — reference / dataset
  material. Likely a future `docs/reference/` tier; not created
  this pass to avoid speculative scaffolding.
- `current_eval_baseline.md` — eval baseline; mixes current-state
  numbers and diagnostic observations. Decide whether to split into
  `current/` (live baseline) and `diagnostics/` (point-in-time
  observations).
- `customer_service_agent_delivery_workbook.md`,
  `customer_service_agent_eval_spec.md` — likely `foundational/`;
  confirm against existing phase docs first.
- `customer_service_tool_spec_v0_2.md` /
  `customer_service_tool_spec_v0_2.yaml` — superseded by
  `current/customer_service_tool_spec_v0_3.md`. Body intact per the
  current PR's scope rules; consider moving to `archive/` (or a
  `foundational/superseded/` subdirectory) in a follow-up, leaving
  the `superseded_by:` front matter pointer intact.
- `fixed_script_library_v1.md` — foundational reference for the
  script library; pending the related TODO_DECISION in
  `current/customer_service_tool_spec_v0_3.md` about whether the
  fixed script library is a runtime capability or a tool.
- `phase4_coding_agent_implementation_packet.md` — sister to
  `foundational/phase4_demo_coding_agent_implementation_packet.md`.
  Move to `foundational/` once roles of the two phase4 packets are
  documented.
- `release_gate.md` — release-gate policy; tier is between
  `runbooks/` and `foundational/`. Decide and move.
- `runtime_freeze_and_risk_policy.md` — foundational-leaning risk
  policy. Likely `foundational/`; confirm.
- **Doc-body path references** — the active non-archive docs were
  swept in this PR for stale `docs/<file>.md` references that point
  at moved files, and updated to the new tier paths
  (`foundational/`, `proposals/`, `diagnostics/`, `runbooks/`).
  Sprint archives (`docs/sprints/`, `docs/archive/`) intentionally
  still carry the old paths because those are immutable historical
  records. A future fold-back pass may catch any stragglers.
- `archive/current-docs/` — directory of dated snapshots (the
  earliest is `pre-sprint-9`). The reorg request suggested renaming
  to `archive/pre-sprint-9-snapshot/`, but the directory's own
  README documents it as a general-purpose snapshot home that also
  holds pre-sprint-8.2 content, so the rename was deferred. Decide
  whether to (a) rename and accept the loss of the per-snapshot
  date prefixes inside the directory, (b) split into one directory
  per snapshot date, or (c) leave the directory as-is and clarify
  its scope in `current/doc_governance.md`.

## Source of truth, in order

1. **Code and tests** — for any *delivered* runtime behavior. If the running
   server, UI, eval harness, or scripts disagree with a doc about what
   actually ships today, the code is authoritative.
2. **`docs/current/*`** — for current runtime contracts, connective rules
   that are hard to recover from code alone (vocabulary alignment, governance
   decisions, cross-module conventions), and any long-lived agreement that
   describes today's behavior at a level above any single file.
3. **Foundational design docs** at `docs/foundational/` (e.g. the
   phase0–phase5 specs, BRD, PRD, agent tech spec) — for durable
   architecture and business intent. They explain *why* the system is
   shaped the way it is. They are not necessarily a faithful description
   of last-week's code; that is by design.
4. **Proposal docs** — for *future* intended behavior or design directions
   that have not fully landed. They are read as "what we plan / considered",
   not "what runs".
5. **Sprint archive docs** under `docs/sprints/` — immutable history. They
   record what each sprint planned, delivered, deferred, and reviewed. They
   are never the source of truth for current behavior; they are the source
   of truth for how we got here.

If two docs of different tiers disagree, the higher tier in this list wins
for the question being asked. For "what runs today" that is code; for "what
were we trying to build" that is foundational; for "what did we ship in
sprint N" that is the sprint archive.

## Five-tier doc model

Every doc in this repo fits into one of five tiers. Future PRs will mark
existing docs with explicit front matter; this README defines the tiers.

1. **Foundational** — durable architecture, domain model, business intent,
   normative freezes, long-lived design decisions. Examples of files that
   live in this tier: phase0–phase5 specs, BRD, PRD, tool specs. They evolve
   slowly and intentionally.
2. **Current runtime / durable connective** (`docs/current/*`) — short,
   maintained docs that describe the live runtime contract or cross-cutting
   conventions. This is where governance, vocabularies, and reconciliation
   notes live.
3. **Sprint archive** (`docs/sprints/*` and `docs/archive/*`) — frozen
   per-sprint objective + handoff + review. Never edited after the sprint
   closes.
4. **Proposal / forward-looking** — design notes, exploration documents,
   alignment proposals, and orchestration plans that describe behavior the
   system does not yet fully implement.
5. **Diagnostic / runbook / reference** — audits, post-mortems, operations
   guides, FAQ datasets, platform API references, and other lookup material.
   They support the other tiers but are not contracts.

## Update rules per tier

- **Foundational**: edit when delivered behavior or business intent changes
  enough that the *intent* described here has actually shifted, or during a
  scheduled fold-back (see governance doc). Do not patch foundational docs
  every sprint; let sprint archives carry the deltas.
- **Current runtime / durable connective**: edit eagerly. These docs are
  expected to track reality. Stale entries here are bugs.
- **Sprint archive**: do not edit after the sprint closes. See "Do not edit
  sprint archives" below.
- **Proposal / forward-looking**: keep the document, even if a different
  approach ships. Mark it with `status: proposal`, `deferred`, `partial`, or
  `superseded_by:` rather than deleting it. The reasoning has long-term
  value.
- **Diagnostic / runbook / reference**: edit when the underlying system
  changes. Diagnostics from past investigations should be preserved, dated,
  and marked `diagnostic` rather than rewritten in place.

## Code ahead of docs

If running code has been reviewed and is the *delivered* behavior, and a doc
describes something different, **update the doc**. Do not revert code to
match a stale doc unless the code is actually wrong on its own merits.

When you do this, prefer reverse-updating the lowest-tier doc that captures
the change (a `docs/current/*` note, or a fold-back into a foundational doc
during a scheduled review). Sprint archives are not edited; instead the next
sprint's handoff or a `docs/current/*` reconciliation note explains the
delta.

## Docs ahead of code

If a doc describes behavior the code does not yet exhibit, **preserve the
doc**. Do not delete future design content because today's runtime hasn't
caught up. Mark the document so a reader can tell:

- `status: proposal` — design only, not implemented.
- `implementation_status: partial` — some pieces shipped, others did not.
- `status: deferred` — explicitly paused.
- `superseded_by: <path>` — replaced by another doc; keep the original for
  history.

When in doubt, prefer marking over deleting. The cost of keeping a clearly
labelled forward-looking doc is small; the cost of losing the reasoning is
high.

## How to ask agents for context

When you ask an agent (Claude, Codex, or another) to plan, review, or change
something in this repo, use the per-task reading lists and the **Context
Pack Prompt** in
[`current/agent_context_guide.md`](current/agent_context_guide.md). That
guide tells the agent which docs and code paths to load first for a given
task type, and asks it to return an explicit source-of-truth decision plus
known doc-status warnings before it starts coding.

This matters because the doc tree is large and many tiers overlap on the
same vocabulary. Without a context pack, an agent will often anchor on
whichever doc it loads first, which is rarely the right tier for the
question.

## Do not edit sprint archives

Sprint archive docs (`docs/sprints/sprint-NNN-*.md`,
`docs/archive/current-docs/*`) are immutable once the sprint is closed. They
record what was planned, what shipped, what was deferred, and how the
sprint was reviewed at that point in time.

Do not:

- rewrite a closed sprint's handoff to match later behavior,
- "fix" a sprint objective after the fact,
- delete a sprint review even if its conclusions were superseded.

If a closed sprint's content is now misleading, capture the correction in a
`docs/current/*` note, the next sprint's handoff, or a fold-back into the
relevant foundational doc — not by editing the archive.
