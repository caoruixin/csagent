# Sprint 17 Handoff — Iteration Governance Lite (G0)

Date: 2026-05-11
Branch: `design-v1-without-human-review`
Sprint class: docs-only governance (no runtime, no test, no prompt,
no CaseSpec, no FAQ corpus, no judge, no eval harness change).

## 0. Context Pack

**Relevant docs**

- `docs/sprint_objective.md` (current-runtime; status: current) — the
  human-promoted Sprint 17 objective; source of the four G0
  deliverables.
- `docs/current/iteration_governance.md` (current-runtime; status:
  current → extended) — pre-Sprint 17 carried only the Constitution
  (40 lines, no front matter); this sprint extends it to the full
  six-section governance bundle + Section 7 stanza.
- `docs/current/doc_governance.md` (durable-connective; status:
  current) — front-matter schema, tier model, Claude/Codex role
  split. Sprint 17 reuses its enums; does not duplicate them.
- `docs/current/agent_context_guide.md` (durable-connective; status:
  current) — per-task reading lists + Context Pack Prompt. Sprint
  17 cross-references it.
- `docs/runtime_freeze_and_risk_policy.md` (foundational; status:
  current) — Tier-0 invariants are sourced from §1 / §2 of this doc.
  Sprint 17 does not redefine Tier-0.
- `docs/sprints/sprint-005-*` (sprint-archive) — origin of the fix
  layer taxonomy; Sprint 17 §3 uses the updated layer set named in
  the Sprint 17 objective (not the verbatim Sprint 5 set).
- `docs/action_bank.md` — updated for §1 current-phase, §3 active
  actions, §5.1 (new) governance-track backlog.
- `AGENTS.md` (was 0 bytes) — seeded with the constitution chain so
  the `@AGENTS.md` include in `CLAUDE.md` resolves to non-empty
  content.

**Relevant code paths** — none. G0 is docs-only; no `server/`, `ui/`,
`eval/`, `eval_interactive/`, `data/`, `scripts/`, or config file
was read with the intent to edit.

**Doc status warnings**

- `docs/sprint_objective.md` was modified by the human pre-sprint
  (Sprint 16 → Sprint 17 objective); my work does not touch it.
- `docs/current/iteration_governance.md` was untracked on disk
  before my Write; the commit will introduce it to HEAD as part of
  the Sprint 17 bundle.
- `docs/codex-findings.md` is untracked and outside my scope; it
  will be written by the review agent at sprint close.

**Source-of-truth decision** — for the four G0 deliverables, the
authoritative artifacts are: `docs/current/iteration_governance.md`
(for the bundle and the stanza), `AGENTS.md` (for the constitution
chain), and `docs/action_bank.md` (for the ledger). The
sprint_objective.md is normative for scope; doc_governance.md is
normative for tier and status enums.

**Implementation status** — `iteration_governance.md` front matter
declares `implementation_status: partial` because Sections 5 and 6
reference acceptance bars (architecture-health metrics) that are
defined here but not yet collected. Sections 1–4 and 7 are
`implemented`.

**Risks before coding** — top three identified before drafting:
(1) Constitution wording loss during renumbering — mitigated by
re-reading the original 40-line file before Write and preserving
each line verbatim under `### 1.x` subsections;
(2) Tier-0 cross-reference drift if `runtime_freeze_and_risk_policy.md`
§ numbering shifts — mitigated by linking to §1 / §2 broadly rather
than a specific bullet;
(3) Layer-set drift from the historical Sprint 5 taxonomy (the
Sprint 17 layer set renames some entries, e.g. `prompt_projection`
vs Sprint 5's `prompt_context_projection`) — addressed by using
the Sprint 17 objective's layer set verbatim and noting the rename
in this handoff, not silently in code.

## 1. Exact actions implemented

- **G0.1** — extended
  `docs/current/iteration_governance.md` from 40 lines (Constitution
  body only, no front matter) to 506 lines, structured as: front
  matter (lines 1–18); H1 + intro (lines 20–38); §1 Constitution
  promoted verbatim with subsections 1.1–1.7 (lines 40–90); §2
  Failure Brief Template with 6 field definitions + worked example
  `cs_example_001` (lines 92–172); §3 Fix Layer Classification
  Checklist with 9-layer set, 7 ordered questions, judge-stability
  tail, default tail, and "why no Java guard by default"
  subsection (lines 173–262); §4 Anti-Hardcode Review Prompt with
  9 questions, 4 verdict values, and the separate sprint-close
  review header (lines 263–355); §5 Eval Acceptance Rules with the
  9 acceptance bars, baseline pointer, visible-vs-shadow rule, and
  no-eval-override rule (lines 356–411); §6 Architecture-Health
  Metrics with a 4-row table (lines 412–429); §7 Required
  sprint-objective stanza with the template and a hypothetical
  Sprint 18 worked example using `cs_example_001` (lines 430–506).
- **G0.2** — seeded `AGENTS.md` (was 0 bytes) with a repo-intent
  header, an explicit constitution chain (`@docs/current/doc_governance.md`,
  `@docs/current/agent_context_guide.md`,
  `@docs/current/iteration_governance.md`), and a "how to use this
  constitution" paragraph. `CLAUDE.md` is unchanged at 4 lines; the
  existing `@AGENTS.md` include now resolves to the chain.
- **G0.3** — landed as Section 7 of `iteration_governance.md`
  (lines 430–506), not as a separate file. Section 7 specifies which
  sprints must include the stanza (semantic-touching), the
  exemption list (pure infra / docs-only / config-governance /
  characterization-test), the four-field stanza template, and a
  worked example referencing `cs_example_001`.
- **G0.4** — `docs/action_bank.md` updated: §1 Current phase now
  leads with Sprint 17 (G0) and re-labels the Sprint 16 narrative as
  "Preceding sprint (most recently closed, Codex pass)" while
  preserving the Sprint 16 paragraph body verbatim; §3 Active /
  next actions carries a new Sprint 17 deliverable block with G0.1
  / G0.2 / G0.3 / G0.4 rows all marked `done` (above the Sprint 16
  table, which is preserved); a new §5.1 Governance track backlog
  subsection lists G1 (Human-led Failure Portfolio) and G2
  (Interactive Eval Case Family + Shadow Split) as deferred; §6
  Closed action index is unchanged (Sprint 16 row already present);
  top-of-file `Date:` bumped from 2026-05-09 to 2026-05-11. I did
  not optionally create `docs/diagnostics/failure-briefs/.gitkeep`
  — Section 2 of `iteration_governance.md` cleanly states the
  directory will materialize during G1, so a placeholder is
  redundant.

## 2. Files changed

| path | before | after | delta |
|---|---|---|---|
| `docs/current/iteration_governance.md` | untracked on disk; 40 lines, ≈1.3K bytes, no front matter, Constitution body only | tracked by this sprint; 506 lines, 23,229 bytes, full bundle | +466 lines, ≈+22K bytes (file is introduced to HEAD by this sprint) |
| `AGENTS.md` | tracked; 0 bytes (empty) | tracked; 2,804 bytes | +2,804 bytes |
| `docs/action_bank.md` | tracked; 30,051 bytes | tracked; 33,875 bytes | +3,824 bytes |

Exactly three files changed for the G0 deliverables. (This handoff
file — `docs/sprints/sprint-017-handoff.md` — is a fourth, new file
under `docs/sprints/`, written per Section 3 of the dev-agent prompt
as the required sprint-close artifact; it is not part of the four
G0 deliverables.) No file under `server/`, `ui/`, `eval/`,
`eval_interactive/`, `data/`, `scripts/`, `docs/archive/`,
`docs/foundational/`, `docs/current_eval_baseline.md`, or any root
config was modified. `docs/sprint_objective.md` is modified in git
status but that is the human's pre-sprint replacement (Sprint 16 →
Sprint 17 objective), not my edit. `docs/codex-findings.md` is
untracked and outside my scope.

## 3. Tests run

No code touched, no tests run. `mvn -pl server test` and
`pytest eval_interactive/tests/` were not invoked, per the Sprint
17 objective's success-metric line and the prompt's explicit hard
rules (no `mvn`, `pytest`, `npm`, or state-changing shell command).

## 4. Layer-classification self-walk (Section 3 questions)

The five scenarios from `docs/sprint_objective.md` success metrics,
walked through `iteration_governance.md` §3.2's seven questions.
"Q1 / no" means question 1 did not match; the walk continues until
a match.

### Scenario 1 — bot calls `request_handover(faq_miss_threshold_exceeded)` without a prior `search_knowledge`

- Q1 (infra)? No — not a session-start / OOM / timeout failure.
- Q2 (java_guard / Tier-0)? No current Tier-0 invariant in
  `runtime_freeze_and_risk_policy.md` §1 / §2 forbids handover
  without a prior `search_knowledge`. The "no prior search guard"
  is listed as a deferred candidate in `docs/action_bank.md` §4
  (`D-S3-no-prior-search-guard`), explicitly not currently Tier-0.
  The failure does not "look like Java-guard territory" either,
  because the decision (whether to handover before searching) is
  a semantic choice. Continue.
- Q3 (prompt_projection)? Possible, but only if the projection
  fails to surface the "no prior `search_knowledge` was called"
  signal. Assume the projection is adequate (turn 1 with no tool
  history is observable). Continue.
- Q4 (skill_state)? No — single-turn, no cross-turn state loss.
- Q5 (semantic_planner)? **Match.** The LLM chose a semantically
  wrong action (handover) when projection and state are correct;
  this is the canonical "unjustified escalation" example named in
  §3.2 Q5.
- **Result: `semantic_planner`.** Resolves cleanly to one layer.

### Scenario 2 — user shifts UC mid-conversation, bot stamps old `active_use_case` on the new turn

- Q1 / no. Q2 / no — no Tier-0 invariant forces a UC reroute on a
  soft topic shift (Sprint 10 §L1 reroute is a runtime feature,
  not a Tier-0 invariant).
- Q3 (prompt_projection)? **Match.** If `active_use_case=UC-old` is
  the only UC visible in the projection and no `alternate_candidate_use_cases`
  signal is surfaced, the LLM chose validly within the available
  options. The projection is impoverished.
- **Result: `prompt_projection`.** Resolves cleanly to one layer.
  (If the projection were already surfacing alternates and the LLM
  still chose old UC, Q5 `semantic_planner` would fire instead;
  the checklist correctly disambiguates the two cases.)

### Scenario 3 — eval flips a passing case to fail across two reruns of the same prompt and CaseSpec

- The judge-stability tail rule (Section 3.2 tail) applies: "if
  the same case flips across reruns of the *same prompt and
  CaseSpec*, reclassify as `judge_calibration` regardless of which
  question above otherwise matched."
- **Result: `judge_calibration`.** Resolves cleanly to one layer
  via the tail rule.

### Scenario 4 — bot returns a FINAL_ANSWER paraphrasing a `retrieved_but_unresolved` hit on a FAQ-path UC

- Q1 / no. Q2 — no current Tier-0 invariant covers FINAL_ANSWER
  paraphrasing of `retrieved_but_unresolved` hits. The "FAQ-grounded
  resolve bypass" is listed as a deferred candidate in
  `docs/action_bank.md` §4 (`D-faq-grounded-resolve-bypass`) and
  Sprint 14 explicitly carved out the hard citation gate
  (`D-hard-citation-gate`). The failure does not look like
  Java-guard territory in the current architecture; it is a
  semantic choice the LLM is making with adequate diagnostics. Continue.
- Q3 (prompt_projection)? Post Sprint 14.1, `retrieved_but_unresolved`
  is persisted to `bot_turns.projected_context.faq_grounding` and
  mirrored on `BotSession` transient slots, so the LLM can see it.
  Projection is adequate. Continue.
- Q4 / no.
- Q5 (semantic_planner)? **Match.** The LLM saw the
  `retrieved_but_unresolved=true` diagnostic and still chose to
  paraphrase as a FINAL_ANSWER — a semantically wrong action.
- **Result: `semantic_planner`.** Resolves cleanly to one layer.

### Scenario 5 — a new escalation reason value is requested by a single CaseSpec

- Q1–Q5 / no. Q6 (eval_spec)? **Match.** The 23-value
  `escalation_reason` enum is frozen per
  `docs/runtime_freeze_and_risk_policy.md` §1.3 / §1.2 and is
  listed as deferred (`D-new-escalation-reason-enum` in
  `docs/action_bank.md` §4). A single CaseSpec asking the system
  to emit a new enum value is asking the system to do something
  it cannot and should not do without a coordinated migration.
- **Result: `eval_spec`.** Resolves cleanly to one layer.

**Summary:** all five scenarios resolve to a single layer through
§3.2's first-match-wins ordering: `semantic_planner` /
`prompt_projection` / `judge_calibration` / `semantic_planner` /
`eval_spec`. The checklist did not deadlock or split across
multiple layers for any scenario.

## 5. Anti-hardcode self-walk (Section 4.1 prompt)

PR scenario from `docs/sprint_objective.md` success metrics: *"PR
adds 12 keyword regexes to `DriftDetector` to catch UC-A↔UC-C
follow-ups, with no Tier-0 invariant claim and no sunset plan."*

Walking the 9 questions in §4.1:

1. Does the PR add keyword / regex / if-else / enum / per-UC matrix
   for a semantic decision? **Yes** — 12 regexes for UC-A↔UC-C
   follow-up routing in `DriftDetector`.
2. Justified as protecting a current Tier-0 invariant? **No** —
   the scenario explicitly says "no Tier-0 invariant claim".
3. Could the same outcome be a soft signal? **Yes** — the runtime
   already has `RuntimeIntentClassifier` (Sprint 10 §L0) producing
   candidate UCs. The right move is to surface an
   `alternate_candidate_use_cases` slot in the projection and let
   the LLM choose, not hardcode 12 regexes that encode an
   eval-derived pattern.
4. Encodes visible-eval case text or trace-specific phrasing?
   **Likely yes** — 12 regexes are almost certainly derived from
   observed eval traces. (Even without confirmation, the scenario
   has the shape of an eval-driven hardcode.)
5. Moves semantic ownership from LLM to Java? **Yes** —
   UC-A↔UC-C follow-up detection is exactly the
   "drift / topic shift" decision §1.3 LLM-owns clause names.
6. Adds prompt if-else? Not applicable — this PR is in Java, not
   prompt. No new finding.
7. Preserves tool schema / capability / PII / grounding boundaries?
   **Yes** technically, but shrinks LLM ownership per Q5.
8. Generalization eval coverage? **Not stated** — and the
   scenario says no sunset plan, which usually implies no
   neighbor / negative / shadow coverage either.
9. Rollback / sunset plan? **No** — the scenario explicitly says
   "no sunset plan".

Q1 fires, Q2 fails, Q5 fails, Q8 missing, Q9 fails. The pattern is
the canonical semantic hardcode: encoding a soft drift decision
into Java keyword pattern-matching, with no Tier-0 anchor and no
exit ramp.

**Verdict: `reject as semantic hardcode`.** (Matches the expected
verdict in the Sprint 17 success metrics.)

Recommended layer for the corrected fix per §3.2: `prompt_projection`
(surface UC-A↔UC-C candidate signal as a soft slot) and let the LLM
own the routing decision. See Section 6 of this handoff for the
demonstration Sprint 18 stanza.

## 6. Hypothetical Sprint 18 stanza (demonstration)

The Section 7 stanza template is mechanically followable. Using
`cs_example_001` as the target failure and the corrected `prompt_projection`
fix from the Anti-Hardcode self-walk above:

```markdown
## Layer-classification + anti-hardcode stanza

**Target failure layer:** prompt_projection

**Tier-0 invariant:** This sprint adds no Tier-0 invariant. The
Constitution's drift / topic-shift bullet (§1.3 LLM owns) governs
the decision; the projection sits inside the Runtime's "trace and
eval contract" responsibility (§1.4). See
`docs/runtime_freeze_and_risk_policy.md` §1 / §2 for the current
Tier-0 invariant set — none of them are touched by this sprint.

**Semantic hardcode:** No semantic hardcode introduced. A new
projected slot `alternate_candidate_use_cases` is added to the
per-turn projection, fed by the existing `RuntimeIntentClassifier`
(Sprint 10 §L0). The LLM owns whether to acknowledge the
alternate-UC signal. No regex, no per-UC matrix, no new prompt
if-else.

**Generalization coverage:** target = UC-A↔UC-C drift (including
`cs_example_001` once promoted from hypothetical to real CaseSpec
in G2). Neighbor = UC-A↔UC-D and UC-A↔UC-F drift on the same
projection slot. Negative = UC-A single-issue follow-up that
should *stay* in UC-A (no false positive on the soft signal).
Shadow = held-out UC-A↔UC-C and UC-A↔UC-D drift traces, not
visible to the dev agent. Case counts deferred to the G2
case-family sprint; this Sprint 18 stanza names the required
case families and the projection slot.
```

A deliver agent can paste this stanza into a future
`docs/sprint_objective.md` and fill in the case counts once G2
lands the case families. No further interpretation is required.

## 7. Sprint objective met? (per success-metric line)

- ✅ `docs/current/iteration_governance.md` carries all 6 sections
  in the exact order specified in G0.1, with the front matter from
  G0.1.
- ✅ The Fix Layer Classification checklist's 7 questions evaluate
  to exactly one layer for each of the 5 hand-walked scenarios.
  See §4 of this handoff.
- ✅ The Anti-Hardcode review prompt produces `reject as semantic
  hardcode` for the hand-walked PR scenario. See §5 of this handoff.
- ✅ `AGENTS.md` is non-empty and resolves to a transitive load of
  the three governance docs (Option A). 2,804 bytes; three
  `@docs/current/...` include lines.
- ✅ The G0.3 sprint-objective stanza is mechanically followable.
  Demonstrated in §6 of this handoff.
- ✅ `docs/action_bank.md` lists Sprint 17 (G0) (§1 current phase
  + §3 deliverables) and the G1 / G2 backlog (§5.1).
- ✅ "No code touched, no tests run" stated explicitly in §3.
- ✅ No file under `docs/sprints/`, `docs/archive/`,
  `docs/foundational/`, `server/`, `ui/`, `eval/`,
  `eval_interactive/`, `data/`, or `scripts/` was modified.

## 8. Open questions for human

The following were noticed during G0 but are out of scope to act on:

1. **Sprint 5 → Sprint 17 layer-set rename.** The Sprint 5 taxonomy
   (`prompt_context_projection`, `skill_orchestration`,
   `case_spec_eval`, `infra_runtime`, `product_policy_gap`,
   `unknown_needs_human_review`) was renamed by the Sprint 17
   objective to a shorter set (`prompt_projection`, `skill_state`,
   `eval_spec`, `infra`, `product_policy`, `human_review_required`).
   The Sprint 17 names are now canonical via
   `iteration_governance.md` §3.1. The legacy Sprint 5 doc
   (`docs/fix_layer_taxonomy.md`, if it exists or returns) should
   be marked `superseded_by` or carry a `notes:` cross-reference
   on the next fold-back. I did not edit it (Sprint 5 archives are
   immutable).
2. **`docs/sprint_objective.md` `Date:` line.** The file is dated
   2026-05-11; the human-promoted Sprint 17 objective was modified
   pre-sprint. No action needed; this is just a confirmation
   of the date.
3. **Tier-0 terminology.** `docs/runtime_freeze_and_risk_policy.md`
   uses "hard invariants" / "frozen runtime contract" in §1–§2, not
   the literal phrase "Tier-0". The Constitution and several sprint
   archives use "Tier-0". A future fold-back of
   `runtime_freeze_and_risk_policy.md` could explicitly name the
   §1–§2 invariants as "Tier-0" to make
   `iteration_governance.md` §3.2 Q2's cross-reference fully
   literal. Not blocking; the link target is unambiguous from
   context.
4. **`docs/diagnostics/failure-briefs/` directory.** I chose not to
   create a `.gitkeep` placeholder. Section 2 of
   `iteration_governance.md` and the §5.1 G1 row both clearly
   state the directory will materialize during G1. If the human
   prefers the path to resolve before G1 lands, a follow-up
   `.gitkeep` PR is trivial.

## 9. Next recommended action

Open Codex review of this sprint using
`.agent-prompts/sprint-017-g0-review-prompt.md` (or the human-edited
equivalent). Codex's scope is defined in `docs/sprint_objective.md`
§ Review rule: internal consistency of the six sections, mechanical
resolution of the five layer-classification scenarios and the one
anti-hardcode PR scenario, AGENTS.md transitive load, stanza
mechanical-followability, action_bank.md state, and diff-stays-in-docs.

Expected outcome on a clean review: `decision: pass,
blocking_count: 0`. If the review surfaces a layer that does not
resolve to a single answer, or an internal contradiction between
sections, the expected outcome is `decision: fix_required,
blocking_count: ≥1` with a named target file and minimal-fix
description per the Sprint 16 review precedent.
