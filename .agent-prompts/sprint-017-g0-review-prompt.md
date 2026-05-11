# Review Agent Prompt — Sprint 17 (G0) Iteration Governance Lite

> Hand this whole file to Codex (the review agent) once the dev agent
> has finished and committed Sprint 17 (G0). Codex will not see this
> conversation; everything it needs is in this prompt and in the linked
> files. Codex must NOT edit any code or doc — its only output is
> `docs/codex-findings.md`.

---

You are the review agent for **Sprint 17 — Iteration Governance Lite (G0)**.
This is a **docs-only governance sprint**. Your review is targeted and
narrow.

## 0. Hard rules

- Do not edit any file except `docs/codex-findings.md`.
- Do not run any test, eval, or build.
- Do not request work that belongs to a future sprint (G1, G2, or
  later). G1 / G2 are explicitly deferred — naming them as a `Recommended
  Next Sprint Action` is fine, but flagging their absence as a
  Sprint 17 blocker is **out of scope**.
- Do not request a different file organization (e.g. splitting
  `iteration_governance.md` into 5 files) unless an internal
  contradiction is found that the split would resolve. The single-file
  bundle is the deliberate choice for G0.
- Do not request architecture-health metric *collection*. G0 only
  defines the metrics.
- Do not edit code, prompt, eval, or `docs/sprints/*` /
  `docs/foundational/*` / `docs/archive/*`.

## 1. Required reading, in this order

1. `docs/sprint_objective.md` — confirm it names Sprint 17 and matches
   the deliverables G0.1 / G0.2 / G0.3 / G0.4. If it still names
   Sprint 16, STOP and write `decision: out_of_scope_review` with a
   note that the sprint objective was not promoted before review was
   requested.
2. `docs/sprints/sprint-017-handoff.md` — the dev agent's full handoff
   including the layer-classification self-walk, anti-hardcode
   self-walk, and hypothetical Sprint 18 stanza. Read end-to-end.
3. The diff for this sprint:
   - `git diff <pre-sprint-base>..HEAD -- docs/current/iteration_governance.md AGENTS.md docs/action_bank.md`
   - `git status` to confirm no other file is modified.
4. `docs/current/iteration_governance.md` — the new state, all 6
   sections (or 7, if G0.3 landed as a separate Section 7).
5. `docs/current/doc_governance.md` and
   `docs/current/agent_context_guide.md` — to check for contradictions
   with the new sections.
6. `docs/runtime_freeze_and_risk_policy.md` §1–§3 — to check that
   Section 3's Tier-0 cross-reference is accurate.
7. `AGENTS.md` — confirm it is non-empty and the constitution chain
   (Option A) loads the three governance docs, OR (Option B) that
   `CLAUDE.md` carries the explicit includes and `docs/action_bank.md`
   carries the AGENTS.md decision row.
8. `docs/action_bank.md` §1, §3, §4, §5.1 (if added), §6 — confirm the
   Sprint 17 entry, the G1 / G2 backlog, and the Sprint 16 closure
   marker.
9. `CLAUDE.md` — confirm whether it was edited (Option B) or left
   alone (Option A).

## 2. Targeted review questions

For each, write a short verdict (one paragraph max). Cite line numbers
from `docs/current/iteration_governance.md` or other touched files
when applicable.

### Q1. Internal consistency

- Are all 6 (or 7) sections internally consistent?
- Do they contradict `docs/current/doc_governance.md` or
  `docs/current/agent_context_guide.md`?
- Does Section 1 (Constitution) preserve the original 40-line wording
  verbatim except for renumbering?
- Are the front matter values consistent with `doc_governance.md`'s
  enums?

### Q2. Fix Layer Classification mechanical usability

Independently walk these 5 scenarios through Section 3's 7-question
checklist. Report the layer each resolves to. Compare against the dev
agent's self-walk in the handoff. Disagreements are findings:

1. User asks "where is my message?" and the bot calls
   `request_handover(faq_miss_threshold_exceeded)` without a prior
   `search_knowledge`.
2. User shifts UC mid-conversation; bot continues stamping the old
   `active_use_case` on the new turn.
3. The eval harness flips a passing case to fail across two reruns of
   the same prompt and CaseSpec.
4. The bot returns a FINAL_ANSWER paraphrasing a
   `retrieved_but_unresolved` hit on a FAQ-path UC.
5. A new escalation reason value is requested by a single CaseSpec.

If any scenario does not resolve to exactly one layer (or to a
different layer than the dev's self-walk), Section 3 has a gap. That
is a `fix_required`.

### Q3. Anti-Hardcode prompt would catch the hardcode PR scenario

Independently apply Section 4's prompt to:

> "PR adds 12 keyword regexes to `DriftDetector` to catch
> UC-A↔UC-C follow-ups, with no Tier-0 invariant claim and no
> sunset plan."

Expected verdict: `reject as semantic hardcode`.

If Section 4's prompt does not produce that verdict (or produces it
unreliably), Section 4 has a gap. That is a `fix_required`.

### Q4. AGENTS.md gap closure

- Is `AGENTS.md` non-empty?
- Does it transitively load the three governance docs (Option A), OR
  did `CLAUDE.md` get the explicit includes and is the
  `docs/action_bank.md` AGENTS.md decision row present (Option B)?
- Either option is acceptable; the only failure mode is "neither
  applied" or "applied half-way".

### Q5. Sprint-objective stanza usability

- Does the G0.3 stanza (in Section 5 sub or Section 7) cover the four
  required fields (target layer, Tier-0 declaration, hardcode
  declaration, generalization coverage)?
- Does it spell out the exemption set (pure infra / docs /
  config-governance / characterization-test sprints)?
- Does the dev agent's hypothetical Sprint 18 stanza in the handoff
  read as mechanically derivable from the template, or did they have
  to invent extra structure?

### Q6. action_bank.md updates

- Sprint 17 entry under §3 with G0.1 / G0.2 / G0.3 / G0.4 deliverable
  rows?
- G1 / G2 backlog rows added?
- Sprint 16 marked closed in §3 / §6 without rewriting the historical
  Sprint 16 narrative?
- Append-only — no deletions of Sprint 14 / 15 / 16 history?

### Q7. Scope containment

- `git status` clean except for the 3 (or 4 with `.gitkeep`) declared
  files?
- No edit under `server/`, `ui/`, `eval/`, `eval_interactive/`,
  `data/`, `scripts/`, root config, `docs/sprints/*`,
  `docs/foundational/*`, `docs/archive/*`, `docs/sprint_objective.md`,
  `docs/codex-findings.md`, `docs/current_eval_baseline.md`,
  `docs/10-handoff.md`?
- No new tier or front-matter status invented?

## 3. Required output: rewrite `docs/codex-findings.md`

Replace `docs/codex-findings.md` (do not append) with this structure.
Match the format of the existing Sprint 16 codex-findings header:

```yaml
---
title: Codex findings (latest sprint review)
doc_tier: diagnostic
status: diagnostic
implementation_status: unknown
runtime_contract: false
last_reviewed: <today>
review_cadence: on_reactivation
notes: >
  Point-in-time sprint review findings (most recent: Sprint 17 G0
  iteration governance lite). Body references specific sections of
  iteration_governance.md, AGENTS.md, and action_bank.md that may
  move on. Use as a historical review record, not as a live list of
  P2 follow-ups.
---
```

Then:

```
## Sprint Review Decision

decision: pass | fix_required | out_of_scope_review
blocking_count: <number>
summary: <one paragraph>

## Blocking Sprint Failures

(One block per blocker, with severity / target doc / blocks-current-sprint /
exact-minimal-fix. "None." if zero.)

## Non-Blocking Notes

(Same shape; "None." if zero.)

## Regression Risks

(Same shape; "None." if zero.)

## Recommended Next Sprint Actions

(Bullet list. Most likely: close Sprint 17 G0 and open Sprint 18 G1
Human-led Failure Portfolio. If you believe a different next-step is
warranted, name it and justify in one sentence.)
```

## 4. Decision rubric

- `pass` — all 7 questions clean. The dev agent's self-walks match
  yours. Diff stays in scope.
- `fix_required` — at least one of Q1, Q2, Q3, Q4, Q5, Q6 has a
  blocking issue. State the minimal fix. Do not propose rewrites
  beyond the minimal fix.
- `out_of_scope_review` — you believe Sprint 17 should also do work
  that the deliver agent and human placed out of scope (G1 case
  briefs, G2 case families, metric collection, runtime change,
  prompt change, etc.). Do not insert that work as a blocker; instead,
  explicitly mark the review out of scope and surface the
  disagreement to the human in the summary.

## 5. What to do if blocked

If you cannot perform the review (missing handoff, missing diff, etc.),
write `decision: out_of_scope_review` and explain the blocker. Do not
guess.

If the diff includes any code change (anything outside the 3-4 declared
docs), write `decision: fix_required` with `blocking_count: 1` and the
single blocker "Sprint 17 was scoped docs-only; revert the
out-of-scope diff before re-review".
