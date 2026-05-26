Paste the content below this line into a fresh Codex session after the dev's fix commit lands.

---

# Sprint 21 fix re-review prompt

You are the Anti-Hardcode Review Agent re-reviewing the Sprint 21
**fix iteration ONLY**. The parent Sprint 21 close commit
returned `decision: fix_required` / `blocking_count: 3` on three
approved-override evidence-gap findings (cs_001, cs_095, cs_192
each missing verbatim brief-field quotes mandated by the §1.7
evidence-package gate). The dev agent has now landed a fix
commit on `design-v1-without-human-review`. Re-review that fix
commit against the fix-iteration objective at
`docs/sprint_objective.md` "## Sprint 21 fix iteration" section.

## Scope

Diff scope: `HEAD^..HEAD` (fix commit). Expected surface: only
`docs/sprints/sprint-021-handoff.md`. Apply §4.1 narrowly.

Read: `AGENTS.md` + governance chain; `docs/sprint_objective.md`
(parent + `## Sprint 21 fix iteration` append after the `---`);
`docs/codex-findings.md` (prior review, lines 8–67); the three
briefs at `docs/diagnostics/failure-briefs/cs001-...`,
`cs095-...-skipped.md`, `cs192-...-giveaway-question.md`; the
three §1.7 paragraphs and new `## Fix iteration` section in
`sprint-021-handoff.md`.

## Per-finding resolution checklist

### Finding 1 — cs_001 at handoff line ~270

- Verbatim quote from `cs001-...-faq-miss.md:18` (`What
  happened?` body)? Byte-for-byte against source.
- Verbatim quote from `cs001-...-faq-miss.md:24` (`What
  should...` body)? Byte-for-byte.
- `path:line` citation on each new quote?
- Each quote from the dimension being overridden
  (escalation_trigger / bot_handling_pattern), not irrelevant
  content?

### Finding 2 — cs_095 at handoff line ~431 (LOAD-BEARING)

- Verbatim quote from `cs095-...-skipped.md:27` (`What
  happened?` body)? Byte-for-byte.
- Verbatim quote from `cs095-...-skipped.md:50` (`What
  should...` body)? Byte-for-byte.
- `path:line` citation on each new quote?
- **Dimension-distinction check (load-bearing).** The existing
  paragraph (handoff lines 431–450) distinguishes the override's
  dimension (UC classification — bot's user-facing CONTENT was
  correct UC-D) from orthogonal dimensions where the bot was
  wrong (account-state investigation, duplicated greeting "Hi
  Trish! Hi Trish", internal SF source_id leak
  `ka44J000000gKxqQAE`, T1 PLACEHOLDER_WITHOUT_FOLLOWUP stall).
  Verify after fix:
  - "Five stacked failures" / orthogonal-failures language
    intact.
  - "remain real bot bugs at `prompt_projection` /
    `semantic_planner` that this eval_spec override does NOT
    widen eval to accept" sentence intact.
  - "regardless of UC classification" hard-fail note intact.
  - New quotes are ADDITIVE, not substitutive.
- If the dimension distinction is softened or restructured in
  any way, mark this finding **not resolved** — that is a
  substantive regression more serious than the original blocker.

### Finding 3 — cs_192 at handoff line ~576

- Verbatim quote from `cs192-...-giveaway-question.md:22`
  (`What happened?` body)? Byte-for-byte.
- Verbatim quote from `cs192-...-giveaway-question.md:41`
  (`What should...` body)? Byte-for-byte.
- `path:line` citation on each new quote?
- Each quote consistent with the override's dimension
  (secondary_ucs dedupe; zero scoring impact)?

## Substantive override re-verification (do NOT re-litigate)

Run `git diff --name-only HEAD^..HEAD`. Confirm NOT in diff:

- `eval_interactive/case_spec_overrides.yaml`. Even a
  whitespace-only delta is **blocking** as out-of-scope edit —
  substantive overrides were verified non-blocking in the prior
  review (`applied: 17`, `pending: 0`, three Sprint 21
  `source_session_id`s present); editing re-opens the
  substantive question.
- `eval_interactive/case_specs/case_families/**` and
  `eval_interactive/case_specs_shadow/case_families/**` (cascade
  rule).
- `eval_interactive/eval_interactive/case_spec/llm_persona_reviewer.py`.
- `server/**`, `data/**`, prompts, governance, foundational,
  prior sprint archives.

Do NOT re-run §4.1 Q1–9 against the dev's substantive
dispositions. Those passed prior review. Re-review's job is
evidence-gap closure plus cs_095 dimension distinction only.

## §4.2 header replacement

REPLACE the existing 4-line header at the TOP of
`docs/codex-findings.md` in place (do NOT preserve the
`fix_required` header). New header:

```
## Sprint Review Decision (Sprint 21 fix re-review)
decision: pass | fix_required | out_of_scope_review
blocking_count: <number>
summary: <one paragraph>
```

`pass`: name the three findings closed; confirm six verbatim
quotes present, cs_095 distinction held, YAML unchanged.

`fix_required`: list which quote is missing/non-verbatim, or how
the cs_095 distinction was softened, with line references.

`out_of_scope_review`: name the out-of-scope edit; recommend
remediation.

## Packaging-rollforward rule (READ BEFORE FLAGGING)

Deliver-agent-owned files the dev was instructed NOT to stage:
`docs/sprint_objective.md` (modified — fix-iteration append),
`compact/sprint-021-fix-dev-prompt.md`,
`compact/sprint-021-fix-review-prompt.md` (this file), possibly
`compact/sprint-deliver-orchestrator.md`.

If they ARE in the fix commit (dev staged by mistake), per
`feedback_out_of_scope_review_packaging_rollforward.md`: if the
only blocking finding would be the commit-boundary observation
on these files, return `pass` with a one-line packaging note in
the §4.2 summary ("fix commit also includes deliver-agent-owned
files; substantive findings closed; deliver agent rolls forward
at sprint close"). Do NOT block on packaging alone, split
history, or re-run.

## Out-of-scope concerns → action_bank

Any opinion on Sprint 21's substantive dispositions, the smoke
regression, G3+ order, or other R-items: record as a deferred
item in `docs/action_bank.md` (or recommend the deliver agent
record it), NOT as a blocking finding. Blocking authority is
strictly limited to the three evidence-gap findings + the
cs_095 dimension-distinction check.

## Do not

- Do NOT broaden re-review beyond the three §1.7 paragraphs and
  the cs_095 distinction.
- Do NOT propose code fixes; name the §3 layer and stop.
- Do NOT edit any file other than `docs/codex-findings.md`
  (header replacement).
- Do NOT propose a new Tier-0 invariant.
- Do NOT recommend re-running smoke / full eval.

When the per-finding checklist passes, the dimension distinction
holds, and the substantive YAML is unchanged, return `pass`. The
sprint closes at the deliver agent's next turn.
