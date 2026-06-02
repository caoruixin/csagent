---
title: Agent Governance & Collaboration Framework — Template Manifest
doc_tier: durable-connective
status: current
implementation_status: partial
source_of_truth: this file
last_reviewed: 2026-06-02
review_cadence: per fold-back of the source project's governance docs
notes: >
  Project-agnostic skeleton extracted from the csagent governance split
  (Layer A/B/C/D). This README is the MANIFEST: it defines the four
  layers, the target file tree, the placeholder convention, and the
  instantiation checklist. It is the only artifact in this directory so
  far — every other file named in the tree below is NOT YET CREATED.
  The csagent repo that this was abstracted from is the worked reference
  instantiation (see §8).
---

# Agent Governance & Collaboration Framework — Template

A portable, project-agnostic skeleton for running an LLM-agent project
under explicit governance: a small always-loaded **constitution**, an
on-demand **process** layer, retention-governed **state ledgers**, and
generated self-contained **prompt artifacts**.

It is abstracted from a working multi-agent setup (dev / deliver /
review / research agents collaborating only through repo docs — never
shared chat history) and is designed to be copied into a new project,
have its `<<PROJECT: …>>` slots filled, and run.

> **Status:** This README is the seed. Only this file exists today. The
> files named in §2 are the planned build-out; create them on demand,
> sourcing each from the corresponding live doc in the reference
> instantiation (§8). Treat this manifest as authoritative for the
> *structure*; treat the reference repo as authoritative for the
> *current mechanics text* until each template file is genericized.

## 1. The four layers

| Layer | What it holds | Load semantics | Churn | Portability posture |
|---|---|---|---|---|
| **A — Constitution** | Timeless rules: objective, primary principle, the *X-owns / Y-owns* responsibility boundary, the iteration rule, the evaluation rule, the forbidden list, the fix-layer classification, the sprint stanza. | **Always-loaded** — the root manifest @-includes it. Keep small (target ≤ ~20 KB). | Rare (folded back on cadence, not per sprint). | Strong **shape**, heavy **placeholders**: the wording is structural; the *content* (domain objective, the layer taxonomy, the forbidden items) is project-specific. |
| **B — Process** | Cadence + collaboration mechanics: doc-governance (tier model, front-matter schema, retention rules), milestone framework, prompt-artifact self-containment rules, bad-case lifecycle, anti-hardcode review kernel, architecture-health metric definitions, agent-context reading lists, and the **role registry** (dev / deliver / review / research) + the context-passing-via-repo-docs model. | **On-demand**, role-keyed (loaded when a task touches that surface). | Occasional (project-tunable). | **Mostly portable verbatim**; a few skeletons (reading lists, metric set, kernel wording). |
| **C — State Ledgers** | The live, retention-governed working state: the backlog ledger + its closed archive, the cross-session handoff (cold-start table / narrative / archive index), the current milestone + sub-sprint contracts, the review findings file. | On-demand; regenerated every cycle. | High (every sub-sprint / milestone). | **Structure + retention procedure portable**; contents 100% project-specific — ship as empty skeletons. |
| **D — Prompt Artifacts** | Generated, self-contained executable session prompts (one per dev sub-sprint, one per review milestone). The *generation rules* live in Layer B; Layer D ships the **prompt skeletons**. | Generated per session; pasted to boot a fresh agent. | Per session. | Skeletons portable; the generated instances are project-specific. |

**Cross-cutting — the root manifest/loader** (`AGENTS.template.md`): a thin
file that @-includes **Layer A always** and *names* B/C/D for on-demand
loading. This is the single mechanism that keeps the always-loaded
context surface = **Layer A + the root manifest only**.

> The role docs (`B-process/roles/*`) are **Layer B**, not a fifth layer —
> they define *who runs the loop*, which is process, not constitution.

## 2. Target file tree

Copy this whole directory into a new project, then fill placeholders.
**Only `README.md` exists today**; the rest is the planned build-out.

```
framework-template/
  README.md                         # THIS manifest (exists)
  INSTANTIATE.md                    # step-by-step bootstrap (TBD)
  AGENTS.template.md                # root loader; @-includes Layer A; names B/C/D  (TBD)
  A-constitution/
    constitution.template.md        # objective / primary principle / X-owns / Y-owns /
                                     #   iteration rule / eval rule / forbidden /
                                     #   fix-layer-checklist skeleton / stanza  [heavy placeholders] (TBD)
  B-process/
    doc-governance.md               # PORTABLE-VERBATIM: tier model + front-matter schema + retention (TBD)
    milestone-framework.md          # PORTABLE-VERBATIM (TBD)
    prompt-artifact-rules.md        # PORTABLE-VERBATIM (TBD)
    badcase-lifecycle.md            # PORTABLE-VERBATIM mechanics (TBD)
    anti-hardcode-review-kernel.template.md   # kernel shape; project tunes wording (TBD)
    architecture-health-metrics.template.md   # metric-definition skeleton (TBD)
    agent-context-guide.template.md # reading-list + context-pack prompt skeleton (TBD)
    roles/
      collaboration-guide.md        # PORTABLE role model + context-passing principle (TBD)
      deliver-agent.md  research-agent.md  review-agent.md  activation-template.md  (TBD)
  C-state-ledgers/
    action-bank.template.md         # header + section skeleton + retention-sweep procedure [empty] (TBD)
    action-bank-archive.template.md # closed-index skeleton (TBD)
    handoff.template.md             # cold-start / narrative / archive-index skeleton (TBD)
    milestone-objective.template.md # schema skeleton (TBD)
    sprint-objective.template.md    # schema + stanza skeleton (TBD)
    review-findings.template.md     # header scaffold (TBD)
  D-prompt-artifacts/
    dev-prompt.template.md          # self-contained dev-session skeleton (TBD)
    review-prompt.template.md       # self-contained review-session skeleton (embeds kernel slot) (TBD)
  examples/
    csagent-reference.md            # pointer to the live reference instantiation (NOT a copy) (TBD)
```

## 3. Placeholder convention

- Every project-specific slot is marked `<<PROJECT: short description>>`.
- A file containing **zero** `<<PROJECT:` markers is **portable-verbatim**:
  copy it unchanged.
- A file containing markers is a **skeleton**: copy it, then replace each
  marker. The reference instantiation (§8) shows one filled example of
  every marker.
- Instantiation is "done" when `grep -r '<<PROJECT:'` over the project
  returns nothing.

## 4. Portable vs project-specific

| Classification | Items |
|---|---|
| **PORTABLE-VERBATIM** (copy unchanged) | doc-governance tier model + retention rules; milestone framework; prompt-artifact rules; bad-case lifecycle mechanics; collaboration-guide role model + context-passing; deliver / research / review role definitions; state-ledger retention-sweep procedure; handoff section structure; milestone/sprint objective *schemas*; the root-manifest structure. |
| **PORTABLE-SKELETON** (shape portable, fill content) | the Constitution spine; the fix-layer classification *pattern* (the layer **set** is project-specific); the anti-hardcode kernel (question wording tunable); the architecture-health metric set; the agent-context reading lists; the root loader's @-include list. |
| **PROJECT-SPECIFIC** (instantiate or leave empty) | domain objective + primary principle + the concrete X-owns/Y-owns lists + forbidden items; the fix-layer taxonomy; Tier-0 / risk invariants; eval surfaces + baselines + bad-case corpus; tool/runtime specifics; **all ledger contents**. Domain/foundational docs (phase specs, tool/eval/runtime specs) are **outside** the framework entirely. |

The hardest abstraction is **Layer A**: it carries the most
project-specific *content* but must remain the cleanest *skeleton*. The
fix-layer taxonomy and the X-owns/Y-owns split are architecture-dependent,
so they appear as placeholders with the reference values shown as an
example.

## 5. Instantiation checklist (summary; full steps go in INSTANTIATE.md)

1. Copy `framework-template/` into the new project.
2. Fill `A-constitution/constitution.template.md`: domain objective,
   primary principle, the X-owns/Y-owns boundary, the fix-layer set, the
   forbidden list, the stanza fields.
3. Wire `AGENTS.template.md` → the project's root agent file; confirm it
   @-includes **only** Layer A (+ doc-governance + agent-context guide if
   you choose to always-load them) and *names* B/C/D for on-demand use.
4. Adopt Layer B verbatim; tune the skeleton files (reading lists,
   metric set, kernel wording).
5. Initialize empty Layer C ledgers from the skeletons.
6. Generate the first Layer D dev prompt from `dev-prompt.template.md`
   for the first sub-sprint.
7. Run `grep -r '<<PROJECT:'` → must be empty before first dev session.

## 6. Acceptance criteria (for the eventual full template)

1. **Bootstrap test:** a fresh project stands up from the copied template
   + filled placeholders, with no reference back to the source repo.
2. **Small always-loaded surface:** only Layer A + the manifest are
   @-included; B/C/D load on demand.
3. **No leakage:** every PORTABLE-VERBATIM file contains zero source-project
   identifiers (grep for domain-specific tokens → empty).
4. **Completeness gate:** `grep -r '<<PROJECT:'` over an instantiated
   project returns empty.
5. **Round-trip fidelity:** the reference instantiation, re-derived from
   the template + its placeholder values, is structurally equivalent to
   the live docs (no mechanics lost).
6. **Separation:** the skeleton (template) and the worked example ship
   no domain content into each other.

## 7. Risks / non-goals

**Risks**
- **n=1 over-generalization** — abstracted from one project; some
  source-project assumptions may be baked in as "portable" when they are
  domain-shaped. Mark uncertain abstractions; validate against §8.
- **Template ↔ live drift** — this is a snapshot; re-sync portable files
  on the source project's doc-governance fold-back cadence rather than
  forking mechanics.
- **Layer-A abstraction leakage** — easy to over-empty (useless) or
  over-specify (source-bound); expect the most iteration here.

**Non-goals**
- Not refactoring the source project's live docs (they remain the
  reference instantiation).
- Not extracting to a separate repo yet (in-repo first).
- Not building prompt-artifact *generator* tooling (ship skeletons +
  rules, not automation).
- Not templating domain/foundational docs.
- Not migrating live ledger contents.

## 8. Reference instantiation

The csagent repo this skeleton was abstracted from is the worked example.
Layer-to-source mapping (genericize each on demand):

| Template slot | Source (csagent) |
|---|---|
| `A-constitution/constitution.template.md` | `docs/current/iteration_governance.md` §1/§2/§3/§5.1–5.5/§7 |
| `B-process/doc-governance.md` | `docs/current/doc_governance.md` |
| `B-process/milestone-framework.md` | `docs/current/process/milestone-framework.md` |
| `B-process/prompt-artifact-rules.md` | `docs/current/process/prompt-artifact-rules.md` |
| `B-process/badcase-lifecycle.md` | `docs/current/process/badcase-lifecycle.md` |
| `B-process/anti-hardcode-review-kernel.template.md` | `docs/current/anti-hardcode-review-kernel.md` |
| `B-process/architecture-health-metrics.template.md` | `docs/current/process/architecture-health-metrics.md` |
| `B-process/agent-context-guide.template.md` | `docs/current/agent_context_guide.md` |
| `B-process/roles/*` | `docs/teams/{collaboration-guide,deliver-agent,research-agent,deliver-activation}.md` |
| `C-state-ledgers/action-bank*.template.md` | `docs/action_bank.md` + `docs/action_bank_archive.md` (header + retention sweep only) |
| `C-state-ledgers/handoff.template.md` | `docs/10-handoff.md` (section structure) |
| `C-state-ledgers/{milestone,sprint}-objective.template.md` | `docs/{milestone,sprint}_objective.md` (schema) |
| `C-state-ledgers/review-findings.template.md` | `docs/codex-findings.md` (header) |
| `D-prompt-artifacts/*.template.md` | `compact/sprint-NNN-dev-prompt.md`, `compact/M<N>-review-prompt.md` (de-instanced) |
| `AGENTS.template.md` | `AGENTS.md` + `CLAUDE.md` |

Out of scope (domain content, never templated): `docs/foundational/*`,
tool/eval/runtime specs, and all `docs/sprints/*` + `docs/milestones/*`
archives.
