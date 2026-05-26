Paste the content below this line into a fresh Claude Code session on branch `design-v1-without-human-review`.

---

# Sprint 21 — Fix-iteration dev prompt

Codex returned `decision: fix_required` / `blocking_count: 3` on
Sprint 21 commit `5cbb373`. All three blockers are
**approved-override evidence-gap** findings: each of cs_001,
cs_095, cs_192 is missing the verbatim brief-field quotes the
parent Sprint 21 review prompt made blocking under the §1.7
evidence-package gate. Your job is narrow: paste in the missing
verbatim quotes at three points in `docs/sprints/sprint-021-handoff.md`,
append a small `## Fix iteration` section at the bottom, commit.

## Loader stanza (read in this order)

1. `AGENTS.md` (transitively loads `doc_governance.md`,
   `agent_context_guide.md`, `iteration_governance.md`).
2. `iteration_governance.md` §1 / §3 / §5 / §7.
3. `docs/sprint_objective.md` — parent Sprint 21 objective + the
   appended `## Sprint 21 fix iteration` section (after the `---`
   rule). That section is your authoritative scope and contains
   the cs_095 dimension-distinction clause + stop-conditions.
4. `docs/codex-findings.md` — three blocking findings, lines
   8–67.
5. `docs/sprints/sprint-021-handoff.md` — three §1.7 self-check
   paragraphs at lines ~270, ~431, ~576, read in surrounding
   disposition context.
6. The three source briefs at
   `docs/diagnostics/failure-briefs/cs001-uc-c-template-escalate-on-faq-miss.md`,
   `docs/diagnostics/failure-briefs/cs095-uc-classification-and-account-aware-path-skipped.md`,
   `docs/diagnostics/failure-briefs/cs192-uc-b-mechanical-escalate-on-resolvable-giveaway-question.md`.

## "Verbatim" rule

Reproduce the brief's text exactly. Italics OK; paraphrasing,
summarizing, mid-sentence trimming NOT OK. Each new quote block
carries a `path:line` citation matching the dev's existing
Ground-truth-chain quote style ("From the brief's `What
happened?` field (path:line): ...").

The line numbers Codex cited point to the **headings** (`##
What happened?` / `## What should a good CS agent have
done?`); the field body follows immediately below each
heading.

## The three findings

### Finding 1 — cs_001 at handoff line ~270

Existing paragraph quotes Ground-truth chain lines 14 + 16 only.
Insert two new verbatim quote blocks:

- One from `cs001-...-faq-miss.md:18` (`What happened?` body —
  the paragraph beginning "User said via form_context...").
- One from `cs001-...-faq-miss.md:24` (`What should...` body —
  the paragraph(s) below the heading).

Override's dimension: `expected.escalation_trigger` /
`expected.bot_handling_pattern`. Quote selections should
reinforce that on that dimension the bot's
`faq_miss_threshold_exceeded` reason is correct (the §1.7-clean
shape). Existing prose already covers the bot's separate
L3-quality failure as not papered over; do not duplicate that.

### Finding 2 — cs_095 at handoff line ~431 (LOAD-BEARING)

Existing paragraph quotes Ground-truth chain line 19 only.
Insert two new verbatim quote blocks:

- One from `cs095-...-skipped.md:27` (`What happened?` body —
  the paragraph beginning "User entered via the Account Support
  form...").
- One from `cs095-...-skipped.md:50` (`What should...` body —
  the paragraph beginning "The capability gap is account-state-
  aware investigation...").

**LOAD-BEARING CONSTRAINT.** The brief documents five stacked
failures (UC routing skipped, generic FAQ answer instead of
account-aware investigation, duplicated greeting, internal SF
source_id leak, T1 stall). The approved override changes ONLY
the UC classification dimension. The existing §1.7 self-check
(handoff lines 431–450) already distinguishes:

- The override's dimension: UC classification — bot's user-
  facing CONTENT was correct UC-D, UC STAMP was wrong UC-A. The
  override is the artefact that needed adjustment.
- The orthogonal dimensions: account-state investigation
  skipped, "Hi Trish! Hi Trish" duplicated greeting,
  `ka44J000000gKxqQAE` internal ID leak, T1
  PLACEHOLDER_WITHOUT_FOLLOWUP stall — these remain real bot
  bugs at `prompt_projection` / `semantic_planner` that this
  override does NOT widen eval to accept.
- The hard-fail note: "the 2026-05-05 run still hard-fails
  composite 0.0 on L1 no_stall regardless of UC classification".

**Your fix MUST preserve that distinction word-for-word.**
Quotes are ADDITIVE, not substitutive. Select quotes from the
field's content about the dimension being overridden (UC
classification), not from content about orthogonal failures. If
you find yourself rewriting the surrounding paragraph to fit the
quotes in, **stop and surface**.

### Finding 3 — cs_192 at handoff line ~576

Existing paragraph argues zero-scoring-impact dedupe but cites
no brief fields. Insert two new verbatim quote blocks:

- One from `cs192-...-giveaway-question.md:22` (`What happened?`
  body — paragraph beginning "User entered via the Ad Support
  form...").
- One from `cs192-...-giveaway-question.md:41` (`What should...`
  body).

Override's dimension: `classification.secondary_ucs` dedupe (UC-B
duplicate removed; primary stays UC-B). Quotes should pin the
dimension (the bot's UC-B primary stamp was correct; dedupe is
generator-quirk correction with zero scoring impact). Existing
prose already covers the bot's separate UC-B template-escalate
failure as captured by the Sprint 20 cs192 case family and not
papered over.

## Files in scope

- `docs/sprints/sprint-021-handoff.md` only:
  - Three §1.7 paragraphs at lines ~270, ~431, ~576 (additive
    quote insertion only).
  - New `## Fix iteration` section appended at bottom.

## Files NOT in scope (hard fence)

- `eval_interactive/case_spec_overrides.yaml` — DO NOT TOUCH.
  Codex's non-blocking checks confirmed the substantive overrides
  are correct (`applied: 17`, `pending: 0`, three Sprint 21
  `source_session_id`s present). Editing this file is scope
  drift.
- `eval_interactive/case_specs/case_families/**` and
  `eval_interactive/case_specs_shadow/case_families/**` (cascade
  rule still in force).
- `eval_interactive/eval_interactive/case_spec/llm_persona_reviewer.py`.
- All other `eval_interactive/case_specs/**` (smoke / anchor /
  promotion / exploration).
- `server/**`, `data/**`, FAQ corpus, prompts, personas.
- All `docs/sprints/**` other than `sprint-021-handoff.md`.
- All `docs/archive/**`, `docs/current/**`, `docs/foundational/**`,
  `docs/runtime_freeze_and_risk_policy.md`, `AGENTS.md`,
  `CLAUDE.md`.
- `docs/sprint_objective.md` — deliver-agent owned; DO NOT edit.
- `docs/codex-findings.md` — review-agent owned; DO NOT edit.
- The 4 other Sprint 21 dispositions in `sprint-021-handoff.md`
  (cs_038, cs_040, cs_176,
  R-generator-get-customer-context-policy-mismatch) — not flagged
  blocking; do not touch.

## Commit-at-end working-tree heads-up

Per `feedback_commit_at_end_bundles_deliver_artefacts.md`, the
working tree at commit time will contain deliver-agent-owned
files you did NOT write:

- `docs/sprint_objective.md` (modified — the `## Sprint 21 fix
  iteration` append).
- `compact/sprint-021-fix-dev-prompt.md` (this file).
- `compact/sprint-021-fix-review-prompt.md` (Codex's prompt).
- Possibly `compact/sprint-deliver-orchestrator.md`.

**Do NOT stage these.** Stage only
`docs/sprints/sprint-021-handoff.md`. The deliver agent rolls the
others forward at sprint close. Codex's fix re-review prompt
knows to ignore them.

## Fix handoff section (append at bottom of `sprint-021-handoff.md`)

Append `## Fix iteration` at the file's bottom. The section
must:

- Name the three findings closed with line references to
  `docs/codex-findings.md` (8–26 for cs_001, 28–47 for cs_095,
  49–67 for cs_192).
- Exhibit the six new brief-quote blocks (two per finding) with
  `path:line` citations, so a reader sees them without diffing.
- Explicitly confirm the cs_095 dimension distinction at handoff
  lines 431–450 is preserved word-for-word — state that the
  "five stacked failures" / orthogonal-failures language, the
  "remain real bot bugs ... does NOT widen eval to accept"
  sentence, and the "regardless of UC classification" hard-fail
  note are intact.
- State `eval_interactive/case_spec_overrides.yaml` was NOT
  touched.
- Carry the fix commit hash.

## Stop conditions

- Any temptation to edit `case_spec_overrides.yaml` (including
  whitespace) — STOP and surface.
- Any rewrite of the cs_095 §1.7 paragraph beyond additive quote
  insertion (restructuring the dimension-distinction language,
  summarizing the orthogonal failures, removing the "regardless
  of UC classification" sentence) — STOP and surface.
- A `What happened?` / `What should...` field body so long that
  "load-bearing portion" quoting feels like cherry-picking —
  STOP and surface; do NOT paraphrase to fit.
- A quote that contradicts the approved override's substance
  (brief actually says the bot was wrong on the override's
  dimension) — STOP and surface; this would re-open the
  substantive question.
- `docs/sprint_objective.md` lacks the `## Sprint 21 fix
  iteration` section — STOP; context is wrong.

## Acceptance bars before commit

- All three §1.7 paragraphs (lines ~270, ~431, ~576) contain two
  new verbatim quote blocks each (six total), with `path:line`
  citations.
- cs_095 dimension-distinction language at handoff lines
  431–450 is byte-identical except for additive insertions (and
  any minor paragraph reflow they cause).
- `git diff --name-only HEAD..` after staging shows only
  `docs/sprints/sprint-021-handoff.md`.
- `## Fix iteration` section appended at handoff bottom.
- Deliver-agent files NOT staged.

When bars are met, commit on `design-v1-without-human-review`
with a message naming this as the Sprint 21 fix iteration
closing the three approved-override evidence-gap findings.
