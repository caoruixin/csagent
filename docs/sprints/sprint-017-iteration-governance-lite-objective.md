# Sprint Objective

Date: 2026-05-11

## Sprint name

Sprint 17 — Iteration Governance Lite (G0)

## Goal

Land the minimum governance scaffolding so that every Sprint 18+ change to the
agent can be evaluated against an **explicit layer-classification gate** and an
**anti-hardcode review prompt**, before it merges.

This is a **docs-only governance sprint**. It must not change runtime, prompt,
FAQ corpus, CaseSpec, judge, eval harness, tests, or scripts. It must not
collect any new metric — it only defines them.

The scope is intentionally narrow: this sprint pays back the gap that
`docs/current/iteration_governance.md` only carries the *constitution* part of
the research-proposed governance bundle (Constitution + Failure Brief +
Layer Classification + Anti-Hardcode Review Prompt + Eval Acceptance Rules
+ Architecture-Health Metrics). G0 finishes the bundle.

## Background

The research agents converged on the following sequencing:

1. **G0 — Iteration Governance Lite** (this sprint): docs-only governance
   scaffolding. No system change. Purpose is to install the gate.
2. **G1 — Human-led Failure Portfolio** (deferred to next sprint): pick
   10–20 representative failures from human experience / past traces and
   convert each into a Failure Brief.
3. **G2 — Interactive Eval Case Family + Shadow Split** (deferred): turn
   the briefs into target / neighbor / negative / shadow case families.
4. Only after G0–G2 do we re-open Semantic Planner shadow mode and any
   runtime work.

Repo state at the start of G0:

- Sprint 16 (Handover Exactly-Once Contract and Repro) is closed with
  Codex `decision: pass, blocking_count: 0`. Sprint 16 archives already
  exist under `docs/sprints/sprint-016-*`. This file replaces the
  Sprint 16 sprint_objective.md content; the Sprint 16 archive is the
  historical record.
- `docs/current/iteration_governance.md` exists but only carries Section 1
  (the Constitution: LLM owns / Runtime owns / Iteration rule / Evaluation
  rule / Forbidden). The remaining five sections from the research
  proposal are missing.
- `docs/current/doc_governance.md` and `docs/current/agent_context_guide.md`
  are landed and current. G0 must not duplicate or contradict them; it
  cross-references them.
- `AGENTS.md` is **0 bytes** but `CLAUDE.md` includes it via `@AGENTS.md`
  as the repo constitution. This means Claude Code currently has no
  enforced constitution. G0 closes this gap (see G0.2).
- `docs/action_bank.md` recommends Eval Governance Follow-up as the next
  primary phase. G0 is the precondition for that.

## Implement exactly these 4 actions

### G0.1 — Expand `docs/current/iteration_governance.md` to the full bundle

The file already contains Section 1 (Constitution). Extend it (in-place,
preserving Section 1 verbatim except for renumbering and the front matter)
to include five additional sections with the structure below. Each section
must be self-contained and copy-pastable by a dev / review / human user.

Required sections, in this order, with these exact titles:

1. **Constitution** — already present. Keep verbatim. Promote to a
   numbered Section 1.

2. **Failure Brief Template** — the 6-question template. Required fields:
   `What happened?`, `What should a good CS agent have done?`,
   `Why does this matter?`, `Is this a one-off or a pattern?`,
   `Which layer is likely responsible?`, `What should NOT be done?`.
   Include a 1-line description of each field (why it exists), a worked
   example based on a hypothetical failure (do not use a real CaseSpec
   id; use `cs_example_001` to avoid coupling to real data), and a
   pointer that briefs are filed under `docs/diagnostics/failure-briefs/`
   (directory may not exist yet — that is fine; G1 will populate it).

3. **Fix Layer Classification Checklist** — the 7-step decision tree from
   Sprint 5 / research §5 Rule 1. Use exactly this layer set (matches
   the existing `docs/sprints/sprint-005-*` taxonomy):
   `infra`, `java_guard`, `prompt_projection`, `skill_state`,
   `semantic_planner`, `eval_spec`, `product_policy`,
   `judge_calibration`, `human_review_required`.
   Order the 7 questions so that the first matching question wins.
   Cross-reference `docs/runtime_freeze_and_risk_policy.md` for the
   Tier-0 invariant definition; do not redefine Tier-0 here.

4. **Review Agent Anti-Hardcode Prompt** — a copy-pastable prompt block
   the human (or future automation) hands to Codex on any change PR.
   Must include the 9 review questions from research §6.3 and the 4-value
   verdict set (`approve` / `approve with downgrade-to-signal follow-up` /
   `reject as semantic hardcode` / `needs human architecture decision`).
   Must require Codex to fill in the 4-line header at the top of
   `docs/codex-findings.md` (`## Sprint Review Decision`,
   `decision:`, `blocking_count:`, `summary:`) — same convention used by
   Sprint 16.

5. **Eval Acceptance Rules** — operational form of the Constitution's
   "Evaluation rule" line. Must specify: target / neighbor / negative
   / shadow expectations; safety / grounding / wrong-containment /
   over-escalation regression bars; the rule that visible-eval
   improvement alone is insufficient when shadow regresses; pointer to
   `docs/current_eval_baseline.md` for the canonical baseline file.
   Do not invent new metrics here — only formalize the acceptance gate.

6. **Architecture-Health Metrics (definitions only)** — define the 4
   metrics named in the research proposal: `new_semantic_hardcode_count`,
   `soft_signal_conversion_count`, `planner_ownership_ratio`,
   `shadow_disagreement_rate`. For each, give a one-line definition,
   the unit, the proposed observation cadence, and the source artifact
   (e.g. PR diff, eval result file). Mark each as
   `collection_status: not_started`. **No collection code, dashboard,
   or Prometheus metric is implemented in G0.**

Front matter for the file (update existing front matter; the file currently
has none):

```yaml
---
title: Iteration governance
doc_tier: current-runtime
status: current
implementation_status: partial
source_of_truth: this file
last_reviewed: 2026-05-11
review_cadence: every 3-5 sprints
supersedes: []
superseded_by: null
notes: >
  Iteration constitution + governance bundle. Section 1 is the LLM-first
  constitution. Sections 2-6 are the operational gates: Failure Brief
  template, Fix Layer Classification checklist, Anti-Hardcode review
  prompt, Eval Acceptance Rules, and Architecture-Health Metric
  definitions. Metrics are defined only; collection lands in a later
  sprint. The Failure Brief and Fix Layer checklist drive G1 / G2.
---
```

### G0.2 — Close the `AGENTS.md` constitution gap (Option A)

`CLAUDE.md` (4 lines, currently checked into the repo) includes
`@AGENTS.md` as the shared repository constitution. `AGENTS.md` is
present but 0 bytes. The constitution chain is broken.

Implement **Option A** — seed `AGENTS.md` with a constitution chain:

Write `AGENTS.md` with:

- A 2–3 paragraph repo intent header (this is a customer-service agent
  built LLM-first; constitution + governance live under `docs/current/`).
- Explicit `@docs/current/iteration_governance.md` include line.
- Explicit `@docs/current/doc_governance.md` include line.
- Explicit `@docs/current/agent_context_guide.md` include line.
- A short "How to use this constitution" paragraph explaining: every
  dev / review agent loads this file; it transitively loads the three
  governance docs; sprint-specific scope is defined per sprint in
  `docs/sprint_objective.md`.

No edit to `CLAUDE.md`. The `@AGENTS.md` include keeps working and now
resolves to non-empty content.

### G0.3 — Add a "Layer classification + anti-hardcode" stanza to the sprint-objective format

Update `docs/current/iteration_governance.md` Section 5 (Eval Acceptance
Rules) **or** add a small Section 7 — your choice during drafting — that
specifies the **required stanza future `docs/sprint_objective.md` files
must include** if the sprint touches any semantic surface (prompt,
runtime semantic decision, eval spec, judge calibration). The stanza
must require:

- Named target failure layer (from the layer set in Section 3).
- Explicit "this is not a Tier-0 invariant" statement, OR a pointer to
  the Tier-0 invariant being protected.
- Explicit "no semantic hardcode introduced" statement, OR justification
  with downgrade-to-signal sunset plan.
- Generalization eval coverage statement (target / neighbor / negative
  / shadow), even if cases do not exist yet (then explicitly defer to
  the case-family sprint).

Pure infra / docs / config-governance / characterization-test sprints
(like Sprint 15 and Sprint 16) are exempt. Spell this exemption out so
future deliver-agent runs do not over-apply the stanza.

Do **not** rewrite `docs/sprint_objective.md` (this file) as part of the
implementation; it is replaced on the next sprint promotion, not
modified mid-sprint.

### G0.4 — Update `docs/action_bank.md` to reflect Sprint 17 (G0) and the next-up G1 / G2 backlog

Append a new entry under §3 "Active / next actions" naming Sprint 17
(G0) deliverables G0.1 / G0.2 / G0.3 / G0.4 with status `in progress`
during the sprint, `done` at close. Move the "Single Handover
Orchestrator" item from §3 to §4 if it is still under §3 (it appears
to already live in §4).

Append two new deferred items under §4 or under a new "§5.1 Governance
track backlog" subsection:

| id | candidate | status | owner | notes |
|---|---|---|---|---|
| G1 | Human-led Failure Portfolio (10–20 representative failures from human experience / past traces, each filed as a Failure Brief) | deferred — next governance sprint | deliver / human | depends on G0 templates |
| G2 | Interactive Eval Case Family + Shadow Split (target / neighbor / negative / shadow per failure brief) | deferred — after G1 | deliver / eval governance | depends on G1 briefs |

Do **not** delete or rewrite Sprint 14 / 15 / 16 entries in §3 / §6.
Append-only.

## Do not implement

- Any change under `server/`, `ui/`, `eval/`, `eval_interactive/`,
  `data/`, `scripts/`, or root config files.
- Any prompt edit (`system_prompt.txt`, routing prompts, judge prompts).
- Any FAQ corpus / CaseSpec / judge / eval-output schema change.
- Any new test (Java, Python, or eval harness).
- Any Failure Brief content (the *template* is in scope; *populated
  briefs* are G1).
- Any Eval Case Family content (the *acceptance rule* is in scope;
  *populated cases* are G2).
- Any architecture-health metric collection (definitions only; collection
  is a later sprint).
- Any edit to `docs/sprints/*` archives.
- Any edit to `docs/foundational/*`. Phase 3 fold-back is in flight on
  another branch / PR; G0 must not collide.
- Any update to `docs/codex-findings.md` (that is the review agent's job
  at the end of the sprint).
- Any update to `docs/10-handoff.md` beyond the standard sprint handoff
  required at close.
- Any update to `docs/current_eval_baseline.md`.
- Any new tier or new front-matter status. Reuse existing
  `doc_governance.md` enums.

## Success metrics

- `docs/current/iteration_governance.md` carries all 6 sections in the
  exact order specified in G0.1, with the front matter from G0.1.
- The Fix Layer Classification checklist's 7 questions evaluate to
  exactly one layer for each of these 5 hand-walked test scenarios
  (the dev agent walks them and pastes the layer in the handoff):
  1. A user asks "where is my message?" and the bot calls
     `request_handover(faq_miss_threshold_exceeded)` without a prior
     `search_knowledge`.
  2. A user shifts UC mid-conversation and the bot continues stamping
     the old `active_use_case` on the new turn.
  3. The eval harness flips a passing case to fail across two reruns
     of the same prompt and CaseSpec.
  4. The bot returns a FINAL_ANSWER paraphrasing a `retrieved_but_unresolved`
     hit on a FAQ-path UC.
  5. A new escalation reason value is requested by a single CaseSpec.
- The Anti-Hardcode review prompt produces one of the 4 verdicts on
  this hand-walked PR scenario (dev agent walks it in the handoff):
  *"PR adds 12 keyword regexes to `DriftDetector` to catch UC-A↔UC-C
  follow-ups, with no Tier-0 invariant claim and no sunset plan."*
  Expected verdict: `reject as semantic hardcode`.
- `AGENTS.md` is non-empty and resolves to a transitive load of the
  three governance docs (Option A).
- `docs/sprint_objective.md` future-author stanza in G0.3 is mechanically
  followable — the dev agent demonstrates this by drafting (in the
  handoff, not in the file) a hypothetical Sprint 18 stanza.
- `docs/action_bank.md` lists Sprint 17 (G0) and the G1 / G2 backlog
  items per G0.4.
- `mvn -pl server test` and `pytest eval_interactive/tests/` are not
  required to be re-run (no code touched). The dev agent must explicitly
  state "no code touched, no tests run" in the handoff. Running them
  anyway is allowed but not required.
- No file under `docs/sprints/`, `docs/archive/`, `docs/foundational/`,
  `server/`, `ui/`, `eval/`, `eval_interactive/`, `data/`, or
  `scripts/` is modified.

## Review rule

Codex must review only:

1. Whether the 6 sections of `iteration_governance.md` are internally
   consistent and do not contradict `doc_governance.md` or
   `agent_context_guide.md`.
2. Whether the Fix Layer Classification checklist mechanically resolves
   each of the 5 success-metric scenarios to a single layer (Codex
   independently walks them).
3. Whether the Anti-Hardcode review prompt would actually catch the
   hand-walked hardcode PR scenario.
4. Whether `AGENTS.md` plus the constitution chain transitively loads
   the three governance docs.
5. Whether the G0.3 sprint-objective stanza is mechanically followable.
6. Whether `docs/action_bank.md` reflects the close of Sprint 16 and
   the open of Sprint 17, with G1 / G2 listed as deferred next-up.
7. Whether the diff stays inside docs.

Codex must NOT:

- Ask for any Failure Brief to be populated (that is G1).
- Ask for any Case Family to be built (that is G2).
- Ask for any architecture-health metric collection to be implemented
  (that is a later sprint).
- Ask for any code, prompt, test, or eval change.
- Ask for `iteration_governance.md` to be split into multiple files
  unless an internal contradiction is found that splitting would
  resolve.
- Ask for any edit to `docs/foundational/*` or `docs/sprints/*`.

If Codex finds the bundle is internally consistent and the diff stays
in scope, the expected `## Sprint Review Decision` is `pass`.

If Codex finds an internal contradiction or a scope leak, the expected
`decision` is `fix_required` with `blocking_count` ≥ 1 and a named
target file + minimal-fix description per the Sprint 16 review format.

If Codex believes the sprint should also do G1 / G2 work, the expected
`decision` is `out_of_scope_review` and the human is the decider.
