# Sprint 105 / WS-4 + Loop C — dev prompt: make the programmatic verdict able to tell good behaviour from bad

> Paste this whole file into a fresh dev session. It is self-contained per
> `prompt-artifact-rules.md` §9.1: you need nothing else except the
> `AGENTS.md` governance chain, which loads automatically.
>
> **This file IS the contract** (§9.3.1). There is no separate
> `sprint_objective.md` to cross-check. If scope changes mid-sprint, it
> changes here, in place.
>
> **This sprint runs in a git worktree, in parallel with Sprints 104 / 106 /
> 107.** §0.2 defines what you own and what you must not touch. Read it before
> writing anything.

## 0. Start here

### 0.1 Create your worktree

You are starting in the primary checkout `/Users/caoruixin/projects/csagent`,
whose tip is `d7d84f86` on branch `perf-replan-2026-07`. Do not work there.

If your session offers an `EnterWorktree` tool, use it. Otherwise:

```bash
cd /Users/caoruixin/projects/csagent
git worktree add ../csagent-wt-105 -b sprint-105-eval-verdict perf-replan-2026-07
cp .env.local ../csagent-wt-105/.env.local     # gitignored; git worktree does NOT carry it
cd ../csagent-wt-105
git log --oneline -1                           # must read d7d84f86
cd eval_interactive && uv sync                 # your worktree has no .venv yet
```

The recorded eval runs you will need as substrate are **gitignored** and exist
only in the primary checkout: `/Users/caoruixin/projects/csagent/results/`
(eight run directories, five of them Sprint 103's: `20260725-165045`,
`20260725-165354`, `20260725-165622`, `20260725-165835`, `20260725-170011`) and
`/Users/caoruixin/projects/csagent/eval_interactive/results/20260725-121244`.
**Treat them as read-only inputs** — reference them by absolute path or copy
them into your worktree; never write back into the primary checkout.

### 0.2 Path ownership — three other sessions are live right now

| Sprint | Owns (writable) |
|---|---|
| 104 | `server/**` |
| **105 (you)** | `eval_interactive/eval_interactive/**`, `eval_interactive/tests/**` **except** the two carve-outs below, `eval_interactive/case_specs/bad_cases/**` (only the `llm_judge_dimensions` field), `docs/sprints/sprint-105-handoff.md`, this prompt file |
| 106 | `eval_interactive/case_specs/promotion/**`, `eval_interactive/case_specs/case_families/**`, `eval_interactive/case_spec_overrides.yaml`, plus the two test carve-outs |
| 107 | `eval/**`, `data/**`, `Makefile` |

Two files/categories under `tests/**` are **Sprint 106's**, not yours:
`eval_interactive/tests/regression/test_case_spec_overrides.py`, and any test
file whose only assertion is a **corpus count anchor** (e.g. "the corpus contains
N bad_cases") — 106's spec edits legitimately move those. Do not edit them; if
one fails because 106 changed the corpus under you, record it as a merge note.
Conversely, if you and 106 both need the same file, 106 yields to you.

**You may read anything. You may write only inside your owned paths.** In
particular: **no change under `server/**`.** If your design concludes that the
bot must *emit* a new trace value for the ladder to work, that is a sequenced
follow-up, not this sprint — record it and derive what you can from the trace
fields that already exist. Never edit `docs/sprints/*` (other than your own
handoff), `docs/archive/*`, or `docs/proposals/*`.

### 0.3 Shared resources — you do NOT hold the backend token

Sprint 104 holds exclusive rights to start/restart the backend on `:8080` and
to run the real-LLM bot simulator this wave. **You must not restart the backend
and must not launch bot sessions.** Your evidence comes from re-scoring
*already recorded* traces (§3 item 1) — which is deterministic, cheap, and the
correct primary evidence for a scoring-layer change.

You may call the **judge** LLM (`JUDGE_*` in `.env.local`) — it is an external
provider, not the backend. Do not run `make ingest`, do not touch Postgres
schema.

### 0.4 Read before coding, in this order

1. `docs/current/iteration_governance.md` — §1.6 (eval is evidence, not
   authority), §5.1 (the acceptance bars), §5.4 (**no eval-side override of a
   real bug** — the central fence for this sprint), §5.5 (what is a gate and
   what is an observation), §3 (fix-layer classification).
2. `docs/proposals/performance_priority_replan_2026-07.md` §1.3 (Loop C), §2
   D3 (the ladder decision), §3 WS-4, §5.
3. `docs/current/process/badcase-lifecycle.md` §5.6 — the curated bad-case
   suite is the primary acceptance gate and its authority model
   (`case_passed_authority: human_review`) is what currently masks the defect
   you are fixing.
4. `docs/sprints/sprint-103-handoff.md` §5.4 — an example of a sprint that
   could not read its own outcome because of this defect.

## 1. Why this sprint exists (evidence already collected)

**Loop C — the programmatic verdict is structurally always 0.**

`eval_interactive/eval_interactive/scoring/composite.py:166-168` and `:225-243`:
`judge_score = mean(gating L3 scores) / 5.0`, else `0.0`; and
`composite = 0.5 * outcome + 0.5 * judge`. The pass gate is
`case_passed AND composite >= 0.7` (`batch/executor.py:514`, `:795`, `:1098`).

Consequently:

- All 19 `bad_cases` set `llm_judge_dimensions: []` ⇒ `judge_score = 0` ⇒
  `composite ≤ 0.5` ⇒ can never reach 0.7. Masked today by
  `case_passed_authority: "human_review"`, so nobody noticed.
- **It is worse than "bad_cases only."** The WS-5 validation on 2026-07-25 ran
  15 real sessions over five `promotion/` and `anchor/` specs — i.e.
  **programmatic-authority** specs — and every one scored `composite = 0.000`
  in all three arms *including the pre-change baseline*. The sharpest instance:
  `promotion/cs_interactive_179` under drift reached `correct_uc = 1.0`,
  `correct_outcome = 1.0`, `containment_outcome = resolved`, **zero L1
  failures** — and still FAILed, because its three L3 dimensions are all
  `advisory` (`llm_judge.py:498-503`, `_ADVISORY_DIMENSIONS`), so
  `judge_score = 0` and the composite cannot exceed 0.5.
- **A case where the bot did the right thing cannot pass.** This is the number
  the autoloop has been optimising, and the reason it is currently stopped.

A second, independent degradation on the same day: the judge had been sharing
the simulator's config, and pointing it at a model that rejects an explicit
`temperature` made **every L3 dimension 400 twice and fall back to
`_DEFAULT_SCORE = 3.0`** (`llm_judge.py:18`) — the whole L3 layer collapsing to
a constant behind one log line. WS-5 decoupled the config (`judge:` section in
`eval_interactive/eval_interactive.yaml:30-35`) and the current `JUDGE_MODEL` is
`deepseek-v4-pro`, but **the silent-collapse mode itself is still there**.

**D3 — the ladder decision (product owner, 2026-07-25).** Business success is
already a three-tier ladder in `docs/foundational/PRD_biz_part.md:66-67`
(D2 = "first resolution / material deflection: the user gets the correct next
step — status explanation, link, form", targeted at 45–55%). Eval carries only
the flat `resolved / escalated / abandoned`
(`eval_interactive/eval_interactive/trace/models.py:31`) and grades pass/fail on
a binary resolve-vs-escalate comparison. Consequently **"correct handover with
full context" and "careless premature handover" score identically.** WS-1
(`fb9c64ff`) closed the crudest version of this on the L1/L2 side; the verdict
model itself was left flat.

## 2. Goal

Two properties, both testable:

- **P1 — a case where the bot behaved correctly can pass.** No case may be
  structurally incapable of reaching the pass bar because of how its judge
  dimensions are configured.
- **P2 — the verdict distinguishes tiers of success.** "Explained the read-only
  half, then handed the data-modifying half over with context" and "handed over
  at turn 0" must not receive the same score.

And one anti-property, which outranks both:

- **P0 — nothing here may make a genuinely wrong bot behaviour pass.** §5.4.
  You are changing the ruler. The only acceptable direction is *more
  discriminating*, never *more permissive*. If a change would raise the score of
  a session you cannot defend on reading its transcript, it is wrong.

## 3. Scope (numbered)

1. **An offline re-score entry point — build this first, it is the measurement
   instrument for everything else.** There is currently no way to re-score a
   recorded run: the CLI has `extract`, `run`, `lint`, `compare`,
   `set-baseline`, `runs` and nothing else (`eval_interactive/cli.py`). Add a
   command that takes a recorded run directory and re-runs the scoring layer
   over its persisted traces, without any bot call.
   **Feasibility gate — do this before designing anything:** verify that
   `results.json` actually carries what L1/L2/L3 need (transcript, per-turn
   trace, tool calls, containment/escalation fields). If it does not, **STOP,
   report exactly which field is missing, and fall back** to fixture-based
   scoring tests plus a request to the human for a coordinated live run. Do not
   fabricate a substrate.

2. **Fix Loop C so P1 holds.** Decide on evidence, and justify the choice
   against §5.4:
   - whether `judge_score = 0.0` when there are no gating L3 results is the
     defect (i.e. absence of a signal is being scored as failure of it), and
   - whether the `advisory` classification of the three dimensions on
     `promotion/` specs is correct but the *composite formula's* treatment of
     "no gating dims" is not.
   The fix must not be "lower the 0.7 bar" and must not be "make everything
   gating so the mean goes up". Both are ruler-widening. State in the handoff
   which sessions' scores move, in which direction, and why each move is
   defensible on its transcript.
   You may set `llm_judge_dimensions` on `case_specs/bad_cases/**` if the
   evidence supports it — that is your owned path — but note their authority is
   `human_review`, so this is about making the number *readable*, not about
   changing the gate.

3. **Make the silent L3 collapse loud.** A judge that 400s and falls back to
   `_DEFAULT_SCORE = 3.0` for every dimension must not produce a run that looks
   scored. Emit a per-run diagnostic that names how many dimension calls fell
   back, and make a run whose L3 layer wholly fell back **unusable as a
   verdict** rather than a mid-range constant. This is a Runtime-side
   observability duty (§1.4), not a semantic decision.

4. **Implement the D1/D2/D3 ladder (WS-4).** Extend the verdict beyond flat
   `resolved / escalated / abandoned` so that P2 holds. Constraints:
   - Derive the tier **in the eval layer, from trace fields that already
     exist** (was resolution attempted; was there a grounded citation; was the
     handover payload complete; at which turn). No `server/**` change.
   - The tier must be reproducible by the harness from a recorded trace — a
     human spot-check verdict and the harness verdict must be able to disagree
     *visibly*, which is the point of the ladder.
   - Keep the existing three values working. Adding a dimension to the verdict
     must not silently re-map old runs; if a recorded baseline's numbers change,
     that must be an explicit, reported migration.

5. **Fix the stale Skill-count assertion.**
   `tests/test_skill_procedure_extractor.py::TestProductionSkillLoad::test_load_all_six_production_skills_populated_critical_steps`
   asserts 6 production Skill YAMLs and finds 7 — stale since WS-3 added
   `resolve_technical_diagnose_or_intake.yaml` on 2026-07-25. Sprint 103 §7
   established this is **not** one of the missing-CSV failures. Fix the count
   (and the name, if it hardcodes "six"). This takes the pytest baseline from
   14 failures to 13.

## 4. Hard fences

- **§5.4 is the governing fence**: no CaseSpec widening, no rubric relaxation,
  no judge downgrade that lets a real bot mistake pass. If you find yourself
  making the ruler more permissive to make a number look better, stop.
- No change under `server/**`, `eval/**`, `data/**`, `Makefile`,
  `case_specs/promotion/**`, `case_specs/case_families/**`, `autoloop/**`,
  `e2e/**`.
- Do not restart the backend; do not launch bot sessions (Sprint 104 holds that
  token).
- Do not resume or run the autoloop. It is stopped pending exactly this fix
  plus Sprint 106; resuming it is a separate, human-authorised step.
- Do not author your own review prompt and do not dispatch Codex (§9.3.2).
- Do not merge to `perf-replan-2026-07` or `main`.

## 5. Layer-classification + anti-hardcode stanza (§7.1)

**Target failure layer:** `eval_spec` for items 2 and 4 (the CaseSpec / rubric /
judge configuration and the verdict model), `infra` for items 1, 3 and 5 (the
eval framework's own instrumentation). Note §5.8: an open `infra`
framework-defect brief preempts semantic sub-sprints — this sprint *is* that
work, which is why it runs now.

**Tier-0 invariant:** This sprint adds no Tier-0 invariant. It changes no
runtime behaviour at all; it changes what the harness can see.

**Semantic hardcode:** No semantic hardcode introduced. The ladder tier is
computed from trace facts (attempted retrieval, citation present, handover
payload completeness, turn index), not from keyword matching on bot text. If
you find yourself pattern-matching the bot's wording to decide a tier, that is
the forbidden shape — report it instead.

**Generalization coverage:** target / neighbor / negative / shadow =
`5 / 19 / 2 / 0` measured as *recorded sessions re-scored*, not as live runs.
Targets: the five WS-5 / Sprint 103 `promotion/` + `anchor/` sessions that
scored `composite = 0.000`, `cs_interactive_179` foremost. Neighbours: the 19
`bad_cases` (all currently structurally 0). Negative controls: at least two
recorded sessions whose behaviour you judge, by reading the transcript, to be
genuinely wrong — they must **not** start passing. Shadow set is not available
to you by construction (§5.1).

## 6. Baselines and test requirements

**Record both baselines before touching any file**, citing the command:

- `cd eval_interactive && uv run pytest -q` → expect
  `764 passed, 14 failed, 5 skipped`. 13 of the 14 come from the missing
  `data/human_review_annotations_2026-04-22_golden.csv` (out of your scope —
  Sprint 107 owns `data/**` and may restore it under you, which would drop the
  failure count without any action from you); the 14th is item 5, which is
  yours. Verify the 13-vs-14 split from the actual failure output rather than
  assuming it — an earlier contract in this series mis-attributed the 14th.
- `cd server && mvn -o test` → expect
  `Tests run: 1493, Failures: 1, Errors: 0, Skipped: 2`, the one failure
  inherited. You will not change `server/**`, so this must be byte-identical
  after your work; run it once at the end to prove it.
- The corpus verdict, for the record:
  `cd eval_interactive && uv run eval-interactive lint --path case_specs --summary-only --exit-zero`
  → as measured 2026-07-26: `488 spec(s)`, `Errors: 226`, `Warnings: 51`,
  `R1 uc_outcome_consistent: 118`, buckets `promotion/ 65`, `anchor_outcome/ 52`,
  `bad_cases/ 37`, `anchor/ 23`. **Sprint 106 is changing these numbers under
  you in a parallel worktree** — record yours, do not treat drift in them as
  your regression, and do not "fix" specs to move them.

**These numbers may be stale.** Prompt baselines in this repo have been wrong
before. Re-measure; if yours differ, attribute the delta before proceeding.

Required: characterisation tests that pin the *current* wrong behaviour before
you change it (a test that asserts `cs_interactive_179`-shaped input scores
`composite = 0.0` today), then flipped to the corrected expectation with the
old assertion preserved as the documented before-state. Self-introduced
failures: **0**.

## 7. Evidence requirements

For a scoring-layer change, deterministic re-scoring of recorded traces **is**
the primary evidence — §5.7's mocked-LLM restriction is about attributing a
*behaviour* change to a prompt change, which is not what this sprint does. But
the L3 judge is a live LLM call, so:

1. Re-score all five `composite = 0.000` sessions (§5 targets) with the fixed
   scoring layer and report the before/after score table, per dimension.
2. For every session whose score rose, **quote the transcript evidence that
   justifies the rise.** A score that rose without a defensible transcript is a
   §5.4 violation you introduced.
3. Re-score the two negative controls and show they did not rise.
4. Report the L3 fallback count from item 3's new diagnostic for every run you
   score. A run with a non-zero fallback count may not be quoted as evidence.
5. State what the ladder does **not** yet capture. The honest limit of a
   trace-derived tier is that it cannot see answer *quality* — the e2e findings
   recorded two materially different groundings for the same question, one of
   them worse advice, both legitimately grounded. If your ladder cannot
   distinguish those, say so.

## 8. Handoff requirements

Write `docs/sprints/sprint-105-handoff.md`. Required sections:

1. Item 1's feasibility finding, verbatim: what `results.json` does and does not
   carry.
2. The Loop C diagnosis you settled on, and why the alternative was rejected.
3. Per-session before/after score table with transcript justification for every
   rise (§7.2).
4. The ladder's definition, as a table: tier → the trace facts that produce it.
5. Test numbers: before / after / attribution, both suites.
6. What the new verdict still cannot see (§7.5).
7. Real defects found but out of contract — listed, not fixed.
8. Where you think this contract is wrong.
9. Self-check, ticked.

## 9. Commit discipline

- Small, reviewable commits on `sprint-105-eval-verdict`. Never `git add -A`
  blind — stage explicit paths; the repo has a documented dirty-index hazard.
- Commit message: what changed, the mechanism, the evidence, and explicitly
  whether the ruler got stricter or looser on each affected session.
- End every message with:
  `Co-Authored-By: Claude Opus 5 (1M context) <noreply@anthropic.com>`
- No run artifacts committed.

## 10. Self-check before claiming done

- [ ] Worktree is `../csagent-wt-105` on `sprint-105-eval-verdict`; nothing
      written outside my owned paths; `server/**` diff is empty.
- [ ] Both baselines recorded before the first edit; self-introduced failures 0;
      `mvn -o test` unchanged.
- [ ] Backend never restarted; no bot session launched.
- [ ] Every score that rose has quoted transcript justification (§5.4).
- [ ] Two negative controls did not rise.
- [ ] L3 fallback diagnostic reports 0 for every run I quote as evidence.
- [ ] The 0.7 bar was not lowered; no dimension was promoted to gating merely to
      raise a mean.
- [ ] Autoloop not resumed; no Codex dispatched; no merge; no other sprint's
      paths touched.
