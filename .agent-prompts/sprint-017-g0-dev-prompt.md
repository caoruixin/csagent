# Dev Agent Prompt — Sprint 17 (G0) Iteration Governance Lite

> Hand this whole file to Claude Code (the dev agent) once the human has
> approved `docs/sprint_objective.md` for Sprint 17. The dev agent will
> not see this conversation; everything it needs is in this prompt and
> in the linked files. Do not give the dev agent the codex-findings.md
> prompt or the deliver-agent meta context.

---

You are the dev agent for **Sprint 17 — Iteration Governance Lite (G0)**.
This is a **docs-only governance sprint**. Read every word of this
prompt before you do anything.

## 0. Hard rules

- This sprint touches `docs/current/iteration_governance.md`,
  `AGENTS.md`, and `docs/action_bank.md`. **Nothing else** under
  `server/`, `ui/`, `eval/`, `eval_interactive/`, `data/`,
  `scripts/`, root config files, `docs/sprints/*`,
  `docs/archive/*`, `docs/foundational/*`,
  `docs/sprint_objective.md` (sprint_objective.md is replaced by the
  human, not by you), `docs/codex-findings.md`,
  `docs/current_eval_baseline.md`, or any other doc may be edited.
- No new file may be created except an empty placeholder
  `docs/diagnostics/failure-briefs/.gitkeep` (only if you decide a
  placeholder makes the directory reference in Section 2 of
  `iteration_governance.md` cleaner — your call; if you skip it, just
  state in the handoff that the directory will materialize during G1).
- Run no tests. No `mvn`, no `pytest`, no `npm`, no shell command that
  changes state.
- Do not move or rename any existing file.
- If you discover a contradiction between the sprint objective and an
  existing governance doc (`docs/current/doc_governance.md`,
  `docs/current/agent_context_guide.md`,
  `docs/runtime_freeze_and_risk_policy.md`), STOP and write the
  contradiction in the handoff under "Open questions for human" —
  do not silently resolve it.

## 1. Required reading, in this order

Read the *entire* file unless noted. Do not skim foundational docs.

1. `docs/sprint_objective.md` — Sprint 17 objective (the human-approved
   version). If it still names Sprint 16, STOP and tell the human; you
   are running before the objective was promoted.
2. `docs/current/iteration_governance.md` — the file you will extend.
   Section 1 (Constitution) must survive verbatim except for renumbering
   and front-matter addition.
3. `docs/current/doc_governance.md` — front-matter schema, Claude/Codex
   role split, fold-back cadence. Do not duplicate or contradict it.
4. `docs/current/agent_context_guide.md` — Context Pack Prompt + reading
   lists. Cross-reference where useful; do not duplicate.
5. `docs/sprints/sprint-005-fix-layer-prompt-context-diagnostic-objective.md`
   and `docs/sprints/sprint-005-handoff.md` (skim) — the source of the
   Fix Layer Classification taxonomy. Section 3 of your output must be
   compatible with this taxonomy.
6. `docs/runtime_freeze_and_risk_policy.md` (skim §1–§3 only — Tier-0
   invariants) — Section 3 of your output cross-references Tier-0; do
   not redefine Tier-0.
7. `CLAUDE.md` (4 lines) and `AGENTS.md` (currently empty) — confirm
   the gap. G0.2 fixes it.
8. `docs/action_bank.md` §1, §3, §4 — for the G0.4 update.
9. The most recent Sprint 16 sprint_objective + handoff + codex-findings
   under `docs/sprints/sprint-016-*` — for **format precedent only**.
   Do not copy the Sprint 16 acceptance criteria; G0 is a different
   class of sprint (governance, not contract+test).

After reading, produce a **Context Pack** following the prompt at the
bottom of `docs/current/agent_context_guide.md`, but compressed to ~30
lines. Save it inline at the top of your handoff. The point is to
verify you read the right files, not to produce a thorough audit.

## 2. Deliverables, in order

Implement these in this order. After each one, run a quick self-check
(spelled out) before moving on. Do not batch — finish G0.1 before
opening G0.2.

### G0.1 — Extend `docs/current/iteration_governance.md` to 6 sections

Open the file. Confirm it currently contains only the Constitution body
(no front matter). Then:

1. Add the front matter block exactly as specified in
   `docs/sprint_objective.md` G0.1.
2. Promote the existing Constitution body to **Section 1 — Constitution**.
   Keep its content verbatim except for converting the unstructured
   "Objective / Primary principle / LLM owns / Runtime owns / Iteration
   rule / Evaluation rule / Forbidden" labels into clear subsection
   headings under Section 1. Do not change wording.
3. Add **Section 2 — Failure Brief Template**. Required content:
   - 1-paragraph intro: what a Failure Brief is, who files one (human +
     deliver agent collaboratively), where they live
     (`docs/diagnostics/failure-briefs/` — directory will materialize
     during G1).
   - 6 fields, each with a 1-line description of *why the field exists*:
     `What happened?`, `What should a good CS agent have done?`,
     `Why does this matter?`, `Is this a one-off or a pattern?`,
     `Which layer is likely responsible?`,
     `What should NOT be done?`.
   - One worked example using a hypothetical case `cs_example_001`. Use
     a concise UC-A → messaging-issue drift scenario. Do NOT use any
     real CaseSpec id from `eval_interactive/case_specs/`.
   - A pointer line: "Briefs are filed under
     `docs/diagnostics/failure-briefs/<brief-id>.md` once the directory
     is populated in Sprint 18 (G1)."
4. Add **Section 3 — Fix Layer Classification Checklist**. Required
   content:
   - 1-paragraph intro: when to use this (any time a failure is
     observed and a fix is being considered, before writing any code).
   - The fixed layer set, in this order:
     `infra`, `java_guard`, `prompt_projection`, `skill_state`,
     `semantic_planner`, `eval_spec`, `product_policy`,
     `judge_calibration`, `human_review_required`.
     One-line description per layer (what it owns).
   - The 7 questions in this order, **first matching question wins**:
     1. Is the session failing to start, crash on infra, or hit a
        timeout / OOM not caused by tool semantics? → `infra`.
     2. Is a Tier-0 invariant being broken? (cross-reference
        `docs/runtime_freeze_and_risk_policy.md`) → `java_guard`. If
        no current Tier-0 invariant covers it, flag for
        `human_review_required` rather than inventing a new Tier-0
        invariant.
     3. Did the LLM choose validly within the available options but
        the *projection / context handed to it* was wrong or
        impoverished? → `prompt_projection`.
     4. Is a multi-tool / multi-turn flow losing state across turns
        (entity, task, intake field, drift carry-over)? →
        `skill_state`.
     5. Is the LLM choosing a *semantically wrong action* even when
        projection and state are correct (e.g. wrong UC hypothesis,
        unjustified escalation, missing follow-up)? →
        `semantic_planner`.
     6. Is the eval CaseSpec or judge asking the system to do
        something it cannot or should not do (factual / policy
        impossibility, judge mis-rubric)? → `eval_spec`.
     7. Is the underlying ask a product / policy decision (e.g. "may
        the bot share an advert URL")? → `product_policy`.
     - Tail: if the same case flips across reruns of the *same prompt
       and CaseSpec*, reclassify as `judge_calibration` regardless of
       which layer otherwise matched.
     - Default tail: if no question matches cleanly, →
       `human_review_required`.
   - One short paragraph: "Why no Java guard by default" — point at the
     Constitution's Iteration rule + research §5 Rule 2.
5. Add **Section 4 — Review Agent Anti-Hardcode Prompt**. Required
   content:
   - 1-paragraph intro: this prompt is handed to Codex (or any review
     agent) on any change PR that touches a semantic surface. Pure
     infra / docs / config-governance / characterization-test PRs are
     exempt — list these exemptions.
   - A copy-pastable prompt block (in a fenced ` ```text ` block) with:
     - 9 review questions per research §6.3:
       1. Does it add keyword / regex / if-else / enum / per-UC matrix
          for a semantic decision?
       2. If yes, is it justified as Tier-0 invariant?
       3. Could this be a soft signal projected to the LLM instead?
       4. Does it encode visible eval case text or trace-specific
          phrase?
       5. Does it move semantic ownership from LLM to Java?
       6. Does it add prompt if-else instead of principle / observable
          guidance?
       7. Does it preserve tool / schema / safety / grounding
          boundaries?
       8. Does it include generalization eval (target / neighbor /
          negative / shadow), not just target case?
       9. Does it have a rollback or sunset plan if temporary?
     - The 4-value verdict set:
       `approve` / `approve with downgrade-to-signal follow-up` /
       `reject as semantic hardcode` / `needs human architecture
       decision`.
     - The required header for `docs/codex-findings.md` (4 lines):
       ```
       ## Sprint Review Decision
       decision: pass | fix_required | out_of_scope_review
       blocking_count: <number>
       summary: <one paragraph>
       ```
       (note: this is the *sprint review* header used at sprint close;
       per-PR anti-hardcode reviews use the verdict set above. Make
       both clear in your wording so they are not conflated.)
6. Add **Section 5 — Eval Acceptance Rules**. Required content:
   - 1-paragraph intro: what counts as a "passed" eval result for
     accepting a sprint or a PR.
   - Required acceptance bars:
     - Target cases pass.
     - Neighbor cases no regression.
     - Negative-control cases unchanged (no false positives).
     - Shadow cases no regression. Shadow case results are not
       readable to the dev agent, only to the human / review agent.
     - Safety floor unchanged.
     - Grounding floor unchanged (per
       `docs/current/faq_grounding_contract.md`).
     - Wrong-containment rate unchanged or down.
     - Over-escalation rate unchanged or down.
     - Architecture-health metrics (Section 6) not regressed.
   - Pointer to `docs/current_eval_baseline.md` for the canonical
     baseline.
   - Explicit statement: a visible-eval improvement that ships with a
     shadow regression is a fail.
   - Explicit statement: an eval-side override may not mask a genuine
     bot bug (cross-reference research §5 Rule 4).
7. Add **Section 6 — Architecture-Health Metrics (definitions only)**.
   Required content:
   - 1-paragraph intro: these metrics are defined here and are
     *referenced by* Section 5 acceptance rules. Collection lands in a
     later sprint. No collection code or dashboard in G0.
   - Table of 4 metrics:
     | metric | definition | unit | observation cadence | source artifact | collection_status |
     | --- | --- | --- | --- | --- | --- |
     | `new_semantic_hardcode_count` | Number of new keyword / regex / if-else / enum entries added to runtime or prompt for a semantic decision in a PR | count per PR | per PR | PR diff + Anti-Hardcode review verdict | not_started |
     | `soft_signal_conversion_count` | Number of existing semantic hardcodes downgraded to LLM-projected soft signals | count per sprint | per sprint close | sprint handoff | not_started |
     | `planner_ownership_ratio` | Fraction of semantic decisions in the runtime owned by LLM planning vs Java guard / regex | percentage | per sprint close (manual count) | runtime survey | not_started |
     | `shadow_disagreement_rate` | Fraction of shadow cases where LLM decision disagrees with the human-labelled expected behaviour | percentage | per shadow run | shadow eval result | not_started |

Self-check after G0.1: re-read your file. The Constitution wording from
the original 40-line file must still appear verbatim (modulo new
Section 1 heading). Section 3's 7 questions must each map to exactly
one layer. No section may exceed 80 lines unless absolutely necessary.

### G0.2 — Close the AGENTS.md gap (Option A)

Confirm `AGENTS.md` is still 0 bytes and `CLAUDE.md` still includes it
via `@AGENTS.md`. Then write `AGENTS.md` with this structure:

```markdown
# Repo Constitution

(2-3 paragraphs: what this repo builds — a customer-service agent built
LLM-first, where rules define boundaries and the LLM owns semantic
understanding. Where the constitution lives: under `docs/current/`. How
sprint scope is decided: per `docs/sprint_objective.md` after deliver
agent + human review. The actual constitution chain is loaded
transitively below.)

## Constitution chain

@docs/current/iteration_governance.md

@docs/current/doc_governance.md

@docs/current/agent_context_guide.md

## How to use this constitution

(1 paragraph: every dev / review agent that respects the @AGENTS.md
include in CLAUDE.md auto-loads the three governance docs. Sprint
scope is in `docs/sprint_objective.md`. Per-task reading lists are in
`agent_context_guide.md`. The Failure Brief template, Fix Layer
Classification checklist, and Anti-Hardcode review prompt all live in
`iteration_governance.md`.)
```

If during review the human picks **Option B** instead, follow the
sprint_objective.md G0.2 Option B branch: edit `CLAUDE.md` to include
the three governance docs explicitly, leave `AGENTS.md` at 0 bytes,
and add an `AGENTS.md decision` row to `docs/action_bank.md`. Default
is Option A.

Self-check after G0.2: `wc -c AGENTS.md` is non-zero. The three
included paths exist. `CLAUDE.md` is unchanged (Option A) or carries
explicit includes (Option B).

### G0.3 — Cross-wire the sprint-objective stanza

Re-open `docs/current/iteration_governance.md`. Add either a new
subsection at the end of Section 5 ("Required sprint-objective stanza
for semantic-touching sprints") or a new Section 7 with the same
title — pick whichever reads cleanly. Required content:

- 1-paragraph intro: which sprints must include this stanza
  (semantic-touching sprints), which are exempt (pure infra /
  docs / config-governance / characterization-test).
- The stanza template, copy-pastable, requiring:
  - Named target failure layer (from Section 3 layer set).
  - Tier-0 declaration: either "this sprint adds no Tier-0 invariant"
    OR pointer to the Tier-0 invariant being protected.
  - Hardcode declaration: either "no semantic hardcode introduced"
    OR justification + downgrade-to-signal sunset plan.
  - Generalization coverage: target / neighbor / negative / shadow
    case counts, OR explicit deferral to a later case-family sprint
    if cases do not exist yet.
- A worked example for a hypothetical Sprint 18 that ships a soft
  signal for UC-A↔UC-C follow-up. Use the same `cs_example_001`
  hypothetical case from Section 2.

Self-check after G0.3: the stanza is mechanically followable — a
deliver-agent run could paste it into a future sprint_objective.md
template without further interpretation.

### G0.4 — Update `docs/action_bank.md`

Open the file. Read §1 and §3 fully so you know where Sprint 16 closure
should be marked. Then:

1. Append a new Sprint 17 (G0) entry under §3 "Active / next actions"
   with deliverable rows G0.1 / G0.2 / G0.3 / G0.4 and status `done`
   (you set this at the end of the dev work, before handoff).
2. Append two rows to §4 (or to a new "§5.1 Governance track backlog"
   subsection if §4 feels semantically wrong) for G1 and G2 per the
   table in `docs/sprint_objective.md` G0.4.
3. Update §1 "Current phase" wording so it acknowledges Sprint 17 (G0)
   is the current phase. Keep the Sprint 16 narrative intact; just
   append a new paragraph.
4. Append a new row to §6 "Closed action index" for Sprint 16 if it is
   not already there. (Sprint 16 is in §6 already per the file you
   read; if so, leave §6 alone and just close §3's Sprint 16 wording.)

Self-check after G0.4: the file still validates as a current ledger
(no stale dates, no broken cross-doc links). Run a `grep -n "Sprint 17"`
on the file to confirm your additions landed.

## 3. Required handoff: write to `docs/sprints/sprint-017-handoff.md`

After all four deliverables are done and self-checked, write the
sprint handoff. Required structure (do not skip any section):

1. **Context Pack** — the ~30-line context pack from §1 above.
2. **Exact actions implemented** — bullet per deliverable. Cite the
   line ranges of `iteration_governance.md` you added (e.g. "Section
   2 lines 41–110") and the file size deltas.
3. **Files changed** — exact list of changed files with size before /
   after. Should be exactly 3 files: `docs/current/iteration_governance.md`,
   `AGENTS.md`, `docs/action_bank.md`. (Plus optionally
   `docs/diagnostics/failure-briefs/.gitkeep`.)
4. **Tests run** — explicitly: "no code touched, no tests run".
5. **Layer-classification self-walk** — walk the 5 hand-walked
   scenarios from `docs/sprint_objective.md` Success metrics. For each,
   paste the question chain you took and the resulting layer. State
   whether the checklist resolved cleanly to one layer.
6. **Anti-hardcode self-walk** — apply Section 4's prompt to the
   hardcode PR scenario from `docs/sprint_objective.md` Success
   metrics. Paste the verdict and the reasoning.
7. **Hypothetical Sprint 18 stanza** — write a short hypothetical
   Sprint 18 stanza using G0.3's template, to demonstrate the stanza
   is mechanically followable. (This goes in the handoff, NOT in
   `iteration_governance.md` itself — Section 7 / §5 already carries
   the *example*; the handoff is the *demonstration*.)
8. **Sprint objective met?** — yes / no per success-metric line.
9. **Open questions for human** — anything you noticed that is out of
   scope but worth raising. Do not act on them.
10. **Next recommended action** — almost certainly: "open Codex
    review using `.agent-prompts/sprint-017-g0-review-prompt.md` (or
    the human-edited version)".

## 4. Commit policy

Make exactly one commit at the end, after the handoff is written.
Commit message format (matches the repo precedent — see
`git log --oneline -10` for style):

```
docs: land sprint 17 g0 iteration governance bundle

(2-3 lines: what landed, no marketing wording. Mention G0.1 / G0.2 /
G0.3 / G0.4 by name.)
```

Do not push. Do not open a PR. The human reviews the diff before
either of those.

## 5. What to do if blocked

If any of the following happens, stop and write the situation to the
top of `docs/sprints/sprint-017-handoff.md` under "Blocked at start":

- `docs/sprint_objective.md` does not name Sprint 17.
- `docs/current/iteration_governance.md` has been edited by something
  else since you started.
- Phase 3 fold-back has landed on `docs/foundational/*` and a merge
  conflict is now open.
- You discover a contradiction between the new sections and an
  existing governance doc that cannot be resolved without rewriting
  the existing doc (which is out of scope for G0).
- Any other situation where proceeding would silently broaden scope.

In all of these cases, do nothing else. Wait for the human.
