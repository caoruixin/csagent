# Sprint 106 / WS-2-B — dev prompt: finish the CaseSpec rewrite on `promotion/` and `case_families/`

> Paste this whole file into a fresh dev session. It is self-contained per
> `prompt-artifact-rules.md` §9.1: you need nothing else except the
> `AGENTS.md` governance chain, which loads automatically.
>
> **This file IS the contract** (§9.3.1). There is no separate
> `sprint_objective.md` to cross-check. If scope changes mid-sprint, it
> changes here, in place.
>
> **This sprint runs in a git worktree, in parallel with Sprints 104 / 105 /
> 107.** §0.2 defines what you own and what you must not touch. Read it before
> writing anything.

## 0. Start here

### 0.1 Create your worktree

You are starting in the primary checkout `/Users/caoruixin/projects/csagent`,
whose tip is `d7d84f86` on branch `perf-replan-2026-07`. Do not work there.

If your session offers an `EnterWorktree` tool, use it. Otherwise:

```bash
cd /Users/caoruixin/projects/csagent
git worktree add ../csagent-wt-106 -b sprint-106-casespec-promotion perf-replan-2026-07
cp .env.local ../csagent-wt-106/.env.local     # gitignored; git worktree does NOT carry it
cd ../csagent-wt-106
git log --oneline -1                           # must read d7d84f86
cd eval_interactive && uv sync                 # your worktree has no .venv yet
```

### 0.2 Path ownership — three other sessions are live right now

| Sprint | Owns (writable) |
|---|---|
| 104 | `server/**` |
| 105 | `eval_interactive/eval_interactive/**` (the Python package), `eval_interactive/tests/**` **except** the two categories named below |
| **106 (you)** | `eval_interactive/case_specs/promotion/**`, `eval_interactive/case_specs/case_families/**`, `eval_interactive/case_spec_overrides.yaml`, `eval_interactive/tests/regression/test_case_spec_overrides.py`, any test file whose only assertion is a **corpus count anchor**, `docs/sprints/sprint-106-handoff.md`, this prompt file |
| 107 | `eval/**`, `data/**`, `Makefile` |

Two carve-outs from Sprint 105's `tests/**` ownership are yours: the
override-regression test, and count-anchor tests (e.g. "the corpus contains N
bad_cases"), which your spec edits may legitimately move. **Name every test file
you touch in your handoff.** If you and Sprint 105 both need the same test file,
**you yield** — record it as a merge note instead of editing.

**You may read anything. You may write only inside your owned paths.** No change
under `server/**`, no change to the Python scoring/loader code (that is Sprint
105's, and it is changing under you). Never edit `docs/sprints/*` (other than
your own handoff), `docs/archive/*`, or `docs/proposals/*`.

### 0.3 Shared resources — you need none

Sprint 104 holds the exclusive backend token. You do **not** need the backend,
Postgres, or any bot session: this sprint is corpus work, verified by the
linter, the loader and pytest. Do not restart the backend. Do not run bot
sessions. Do not resume the autoloop.

### 0.4 Read before coding, in this order

1. `docs/current/iteration_governance.md` — §1.6, §3 (fix-layer
   classification), **§5.4 (no eval-side override of a real bug — the governing
   fence for this sprint)**, §5.1 (acceptance bars).
2. `docs/proposals/performance_priority_replan_2026-07.md` §2 (the three product
   decisions), §3 WS-2, §5.
3. `git show 74de32c3` — the WS-2 commit that did `anchor/` + `smoke/`. **Read
   its full message and its diff on two or three specs.** It is your worked
   precedent: what a flip looks like, what a keep looks like, what the two-stage
   conversion looks like, and the rationale block it writes into each changed
   file.
4. `docs/current/process/badcase-lifecycle.md` §5.6 — how the curated suite is
   gated, so you understand what your edits do and do not authorise.

## 1. Why this sprint exists (evidence already collected)

The CaseSpec generator read *"a human agent took this over"* out of historical
transcripts as *"the bot should escalate"*
(`eval_interactive/eval_interactive/case_spec/case_outcome_resolver.py:196-206`,
`transcript_evidence.py:146-208`). Because the corpus is by definition sessions
that reached a human, the regex fired almost everywhere, and FAQ-resolvable use
cases were stamped `escalate`. The linter's R1 rule had a carve-out that waived
exactly this contradiction, and the linter had no CLI entry point at all, so it
was invisible until WS-1 (`fb9c64ff`) wired it in.

**An agent optimised against this target is being trained to escalate.** That is
Loop A and Loop B of the re-plan, seen from the corpus side.

WS-2 (`74de32c3`) fixed `anchor/` and `smoke/` — the programmatic gates —
under the product principle taken on 2026-07-25:

> **Data-modifying asks go to a human. Explanation-class and read-only status
> queries are the bot's to finish.** (D1 promotes read-only user-data status
> query into scope; D2 revises "customer indicates frustration ⇒ must escalate"
> to "de-escalate and keep solving first"; high-risk topics — fraud, safety,
> legal — keep immediate escalation.)

Results of that pass: 51 flips, 23 deliberate keeps (genuine data modification
or an explicit human demand), 6 mixed asks converted to two-stage.
`anchor/` R1 violations 74 → 20; `smoke/` 6 → 3.

**What is left is `promotion/` and `case_families/`,** measured with the repo's
own linter on 2026-07-26:

| bucket | errors | of which R1 | of which R2 |
|---|---|---|---|
| `promotion/` | 64 | 64 | 0 |
| `case_families/` | 22 | 17 | 3 |
| (`anchor/` after WS-2) | 20 | 20 | 0 |
| (`smoke/` after WS-2) | 3 | 3 | 0 |
| (`bad_cases/`) | 16 | 2 | 3 |

Method, cite it verbatim in your handoff:
`cd eval_interactive && uv run eval-interactive lint --path case_specs/<bucket> --summary-only --exit-zero`.

**Do not count these with `grep`.** WS-2 wrote a rationale block into every file
it changed, and that block quotes the strings `should_escalate: true` and
`allow_bot_resolution: 'true'`. A naive text search therefore still reports the
pre-WS-2 count of 158 and is wrong. Use the linter or the loader.

Why `promotion/` specifically, and why now: per the re-plan §1.4, resolve-class
intent-switching coverage lands **almost entirely on `promotion/`**
(`exploration/` is 106/107 `hard_shift` but all intake UCs stamped `escalate`;
`anchor/` has zero `hard_shift`; `smoke/` and `bad_cases/` exclude drift by
construction). Sprint 104 is measuring drift behaviour on `promotion/` specs
right now. Their expectations are the ruler that work will be judged against.

The 64 contradictory `promotion/` specs by declared UC:
`UC-A 20`, `UC-C 16`, `UC-F 12`, `UC-D 7`, `UC-FP 6`, `UC-E 3`.

## 2. Goal

Every `promotion/` and `case_families/` spec expects the outcome a good CS agent
would actually produce on that customer's ask, under the D1/D2 product
principle — so that the corpus stops teaching escalation, and so Sprint 104's
drift measurements are graded against a defensible expectation.

**The goal is not "R1 count → 0".** A spec that genuinely should escalate and
whose UC is nominally FAQ-capable is a legitimate R1 violation, and the right
outcome for it may be to keep the expectation and record why. WS-2 kept 23 of 80
on `anchor/`+`smoke/`. Judging by the sample of `promotion/` asks — "remove this
1-star feedback", "resolve a duplicate charge", "remove a false review" — the
keep rate here will likely be **higher**, not lower. A handoff reporting
"32 flips, 30 keeps, 2 two-stage" with per-case reasoning is a better result
than one reporting 64 flips.

## 3. Scope (numbered)

1. **Adjudicate all 64 `promotion/` R1 specs, one at a time, against the
   customer's actual ask.** For each, decide and record one of:
   - **flip to `resolve`** — the ask is explanation-class or a read-only status
     query, and the bot can finish it. Representative candidates from the
     sample: `cs_interactive_003` (UC-C, "my house-clearance ad has received no
     inquiries" — explanation), `cs_interactive_023` (UC-FP, "does reposting a
     puppy ad count as a new listing" — policy explanation).
   - **keep `escalate`** — the ask requires modifying data the bot cannot touch,
     or is high-risk (fraud / safety / legal), or the customer explicitly
     demands a human. Representative candidates: `cs_interactive_017` /
     `cs_interactive_018` / `cs_interactive_020` (remove a rating or review —
     data modification), `cs_interactive_007` / `cs_interactive_022` (payment /
     refund adjustment).
   - **convert to two-stage** — the ask has an explanation half and a
     data-modifying half. Use the existing
     `conditional_outcome_acceptance` + `require_closure_precondition`
     mechanism, exactly as WS-2 did for `wmkb` / `cs095` / `iwzx`: require the
     explanation/read-only half to be answered, *then* permit handover. Check
     that `outcome_checks` is non-empty — WS-2 found two specs where an empty
     `outcome_checks` would have left the new rule inert.
   - **restructure / demote** — the persona's goal is unsatisfiable in chat
     (WS-2's `cs_uc_a_loaded_listing` precedent: demoted to grounding-only,
     outcome not scored). Use sparingly and justify.

   Write the reason into the file, in the same rationale-block style
   `74de32c3` used, so the next reader does not have to re-derive it.

2. **Do the same for `case_families/`** — 17 R1 + 3 R2. R2
   (`uc_allow_bot_resolution_consistent`) is a different contradiction from R1
   and may require changing `allow_bot_resolution` rather than the outcome;
   diagnose before editing.

3. **Fix `promotion/cs_interactive_185`'s `primary_uc`.** Sprint 103 §6.6
   recorded it: the spec declares UC-D (Account & Login), but its scripted
   drift in every measured draw is toward editing live ads (UC-A/UC-B
   territory). Decide on evidence from the spec's own `user_goal_summary`,
   `seed_messages` and `hidden_facts` whether `primary_uc` or the drift is
   wrong, and fix the one that is. **Coordinate note:** Sprint 104 is measuring
   this exact spec in a parallel worktree. Change nothing about it beyond what
   the evidence requires, and flag the change prominently in your handoff so
   Sprint 104's evidence can be re-attributed at integration.

4. **Sweep `case_spec_overrides.yaml` for overrides that would re-assert.** WS-2
   found approved L3 overrides pinning escalate-shaped expectations for two
   flipped sessions; left in place they would have re-asserted on the next
   regeneration and emitted an `escalation_trigger` on a `should_escalate:
   false` spec, which `Expected.__post_init__` raises on — a latent load
   failure, not a no-op. Check every override whose `source_session_id` matches
   a spec you change. Overrides are approved artefacts: if one contradicts your
   flip, the correct move may be to leave the spec alone and report the
   conflict, not to delete an approval.

5. **Re-measure and report.** Per-bucket linter counts before and after, with
   the command cited. Rule-level breakdown, not just totals.

## 4. Hard fences

- **§5.4 governs this sprint.** You may not widen a CaseSpec to accept
  behaviour the bot got wrong. The direction of travel is *removing specs that
  accept over-escalation*, not *adding tolerance*. Concretely: adding
  `escalate` to an `acceptable_outcomes` list that did not have it needs an
  explicit, written justification, and "the bot currently escalates here" is
  **not** one.
- Do not dilute a signal to zero. WS-2 found `[resolve, escalate]` lists where
  both terminals scored 1.0 — the spec measured nothing. Prefer two-stage over
  a permissive list.
- No change to the Python package (`eval_interactive/eval_interactive/**`), to
  `server/**`, `eval/**`, `data/**`, `Makefile`, `autoloop/**`, `e2e/**`, or to
  `case_specs/` buckets other than `promotion/` and `case_families/`.
- Do not resume the autoloop.
- Do not author your own review prompt and do not dispatch Codex (§9.3.2).
- Do not merge to `perf-replan-2026-07` or `main`.

## 5. Layer-classification + anti-hardcode stanza (§7.1)

**Target failure layer:** `eval_spec`. This is the §3.2 question-6 case: the
CaseSpec is asking the system to do something it should not do (hand over an
explanation-class ask). Per-case, a spec may instead be adjudicated
`product_policy` — if whether the bot *may* take the action is a product
decision nobody has taken, do not decide it yourself: keep the current
expectation, mark the spec, and list it in the handoff for the product owner.

**Tier-0 invariant:** This sprint adds no Tier-0 invariant and changes no
runtime code.

**Semantic hardcode:** No semantic hardcode introduced. This sprint changes
expectations in data, not decision logic; no keyword, regex or enum is added
anywhere. If a spec seems to need a new enum value to express its expectation,
stop and report — a frozen enum the CaseSpec wants to extend is itself an
`eval_spec` finding (§3.2 question 6).

**Generalization coverage:** target / neighbor / negative / shadow =
`64 / 17 / ≥6 / 0`. Targets: the 64 `promotion/` R1 specs. Neighbours: the 17
`case_families/` R1 specs. Negative controls: at least six specs you deliberately
**keep** as `escalate` (data-modifying or high-risk), which must not change
verdict — plus the two `drift_behavior: none` anchors
(`anchor/cs_interactive_030`, `anchor/cs_interactive_155`) which you must not
touch at all. Shadow set is not available to you by construction (§5.1).

## 6. Baselines and test requirements

**Record before touching any file**, citing commands:

- Per-bucket linter counts (§1's table) — re-run all five buckets yourself.
- `cd eval_interactive && uv run pytest -q` → expect
  `764 passed, 14 failed, 5 skipped`. Of the 14: 13 come from the missing
  `data/human_review_annotations_2026-04-22_golden.csv` (Sprint 107's scope) and
  1 is a stale Skill-count assertion (Sprint 105's scope, item 5 of its
  contract). **Neither is yours.** If your pytest count moves for any other
  reason, that is your regression.
- `cd server && mvn -o test` → run once at the end only, to prove `server/**`
  is untouched: expect `Tests run: 1493, Failures: 1` unchanged.

Note: Sprints 105 and 107 are changing the pytest baseline under you (105 fixes
the stale assertion, 107 may restore the CSV). Record your own numbers, and do
not treat their improvements as your regression.

**These numbers may be stale.** Re-measure; attribute any delta before
proceeding rather than reverting in-scope work to match a number.

Required: every spec you change must load. Run the loader over the full corpus
after your edits (the linter run does this) and prove zero load failures —
`Expected.__post_init__` raises on an `escalation_trigger` with
`should_escalate: false`, and that is exactly the failure mode item 4 guards
against.

## 7. Evidence requirements

This sprint's evidence is **adjudication quality**, not a pass-rate. Required:

1. A per-spec decision table: spec id, declared UC, the customer's ask in one
   clause, decision (flip / keep / two-stage / restructure), and the one-line
   reason. All 64 + 17 rows. This table is the deliverable that matters most.
2. For every **flip**, the specific evidence in the spec itself that the bot can
   finish the ask — a `hidden_facts` entry, the `user_goal_summary`, the
   `closure_criterion`. WS-2's precedent: `cs011`'s own `hidden_facts` said the
   reset mail was in the spam folder while the spec demanded a handover.
3. For every **keep**, which of the three keep reasons applies (data
   modification / high-risk / explicit human demand).
4. Before/after linter counts per bucket, per rule, with the command cited.
5. Any spec you referred to the product owner rather than deciding (§5's
   `product_policy` exit), listed separately.

Do **not** run bot sessions to decide an expectation. The bot's current
behaviour is not evidence about what the expectation should be — treating it as
such is the §5.4 failure mode this whole workstream exists to undo.

## 8. Handoff requirements

Write `docs/sprints/sprint-106-handoff.md`. Required sections:

1. The per-spec decision table (§7.1).
2. Flip / keep / two-stage / restructure counts, and the reasoning for the
   overall mix — especially if the keep rate is high.
3. Before/after linter counts, per bucket and per rule, with commands.
4. The `cs_interactive_185` decision (§3.3), flagged for Sprint 104's
   re-attribution.
5. Every override touched or reported in `case_spec_overrides.yaml` (§3.4), and
   every test file touched under `eval_interactive/tests/**` (§0.2).
6. Specs referred to the product owner rather than decided.
7. Real defects found but out of contract — listed, not fixed. Expect these: the
   generator that produced these specs is still running the same
   `case_outcome_resolver` logic, so the next regeneration will re-create the
   contradiction unless something upstream changes. Say so if you conclude it.
8. Where you think this contract is wrong.
9. Self-check, ticked.

## 9. Commit discipline

- Commit in reviewable batches on `sprint-106-casespec-promotion` — group by
  decision class or by UC, not one commit per file and not one commit for all 81.
  Never `git add -A` blind; stage explicit paths.
- Each commit message states the decision class, the count, and the principle
  applied. `74de32c3` is the model.
- End every message with:
  `Co-Authored-By: Claude Opus 5 (1M context) <noreply@anthropic.com>`

## 10. Self-check before claiming done

- [ ] Worktree is `../csagent-wt-106` on `sprint-106-casespec-promotion`;
      nothing written outside my owned paths; `server/**` and
      `eval_interactive/eval_interactive/**` diffs are empty.
- [ ] All 64 `promotion/` + 17 `case_families/` R1 specs adjudicated
      individually; decision table complete with no blank reasons.
- [ ] Counted with the linter/loader, never with `grep` (§1).
- [ ] No `acceptable_outcomes` list gained `escalate` without written
      justification; no permissive `[resolve, escalate]` introduced.
- [ ] Full-corpus load succeeds; zero `Expected.__post_init__` failures.
- [ ] `case_spec_overrides.yaml` checked for every changed
      `source_session_id`; approvals not silently deleted.
- [ ] `cs_interactive_185` decision flagged for Sprint 104.
- [ ] Every touched test file named in the handoff; where Sprint 105 also needed
      one, I yielded.
- [ ] No bot session run; no backend restart; autoloop not resumed; no Codex
      dispatched; no merge.
