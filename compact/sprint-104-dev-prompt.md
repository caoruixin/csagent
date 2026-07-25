# Sprint 104 / WS-6-B — dev prompt: DISCOVER's clarification budget is the new bottleneck, and drift eats it

> Paste this whole file into a fresh dev session. It is self-contained per
> `prompt-artifact-rules.md` §9.1: you need nothing else except the
> `AGENTS.md` governance chain, which loads automatically.
>
> **This file IS the contract** (§9.3.1). There is no separate
> `sprint_objective.md` to cross-check. If scope changes mid-sprint, it
> changes here, in place.
>
> **This sprint runs in a git worktree, in parallel with Sprints 105 / 106 /
> 107.** §0.2 defines what you own and what you must not touch. Read it before
> writing anything.

## 0. Start here

### 0.1 Create your worktree

You are starting in the primary checkout `/Users/caoruixin/projects/csagent`,
whose tip is `d7d84f86` on branch `perf-replan-2026-07`. Do not work there.

If your session offers an `EnterWorktree` tool, use it. Otherwise:

```bash
cd /Users/caoruixin/projects/csagent
git worktree add ../csagent-wt-104 -b sprint-104-discover-budget perf-replan-2026-07
cp .env.local ../csagent-wt-104/.env.local     # gitignored; git worktree does NOT carry it
cd ../csagent-wt-104
git log --oneline -1                           # must read d7d84f86
```

All paths in this contract are relative to your worktree root.

### 0.2 Path ownership — three other sessions are live right now

| Sprint | Owns (writable) |
|---|---|
| **104 (you)** | `server/**`, `docs/sprints/sprint-104-handoff.md`, this prompt file |
| 105 | `eval_interactive/eval_interactive/**`, `eval_interactive/tests/**`, `eval_interactive/case_specs/bad_cases/**` |
| 106 | `eval_interactive/case_specs/promotion/**`, `eval_interactive/case_specs/case_families/**`, the approved-override YAML |
| 107 | `eval/**`, `data/**`, `Makefile` |

**You may read anything. You may write only under `server/**` plus your own
handoff.** If your fix appears to require a change under another sprint's
paths, **STOP and report it** — do not reach across. Never edit
`docs/sprints/*` (other than your own handoff), `docs/archive/*`, or
`docs/proposals/*`.

### 0.3 Exclusive resource token — you hold it

You are the **only** session authorised to start/restart the backend on
`:8080` and to run the real-LLM eval simulator this wave. Postgres `:5442` and
Redis are shared and append-only — do not run `make ingest`, do not drop or
migrate anything, do not `TRUNCATE`.

Long real-LLM runs must not be interrupted by the Mac sleeping: launch the
backend under `caffeinate -dimsu`. A run that spans a sleep is uncertifiable —
kill it and re-run rather than reporting resumed draws.

### 0.4 Read before coding, in this order

1. `docs/current/iteration_governance.md` — §1.3 / §1.4 (LLM-vs-Runtime
   ownership), §1.5 + §1.7 (anti-hardcode), §3 (fix-layer classification),
   §5.7 (mocked-LLM evidence is not behaviour evidence).
2. `docs/sprints/sprint-103-handoff.md` §5.2, §5.4, §6.1, §6.2 — the measured
   evidence this sprint acts on. Do **not** re-derive it.
3. `docs/proposals/performance_priority_replan_2026-07.md` §1.5, §3 WS-6, §5.
4. `docs/diagnostics/failure-briefs/ws5-2026-07-25-intent-switch-forces-escalation.md`
   and `docs/diagnostics/failure-briefs/e2e-2026-07-25-idempotent-repeat-burns-turn-budget.md`.

## 1. Why this sprint exists (evidence already collected)

Sprint 103 gave the LLM a `propose_reroute` tool so it can change the use case
outside DISCOVER. The capability works: of the sessions that reached
RESOLVE/CONFIRM with a drift raised, 2 of 2 re-routed, one over the
`CONFIRM→RESOLVE[UC-A]` edge, with model-authored reasoning. Negative controls
re-routed 0 times.

**And the outcome did not move.** 0 of 6 sessions resolved (baseline 0 of 5),
and bot-initiated escalation did not fall. Sprint 103 §6.2 names the reason and
calls it "the next real work item on this failure shape":

- **5 of the 10 measured sessions escalated with
  `clarification_budget_exhausted`, and 4 of those never left DISCOVER.**
- `control-policy.yaml:2` sets `max-clarification-rounds: 2`. When the customer
  raises a second, different need while DISCOVER is still clarifying the first,
  that new need **consumes a clarification round**. Drift therefore
  *accelerates* budget exhaustion.
- This is why `cs_interactive_185` — one of Sprint 103's two named targets —
  could not exercise the new capability at all: `propose_reroute` is declared
  on the RESOLVE and CONFIRM Skills, so on a session that never leaves
  DISCOVER it was never in `plan.allowedTools`, never projected, never
  callable.

Phase traces, verbatim from Sprint 103 §5.2:

| draw | case | phase trace | stop reason |
|---|---|---|---|
| 1 | 185 | `DISCOVER×2; DISCOVER→ESCALATE[UC-A]` | `clarification_budget_exhausted` |
| 2 | 185 | `DISCOVER; DISCOVER→RESOLVE[UC-D]; RESOLVE→ESCALATE[UC-D]` | `clarification_budget_exhausted` |
| 3 | 185 | `DISCOVER×2; DISCOVER→ESCALATE[UC-D]` | `clarification_budget_exhausted` |
| 2 | 179 | `RESOLVE→RESOLVE[UC-C]; RESOLVE→ESCALATE[UC-C]` | `turn_budget_exhausted` **on turn 2 of a `max_turns: 15` session** |

The second defect in that table is Sprint 103 §6.1: a per-turn tool-step
exhaustion being reported as a session-level turn budget. Same shape as the
known `resolveMaxStepsReason`-on-loop-exhaustion mis-stamp. It corrupts every
downstream reading of *why* the bot escalated, including this sprint's own.

## 2. Goal

A customer who raises a second, different need mid-conversation should not be
escalated **because the clarification budget ran out on the drift itself**, and
an escalation should be stamped with the reason that actually caused it.

Note the shape of the fix this does **not** authorise: raising
`max-clarification-rounds`. Sprint 103 deliberately did not touch a budget, and
raising one to make a symptom disappear is the same mistake as lowering one to
force a number. The budget is Runtime's (§1.4); what the LLM is *told* about
its remaining latitude, and how a drifted turn is *accounted*, is where the
defect lives.

## 3. Scope (numbered — deliver all four, in order)

1. **Characterise before changing anything.** Read the persisted turn records
   for the six Sprint 103 target sessions (`bot_turns`, `bot_turn_llm_calls`,
   `bot_events` in Postgres `:5442`) and establish, with quoted evidence:
   (a) exactly where a clarification round is counted and where the budget is
   enforced — start from `server/src/main/resources/config/control-policy.yaml`,
   `ControlKernel.java`, `PhaseEvaluator.java`; (b) whether a drift-raising
   customer turn is charged a clarification round, and if so at which call
   site; (c) what the LLM is told about its remaining clarification latitude in
   the per-turn projection (`ContextProjectionBuilder.java`, the DISCOVER skill
   `server/src/main/resources/skills/discover_triage.yaml`, and the system
   prompt). Write this down in the handoff **before** the fix. If (b) turns out
   to be false — the round is not charged and the exhaustion has another cause
   — say so plainly and re-scope items 2–3 around the cause you found. A
   characterisation that falsifies the contract's premise is a successful
   sprint, not a failed one.

2. **Fix the accounting or the projection, per §3 layer classification.**
   Whichever of these the evidence from item 1 supports:
   - `prompt_projection` — DISCOVER's projection does not tell the LLM the
     truth about its remaining rounds, or about the fact that a newly-raised
     second need is a *different* need rather than a failed clarification of
     the first. Precedent for this exact remedy: `6906577d` (P1-04) removed a
     clause that made a false claim about runtime behaviour and replaced it
     with the facts the runtime actually holds. Stating a true fact to the LLM
     is Runtime's duty under §1.4; it is not a semantic hardcode.
   - `skill_state` — the drifted need is not carried across the phase boundary,
     so DISCOVER re-clarifies from scratch.
   Do **not** add keywords, regexes or per-UC rules to decide whether a turn is
   a drift; `DriftDetector.java` and `RuntimeIntentClassifier.java` must not
   gain a single new pattern. If you believe the only available fix is a new
   deterministic guard, that requires a current Tier-0 invariant in
   `docs/runtime_freeze_and_risk_policy.md` §1/§2 — if none covers it,
   classify `human_review_required` and STOP.

3. **Fix the `turn_budget_exhausted` mis-stamp.** A per-turn tool-step
   exhaustion must not be reported as a session-level turn budget. Anchors:
   `PhaseEvaluator.java` (`resolveMaxStepsReason`, and the reason list near
   `:80`), `EscalationReasonResolver.java` (the priority map near `:113`,
   `turn_budget_exhausted` at priority 42). The canonical reason set is a
   Runtime contract — if the correct reason for this condition does not exist
   in it, do not invent a 25th value silently: propose it in the handoff with
   its priority and justification, and implement only if it is a
   re-attribution among existing values.

4. **Make `propose_reroute` reachable on the sessions that need it, or prove it
   cannot be.** Sprint 103 declared the tool on RESOLVE and CONFIRM only. If
   items 1–3 leave a session stuck in DISCOVER, decide on evidence whether
   DISCOVER's existing `classify_use_case` already covers the case (Sprint 103
   §1.2 found its commit semantics are DISCOVER-bound) or whether the real gap
   is that DISCOVER escalates instead of advancing. Report the finding; only
   change tool declarations if item 1's evidence supports it, and if you do,
   update the golden tool-set and declaration-matrix tests by **adding**
   members to the exact-set assertions, never by relaxing them.

## 4. Hard fences

- No new keyword / regex / enum-expansion for a soft semantic decision
  (§1.5, §1.7). `git grep` your diff for `DriftDetector`,
  `RuntimeIntentClassifier`, `risk-keywords.yaml` and report the result.
- No budget raised or lowered to move a metric. If your evidence says a budget
  value is genuinely wrong, that is a product decision — surface it, do not
  take it.
- No change under `eval_interactive/**`, `eval/**`, `data/**`,
  `case_specs/**`, `autoloop/**`, `e2e/**`. If a CaseSpec looks wrong, record
  it for Sprint 106 (Sprint 103 §6.6 did exactly this).
- Do not author your own review prompt and do not dispatch Codex (§9.3.2:
  verification artefacts are authored after delivery, by the human/deliver
  agent, against what you actually shipped).
- Do not merge to `perf-replan-2026-07` or `main`. Leave your branch for the
  integration pass.

## 5. Layer-classification + anti-hardcode stanza (§7.1)

**Target failure layer:** `prompt_projection` primarily; `skill_state`
secondarily for item 2; `infra` for item 3's mis-stamp. Item 1 may re-route
this — if the evidence lands on `semantic_planner` or
`human_review_required`, say so and stop rather than forcing a fix into the
layer this stanza predicted.

**Tier-0 invariant:** This sprint adds no Tier-0 invariant. If item 2's
evidence points at a deterministic guard, no current Tier-0 in
`docs/runtime_freeze_and_risk_policy.md` §1/§2 covers "drift must not consume a
clarification round" — classify `human_review_required` instead of inventing
one.

**Semantic hardcode:** No semantic hardcode introduced. Replacing a false
statement in the projection with the facts the runtime holds is a §1.4
correction, not a new rule; the judgement of whether two asks are the same
stays with the LLM per §1.3.

**Generalization coverage:** target / neighbor / negative / shadow =
2 / 2 / 2 / 0. Targets `promotion/cs_interactive_185` (never left DISCOVER in
3 of 3 draws) and `promotion/cs_interactive_179` (the `turn_budget_exhausted`
mis-stamp draw). Neighbours `promotion/cs_interactive_263` (UC-F,
`clarification_budget_exhausted`, never left DISCOVER) and one further
`hard_shift` resolve-class `promotion/` spec of your choice, named in the
handoff. Negative controls `anchor/cs_interactive_030` and
`anchor/cs_interactive_155`, both `drift_behavior: none` — they must not gain
extra clarification rounds or change their stop reason. Shadow set is not
available to you by construction (§5.1).

## 6. Baselines and test requirements

**Record both baselines before touching any file**, and cite the command:

- `cd server && mvn -o test` → expect
  `Tests run: 1493, Failures: 1, Errors: 0, Skipped: 2`. The single failure is
  inherited: `SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly`
  (requires the literal `"Sprint 6"` in `system_prompt.txt`).
- `cd eval_interactive && uv run pytest -q` → expect
  `764 passed, 14 failed, 5 skipped`. 13 of the 14 come from the missing
  `data/human_review_annotations_2026-04-22_golden.csv`; the 14th is a stale
  Skill-count assertion
  (`tests/test_skill_procedure_extractor.py::TestProductionSkillLoad::test_load_all_six_production_skills_populated_critical_steps`,
  expects 6 production Skill YAMLs, finds 7 since WS-3 added
  `resolve_technical_diagnose_or_intake.yaml`). **Both are out of your scope**
  — Sprint 105 owns the stale assertion. Do not fix either.

**These numbers may be stale by the time you run them.** Prompt baselines in
this repo have been wrong before. Re-measure, and if your numbers differ,
attribute the delta before proceeding — do not revert in-scope work to make a
number match.

Required: new tests for every changed decision path; self-introduced failures
must be **0**. If you extend a golden set assertion, state explicitly that you
added members rather than relaxing the assertion.

## 7. Evidence requirements

Mocked-LLM tests are **wiring evidence only** (§5.7) — the mock decides what
the "LLM" chose, so they cannot show a behaviour change. Label them as such.

Behaviour evidence, required:

1. Restart the backend after your final server change and prove the run went
   against changed code (the startup log's tool-registry line, or
   `javap -v -p target/classes/...` for a constant you added). `spring-boot:run`
   has no hot reload — a missing field in the payload means a stale backend,
   not a code bug.
2. **3 draws × 2 targets + 2 negative controls + 2 neighbours**, one attempt
   per case per invocation (`--parallel 1`; the CLI runs 1 attempt/case, so N
   draws = N invocations). Expect roughly 60 s per session, not the 4–9 min an
   earlier contract claimed.
3. Adjudicate the outcome **by reading transcripts and per-turn phase traces**,
   not by composite score. The programmatic composite is structurally 0 on
   these specs (replan §1.3) and Sprint 105 is fixing that in parallel — you
   will not have a trustworthy composite this sprint. Report: phase trace per
   draw, stop reason, escalation reason, whether the drifted ask was engaged,
   whether the session resolved.
4. Record the commit SHA your worktree's `eval_interactive/case_specs/` was at
   when you ran (`git rev-parse HEAD`). Sprint 106 is rewriting `promotion/`
   expectations in a parallel worktree — including, possibly,
   `cs_interactive_185`'s `primary_uc` — so your evidence is valid against the
   specs you measured and must be re-attributed at integration. Your worktree's
   copy is frozen, so this does not disturb your run; it only has to be stated.
5. Report what did **not** move as plainly as what did. Sprint 103 §5.4 is the
   standard: "engagement improved, outcome did not, and only one of four is
   attributable to this sprint" is the kind of statement expected. Do not
   collapse two different measures into one primary metric to make it look
   better; if the contract's framing collapses them, say so (§8).

## 8. Handoff requirements

Write `docs/sprints/sprint-104-handoff.md`. Required sections:

1. What item 1's characterisation found, with quoted code and quoted trace
   evidence, **including the case where it falsified this contract's premise**.
2. What changed, per file, and why that layer.
3. Test numbers: before / after / attribution, both suites.
4. Behaviour evidence per §7, including the run ids and the phase trace table.
5. Real defects found but out of contract — listed, not fixed.
6. Where you think this contract is wrong. Sprint 103 §7 found three contract
   errors, including one mis-attributed test failure; this section is expected
   to be non-empty and is not a criticism of the contract's author.
7. Self-check, ticked.

Note: eval run-ids are stamped with a UTC/local mix (~8 h offset, Sprint 103
§6.5). Cite run directories exactly as produced and do not use ids to order
runs.

## 9. Commit discipline

- Small, reviewable commits on `sprint-104-discover-budget`. Never
  `git add -A` blind — the repo has a documented dirty-index hazard where a
  sweeping commit poisons parallel work. Stage explicit paths.
- Commit message: what changed, the mechanism, and the evidence. If you
  withdraw a fix after measurement, say so in the message (`6906577d` is the
  precedent — a withdrawn fix recorded in full is more useful than a silent
  one).
- End every message with:
  `Co-Authored-By: Claude Opus 5 (1M context) <noreply@anthropic.com>`
- No run artifacts committed (`results/` is gitignored at the repo root).

## 10. Self-check before claiming done

- [ ] Worktree is `../csagent-wt-104` on `sprint-104-discover-budget`; nothing
      written outside `server/**` + my handoff.
- [ ] Both baselines recorded before the first edit; self-introduced failures 0.
- [ ] Item 1's characterisation written down before the fix, with quoted
      evidence.
- [ ] `git grep` over my diff: no new pattern in `DriftDetector`,
      `RuntimeIntentClassifier`, `risk-keywords.yaml`; no budget value changed.
- [ ] Backend restarted after the final server change, and served-code proven.
- [ ] 3×2 targets + 2 negative controls + 2 neighbours run; outcome adjudicated
      by transcript, not composite.
- [ ] What did not move is stated as plainly as what did.
- [ ] No Codex dispatched; no merge; no other sprint's paths touched.
