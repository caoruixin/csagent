# Sprint 107 / WS-7-A — dev prompt: can the deterministic replay harness be revived, and what replaces the lost ground truth?

> Paste this whole file into a fresh dev session. It is self-contained per
> `prompt-artifact-rules.md` §9.1: you need nothing else except the
> `AGENTS.md` governance chain, which loads automatically.
>
> **This file IS the contract** (§9.3.1). There is no separate
> `sprint_objective.md` to cross-check. If scope changes mid-sprint, it
> changes here, in place.
>
> **This sprint runs in a git worktree, in parallel with Sprints 104 / 105 /
> 106.** §0.2 defines what you own and what you must not touch. Read it before
> writing anything.
>
> **This is an investigation-first sprint.** Its acceptable outcome includes
> "reviving this is not worth it, and here is the evidence". Do not force a
> revival that the evidence does not support.

## 0. Start here

### 0.1 Create your worktree

You are starting in the primary checkout `/Users/caoruixin/projects/csagent`,
whose tip is `d7d84f86` on branch `perf-replan-2026-07`. Do not work there.

If your session offers an `EnterWorktree` tool, use it. Otherwise:

```bash
cd /Users/caoruixin/projects/csagent
git worktree add ../csagent-wt-107 -b sprint-107-replay-harness perf-replan-2026-07
cp .env.local ../csagent-wt-107/.env.local     # gitignored; git worktree does NOT carry it
cd ../csagent-wt-107
git log --oneline -1                           # must read d7d84f86
```

### 0.2 Path ownership — three other sessions are live right now

| Sprint | Owns (writable) |
|---|---|
| 104 | `server/**` |
| 105 | `eval_interactive/eval_interactive/**`, `eval_interactive/tests/**` |
| 106 | `eval_interactive/case_specs/promotion/**`, `case_families/**`, `case_spec_overrides.yaml` |
| **107 (you)** | `eval/**`, `data/**`, `Makefile`, `docs/sprints/sprint-107-handoff.md`, this prompt file |

**You may read anything. You may write only inside your owned paths.** The
critical fence: **no change under `server/**`.** The replay harness is a
consumer of the server's code; Sprint 104 is changing that code right now. If
the harness cannot compile or run without a server-side change, **STOP and
report it** — that is a sequenced follow-up, not this sprint. Never edit
`docs/sprints/*` (other than your own handoff), `docs/archive/*`, or
`docs/proposals/*`.

### 0.3 Shared resources — read this carefully, you can break another session

Sprint 104 holds the **exclusive** backend token: it is running a live backend
on `:8080` against Postgres `:5442` and writing real session rows. You must not:

- start or restart anything on `:8080`;
- run `make ingest`, `make backend`, `make demo`, or any migration;
- drop, truncate, or schema-change anything in the `csagent` database;
- delete or overwrite anything under the primary checkout's `results/`.

If the replay harness needs a database, use a **separate database name** (e.g.
`csagent_replay`) or a testcontainer, and say which in your handoff. If it can
only run against the shared `csagent` database, **STOP and ask** rather than
running it.

Maven: run offline (`mvn -o`) so two worktrees do not race on `~/.m2`
downloads.

### 0.4 Read before coding, in this order

1. `docs/current/iteration_governance.md` — §1.6, §5.1, **§5.4**, §5.5, §3
   (fix-layer classification).
2. `docs/proposals/performance_priority_replan_2026-07.md` §3 WS-7 and §4 — the
   layered-evaluation argument this sprint tests.
3. `data/eval_datasets/HUMAN_REVIEW_GUIDE.md` and
   `data/eval_datasets/DATASET_REPORT.md` — what the human review process was
   and what it produced.
4. `git log --oneline -- eval/ | head -20` and `git show ff86337a --stat` — the
   last commit that touched the harness (2026-04-27).

## 1. Why this sprint exists (evidence already collected)

The re-plan's answer to *"should scale testing keep using an LLM simulator?"* is
**layer it, don't choose** (§3 WS-7):

- **Small-batch human spot-check** stays the primary gate (`bad_cases` run as
  `case_passed_authority: human_review`).
- **Mid-batch semantic regression** uses the LLM simulator — but only after WS-5,
  which landed on 2026-07-25 (`8dcd0449`: `goal_status` rubric,
  `drift_behavior` injection, judge decoupled from the simulator).
- **Large-batch deterministic regression** revives the `eval/` Java replay
  harness: **601 recorded real sessions, zero LLM calls on the user side**
  (`Makefile:27-28` → `cd eval && mvn verify -Peval-full`). It is structurally
  immune to simulator bias, which is exactly the bias the other two layers
  cannot audit themselves for.

Two facts, verified 2026-07-26:

- The harness is **unchanged since 2026-04-27** (`ff86337a`) — roughly three
  months, spanning M-Auto-9 through M-Auto-12 plus the entire WS-1..WS-6 series.
  It has not been compiled or run in that window.
- **`data/human_review_annotations_2026-04-22_golden.csv` is absent.** This is
  the file that carried human ground truth, and its absence is also the cause of
  **13 of the 14 current `eval_interactive` pytest failures** (the 14th is a
  stale Skill-count assertion, Sprint 105's item 5). `data/human_review_queue.csv`
  (102.8 KB) and `data/golden_dataset.csv` / `golden_turns.csv` **are** present.

Real data is plentiful: `data/eval_datasets/` (7 prepared datasets +
`DATASET_REPORT.md` + `HUMAN_REVIEW_GUIDE.md`), `data/filtered/` (6,835 filtered
real sessions), `docs/case-data-stat.md` (120,367 cases) for volume weighting.

## 2. Goal

Answer three questions with evidence, and act only on the ones the evidence
supports:

- **Q1 — Does the harness still run at all?** Compile it and run the smallest
  meaningful slice. Report exactly what breaks.
- **Q2 — What is recoverable of the lost human ground truth?** The annotations
  file is gone; the queue it was derived from may not be. Establish what can be
  legitimately reconstructed and what is permanently lost.
- **Q3 — Is deterministic large-batch replay worth reviving, given the other two
  layers?** The re-plan asserts "reviving it is far cheaper than writing a new
  simulator". Test that assertion against what you actually find, and be willing
  to contradict it.

## 3. Scope (numbered)

1. **Compile and characterise.** `cd eval && mvn -o verify -Peval-full`, or the
   narrowest target that exercises the replay path. Record verbatim: what
   compiles, what fails, and for each failure whether the cause is (a) drift in
   `server/**` APIs the harness calls, (b) the missing CSV, (c) config/env, or
   (d) rot in the harness itself. This characterisation is the sprint's primary
   deliverable and is valuable even if nothing else lands.

2. **Adjudicate the missing ground truth — do not manufacture it.** Establish,
   with evidence:
   - what `human_review_annotations_2026-04-22_golden.csv` contained (from
     `HUMAN_REVIEW_GUIDE.md`, the consuming test code, and git history — check
     whether it was ever tracked: `git log --all --oneline -- 'data/human_review_annotations*'`);
   - whether `data/human_review_queue.csv` or `data/golden_dataset.csv` carries
     the same rows in a different shape;
   - whether the 13 failing tests actually need *human* labels, or only need a
     file with the right schema.

   **Hard integrity fence: you may not synthesise human judgements.** Do not
   generate labels with an LLM, do not derive them from the bot's own behaviour,
   and do not rename another dataset to stand in for the golden file. Human
   ground truth that has been lost is lost; the honest remedies are to restore
   it from a real source, to re-run a real human review, or to re-anchor the
   tests on something that does not claim to be human ground truth. Anything
   else silently converts "the bot agrees with itself" into "the bot agrees with
   a human", which is the single most damaging thing that could be done to this
   repo's eval integrity (§1.6, §5.4).

3. **If — and only if — item 2 finds a legitimate restoration path**, restore the
   file and report the pytest baseline delta (14 failures → N). Sprints 105 and
   106 are measuring against the 14-failure baseline in parallel worktrees;
   flag the change prominently so their numbers can be re-attributed at
   integration.

4. **If item 2 finds no legitimate path**, propose the re-anchoring instead of
   doing it: name the 13 tests, say what each would assert without human labels,
   and state plainly what evaluative power is lost. Re-anchoring tests touches
   `eval_interactive/tests/**`, which is **Sprint 105's path** — propose, do not
   implement.

5. **Answer Q3 with a cost estimate, not an opinion.** Given item 1's findings:
   how much work is a revival, what would it measure that the other two layers
   cannot, and — the question that actually matters — **would it have caught any
   of the defects found in the last two weeks?** Test it against the concrete
   list: the `correct_outcome` asymmetry (WS-1), the 158 contradictory specs
   (WS-2), the frustration force-escalation (WS-3/D2), the dead
   `drift_behavior` injection (WS-5), the search-suppression false projection
   (P1-04), the DISCOVER clarification-budget exhaustion (Sprint 103 §6.2). A
   harness that would have caught none of them is worth less than the re-plan
   assumed, and saying so is a valid deliverable.

## 4. Hard fences

- **No `server/**` change.** If the harness needs one, stop and report.
- **No synthesised human ground truth** (§3.2). This is the sprint's most
  important fence.
- No touching `:8080`, `make ingest`/`backend`/`demo`, the shared `csagent`
  database schema, or the primary checkout's `results/`.
- No change under `eval_interactive/**` — that is Sprints 105 and 106.
- Do not resume the autoloop.
- Do not author your own review prompt and do not dispatch Codex (§9.3.2).
- Do not merge to `perf-replan-2026-07` or `main`.
- Large binaries: `data/` already holds an 18 MB CSV and a ZIP. Do not add new
  large artefacts to git; if a revival produces bulk output, gitignore it and say
  where it lives.

## 5. Layer-classification + anti-hardcode stanza (§7.1)

Per iteration_governance §7, a **pure-infra** sprint is exempt from this stanza.
This sprint is exempt on items 1, 3 and 5 — the eval framework's own
compilation, datasets and cost analysis change no semantic surface. The stanza
is stated anyway for item 2/4, which touch what the eval *means*:

**Target failure layer:** `infra` (the eval framework: replay harness,
datasets, baseline aggregation). Item 2's outcome may reclassify the 13 failing
tests as `eval_spec` — if so, that is a finding to report, not scope to take.

**Tier-0 invariant:** This sprint adds no Tier-0 invariant and changes no
runtime code.

**Semantic hardcode:** No semantic hardcode introduced; no keyword, regex or
enum is added anywhere.

**Generalization coverage:** case family not yet built; not applicable. This
sprint produces a characterisation and a cost estimate, not a behaviour change.
Per §5.8, note in the handoff whether item 1 uncovers an eval-framework `infra`
defect severe enough to preempt semantic sub-sprints — if it does, say so
prominently, because Sprints 104/105/106 are running right now.

## 6. Baselines and test requirements

**Record before touching any file**, citing commands:

- `cd eval && mvn -o verify -Peval-full` (or the narrowest equivalent) — the
  before-state, however broken. Capture the full failure output.
- `cd eval_interactive && uv run pytest -q` → expect
  `764 passed, 14 failed, 5 skipped`. Note which 13 are CSV-caused **by reading
  the failure output**, not by assuming: Sprint 103 §7 caught this contract
  family mis-attributing the 14th failure to the CSV when it was a stale
  Skill-count assertion. Verify the attribution yourself.
- `cd server && mvn -o test` → expect `Tests run: 1493, Failures: 1`. You will
  not change `server/**`; run once at the end to prove it.

**These numbers may be stale.** Re-measure and attribute any delta.

## 7. Evidence requirements

1. Item 1's failure characterisation, verbatim output, with each failure
   classified (a)/(b)/(c)/(d).
2. Item 2's ground-truth adjudication, with the git history search result
   quoted — including the case where the file was **never** tracked, which
   changes the story from "deleted" to "always local".
3. If you restored anything: the source it came from, and why it is legitimately
   the same data.
4. Item 5's cost estimate, as a table: defect from the last two weeks → would
   this harness have caught it (yes / no / only with new fixtures) → why.
5. A recommendation with a clear verdict: **revive / revive-narrowly / do not
   revive**, and what you would do with the effort instead.

## 8. Handoff requirements

Write `docs/sprints/sprint-107-handoff.md`. Required sections:

1. Item 1 characterisation (§7.1).
2. Ground-truth adjudication (§7.2), and explicitly: what is permanently lost.
3. What you changed, if anything, and the pytest baseline delta with a
   re-attribution note for Sprints 105/106.
4. The item 4 re-anchoring proposal, if item 2 found no restoration path — as a
   proposal, addressed to whoever owns `eval_interactive/tests/**` next.
5. Item 5's cost table and the §7.5 verdict.
6. Real defects found but out of contract — listed, not fixed. Flag any that
   meet the §5.8 preemption bar loudly.
7. Where you think this contract is wrong.
8. Self-check, ticked.

## 9. Commit discipline

- Small, reviewable commits on `sprint-107-replay-harness`. Never `git add -A`
  blind — `data/` is large and partly gitignored; stage explicit paths.
- If the sprint's outcome is "do not revive", the deliverable is the handoff
  plus the characterisation; committing no code change is a correct outcome, not
  a failure.
- End every message with:
  `Co-Authored-By: Claude Opus 5 (1M context) <noreply@anthropic.com>`

## 10. Self-check before claiming done

- [ ] Worktree is `../csagent-wt-107` on `sprint-107-replay-harness`; nothing
      written outside my owned paths; `server/**` and `eval_interactive/**`
      diffs are empty.
- [ ] Nothing started on `:8080`; no `make ingest`/`backend`/`demo`; shared
      `csagent` database schema untouched; primary checkout's `results/`
      untouched.
- [ ] **No human judgement was synthesised, inferred from bot behaviour, or
      substituted by renaming another dataset.**
- [ ] The 13-vs-14 pytest failure attribution was verified from output, not
      assumed.
- [ ] Item 5's cost table tested against the concrete two-week defect list.
- [ ] A clear revive / revive-narrowly / do-not-revive verdict is stated.
- [ ] Any §5.8-bar framework defect flagged prominently for the three parallel
      sprints.
- [ ] No Codex dispatched; no merge; no other sprint's paths touched.
