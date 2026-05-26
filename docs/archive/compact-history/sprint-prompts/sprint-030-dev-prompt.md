Paste the content below this line into a fresh Claude Code session on branch `refactor/remove-the-shackles` (or whatever branch the human points you at). Working tree at session-start carries deliver-agent-owned files + a small set of pre-existing unrelated mods — see §10.

---

# Sprint 30 dev prompt — Alternate-UC signal data-source design (investigation-only, docs-only)

You are the dev agent. Sprint 30 is **investigation-only / docs-only**. Single track, single deliverable: one new design proposal doc + one R-item registration. **Zero** code change. **Zero** test runs. **Zero** smoke runs. The design doc IS the deliverable.

## 1. Loader stanza

Read in order before any work:

1. `AGENTS.md` (transitively loads `iteration_governance.md` §1 §3 §5 §7 + `doc_governance.md` + `agent_context_guide.md`).
2. `docs/sprint_objective.md` — Sprint 30 contract, end to end. §4's empirical premise check is fact; re-verify each item at session start.
3. `docs/current/iteration_governance.md` §7.2 — the hypothetical worked example whose data-source assertion failed Sprint 30's premise check. Read closely.
4. `docs/proposals/handover_orchestrator_design.md` — Sprint 16 docs-only design freeze precedent. Mirror its shape (§ headers, "what is confirmed vs not confirmed", "future shape", "acceptance criteria for the future runtime sprint").
5. `docs/sprints/sprint-027-handoff.md` — most recent investigation-only sprint precedent (probe sprint with closed-set recommendation).
6. `docs/sprints/sprint-016-handoff.md` (or the surrounding archive if the handoff is named differently) for the Sprint 16 design-freeze handoff shape.
7. Run a fresh Context Pack (per `agent_context_guide.md`) before writing the design doc.

## 2. Premise re-verification (mandatory — do not start writing without this)

Each of the five items in `docs/sprint_objective.md` §4 must hold at session start (HEAD ≥ `df8b8cd`). Read the cited paths + line ranges. Expected outcomes:

1. `RuntimeIntentClassifier.classify()` at `server/src/main/java/com/gumtree/csagent/service/runtime/RuntimeIntentClassifier.java:160–269` returns `IntentClassification` (single `predictedUseCase`).
2. `IntentClassification.java` at `server/src/main/java/com/gumtree/csagent/model/IntentClassification.java` — confirm no `alternates` / `candidates` field. `grep -n "alternates\|candidates"` returns empty.
3. `DriftResult.java` at `server/src/main/java/com/gumtree/csagent/model/DriftResult.java` — fields are `{type, newUseCase, escalationRequested}` only.
4. `BotSession.candidateUseCases` is a `String[]`; `ControlKernel.applyRerouteDecision` at `server/src/main/java/com/gumtree/csagent/service/runtime/ControlKernel.java:686` collapses it to single-element `[targetUc]` after reroute (line 1201 fallback path).
5. `ContextProjectionBuilder` projects existing `candidate_use_cases` slot at `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java:380–394`.

If ANY item is false at the cited path/line, STOP. The design problem changed; surface drift in the handoff §1 + §3 + §11; do not write the design doc under a false premise.

## 3. The work — write one design doc

Path: `docs/proposals/alternate_uc_signal_data_source_design.md`. Front matter per `docs/current/doc_governance.md`:

```yaml
---
title: Alternate-UC signal data-source design
doc_tier: proposal
status: proposal
implementation_status: not_started
source_of_truth: this file
last_reviewed: 2026-05-XX
review_cadence: ad hoc
supersedes: []
superseded_by: null
notes: >
  Design freeze authored Sprint 30 (docs-only). Names the data source
  for the future `alternate_candidate_use_cases` projection slot whose
  hypothetical was sketched in `docs/current/iteration_governance.md`
  §7.2. Sprint 31 implements the chosen design.
---
```

Sections (all required; mirror `handover_orchestrator_design.md` shape where appropriate):

1. **Purpose** — one paragraph. Why this doc exists. Cite the §7.2 worked example + Sprint 30 premise gap.
2. **Premise gap (what §7.2 assumed vs what code actually does)** — the §4 premise check from `docs/sprint_objective.md`, restated against your fresh re-verification. Reproducibility: cite file:line for each claim and the exact `grep` / `head` invocation that confirms it.
3. **Three (or more) candidate data sources** — for each:
   - One-paragraph description.
   - Code paths the option would touch (file:line ranges, precise).
   - Layer classification per `iteration_governance.md` §3.
   - Anti-hardcode posture: soft signal (LLM owns the read) vs hard branch. Soft signal is required.
   - Sprint 31 size estimate (small / medium / large) + single-track vs two-track.
   - Risks (per-turn cost, per-turn determinism, eval-trace shape, drift-detector interaction, projection-cost regression).
4. **Recommendation** — pick ONE option. Justify against `iteration_governance.md` §1.3 / §1.5 / §1.7. If no option satisfies the §1.7 anti-hardcode bar, the recommendation is `human_review_required` (§3.2 default tail) — surface honestly.
5. **Code-paths-to-touch table for the chosen option** — Sprint 31 dev reads this verbatim. Be precise. file:line ranges + change type per row (new field on model / new method / new projection branch / etc.).
6. **§7 stanza pre-fill for Sprint 31** — pre-fill the `Layer-classification + anti-hardcode stanza` template from `iteration_governance.md` §7.1 against the chosen option:
   - target failure layer
   - Tier-0 invariant ("This sprint adds no Tier-0 invariant" or named invariant)
   - semantic-hardcode posture
   - generalization coverage (target / neighbor / negative / shadow case shape)
7. **Out-of-scope for Sprint 31** — what Sprint 31 will NOT ship even after the data-source design lands. Specifically: shadow planner mode is OUT; that is a separate, larger sprint. Other items the design pass surfaces.
8. **Open questions for the human** — every item the design pass could not decide. Each open question SHALL name the impasse + the decision the human is being asked to make.
9. **Acceptance criteria for the future Sprint 31** — what tests / smoke / projection diff will be required for Sprint 31 close. Mirror `handover_orchestrator_design.md` §6 shape.
10. **References** — file paths only; no link rot.

Length budget: aim for 6–12K. Sprint 16 design doc (`handover_orchestrator_design.md`) is ~14K and that is on the high end. Sharper is better.

## 4. R-item registration

After the design doc lands, append ONE new R-item to `docs/action_bank.md`. Recommended name: `R-alternate-uc-signal-data-source`. Disposition: `proposal (Sprint 30 design freeze; Sprint 31 implements)`. Mirror existing R-item entry shape (id | source | description columns; the description is one prose paragraph naming the chosen design + Sprint 31 sizing). Do NOT edit any existing R-item; do NOT close `R-prompt-phase-plan-directive-followship` at line 450; do NOT touch the §6 closed-action index — that's the deliver-agent's close-turn job.

## 5. Discipline (the hard gate)

This is a docs-only sprint. The following are STOP conditions, not soft preferences:

- **No code change.** `git diff --name-only` against the merge base must show ZERO files under `server/`, `eval_interactive/`, or `server/src/main/resources/prompts/`.
- **No test run.** No `mvn test`, no `pytest`, no `eval-interactive run`. Sprint 30 ships no code; running tests is noise.
- **No smoke run.** Same reason.
- **No CaseSpec authoring.** Sprint 29's case-family fence is in force.
- **No `system_prompt.txt` edit.** That belongs to Sprint 31 (under the chosen design's plan).
- **No Tier-0 invariant addition.** Sprint 30 ships no Tier-0 change. If the chosen option requires one, surface as `human_review_required` per `docs/sprint_objective.md` §5.4.
- **No editing of existing case families.** Sprint 20 / Sprint 29 cascade fence.
- **No mid-sprint Sprint 31 scope drafting.** Sprint 31 scope is the next deliver-cycle's job.
- **No editing of `iteration_governance.md` §7.2** in place. The fold-back is a separate cadence.

## 6. Reproducibility rule

Every claim about code behaviour in the design doc MUST cite a source path + (where applicable) a `grep` / `head` / `wc -l` invocation that confirms it. No claim without method. Per `feedback_deliver_agent_cited_numbers_must_be_reproducible.md`. Examples:

- "RuntimeIntentClassifier returns one UC per call" → cite `RuntimeIntentClassifier.java:160` + the `IntentClassification` constructor signature.
- "BotSession.candidateUseCases collapses to single element after reroute" → cite `ControlKernel.java:686` + the assignment statement.
- "X% of smoke turns have empty candidates" → cite `jq` extraction against a named `results.json` path.

## 7. Anti-hardcode discipline (recap)

Per Constitution §1.5 / §1.7:

- The chosen design MUST be a **soft signal** — the LLM reads `alternate_candidate_use_cases` and decides; no Java branch acts on the value.
- The chosen design MUST NOT introduce a keyword / regex / if-else / per-UC matrix in the data-source pipeline.
- The chosen design MUST NOT silently promote `RuntimeIntentClassifier`'s existing pattern set to a "list" (each pattern fires independently and collapses to one UC; promoting "all matching patterns become alternates" is a semantic shift the design SHALL discuss explicitly, not bake in).
- The chosen design MUST NOT propose a new Tier-0 invariant. If it requires one, exit as `human_review_required`.

## 8. 12-section handoff doc contract

Write `docs/sprints/sprint-030-handoff.md` with the standard 12-section shape (per `docs/sprint_objective.md` §11). The 12 sections are: (1) Context Pack, (2) Sprint-objective recap, (3) Premise re-verification, (4) Design pass walkthrough, (5) Recommendation summary, (6) Code-paths-to-touch table, (7) Open questions for the human, (8) Anti-hardcode self-walk, (9) Files changed, (10) Layer-classification self-walk, (11) R-item registration, (12) Closure verdict placeholder (deliver-agent owned).

## 9. Stop conditions

STOP and report (do NOT silently work around) when:

1. Any of §2's five premise items fails at session start.
2. The design pass concludes no candidate option satisfies the §1.7 anti-hardcode bar — exit as `human_review_required` per §1.7 / §3.2 default tail; do not invent a new Tier-0 invariant; do not silently promote a hard branch as "soft".
3. A code-edit feels necessary to validate the design — STOP. Validation is Sprint 31's job.
4. The design pass surfaces an architectural question that needs product / platform input (e.g. "does the chosen option require a new bot-side endpoint?") — STOP and surface in §7 open questions, do not assume the answer.
5. Tempted to draft Sprint 31 objective inside this sprint — STOP.
6. Tempted to author CaseSpecs — STOP. Sprint 29 corpus untouched.
7. Tempted to edit `iteration_governance.md` §7.2 in place — STOP. Fold-back is a separate cadence.

## 10. Working tree at session start (commit-at-end pattern)

Expect uncommitted files at session start:

- `docs/sprint_objective.md` (Sprint 30 contract; deliver-agent-owned).
- `compact/sprint-030-dev-prompt.md`, `compact/sprint-030-review-prompt.md` (deliver-agent-owned).
- `docs/sprints/sprint-029-objective.md` (Sprint 29 archive; deliver-agent-owned at archive time).
- `docs/sprints/sprint-029-handoff.md` (Sprint 29 dev handoff; landed during Sprint 29, may still be untracked).
- `docs/10-handoff.md` (deliver-agent §1 lead refresh).
- `docs/action_bank.md` (modified — pre-existing tracked change from Sprint 29 close window).
- `csagent_system_design_review.md`, `server/src/main/resources/prompts/system_prompt.txt`, `eval_interactive/case_specs/case_families/sprint29_directive_probe/` — pre-existing unrelated mods OR Sprint 29 untracked artefacts.

**Do not stage these.** Stage only your authored §3 + §4 files (the new design doc + the `docs/action_bank.md` R-item append + the new `docs/sprints/sprint-030-handoff.md`) for your final commit. Do not run `git add -A` / `git add .`. The human bundles deliver-agent files at commit time per the commit-at-end pattern. Per `feedback_commit_at_end_bundles_deliver_artefacts.md`.

## 11. Final-commit run commands

There is **no test run** for Sprint 30. The dev's commit lands the design doc + the R-item append. The handoff §11 records the exact text appended to `docs/action_bank.md`.

If the dev catches an inconsistency between the design doc and a code claim during writing, STOP — fix the doc OR re-verify the code; do NOT add code to "make the doc true." Sprint 30 is descriptive, not prescriptive.

If you hit a stop condition, write the handoff up to the stop point and mark later sections "not reached due to <stop condition>". Do not skip silently.
