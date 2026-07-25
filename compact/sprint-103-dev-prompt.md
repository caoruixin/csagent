# Sprint 103 / WS-6-A — dev prompt: give the LLM the means to re-route on intent switch

> Paste this whole file into a fresh dev session. It is self-contained per
> `prompt-artifact-rules.md` §9.1: you need nothing else except the
> `AGENTS.md` governance chain, which loads automatically.

## 0. Your role and how to start

You are the **dev agent** for Sprint 103 / WS-6-A. Implement the contract
below, verify it with real evidence, author a handoff, and stop. You do not
decide scope; if you believe the contract is wrong, **STOP and surface it**
rather than silently re-scoping.

Before writing code, read these four files in this order:

1. `docs/current/iteration_governance.md` — the always-loaded constitution.
   §1.3 / §1.4 (LLM-vs-Runtime ownership), §1.5 + §1.7 (anti-hardcode),
   §3 (fix-layer classification), §5.7 (mocked evidence is not behaviour
   evidence).
2. `docs/diagnostics/failure-briefs/ws5-2026-07-25-intent-switch-forces-escalation.md`
   — the brief this sprint answers.
3. `docs/proposals/performance_priority_replan_2026-07.md` §1.5 and §3 WS-6.
4. `docs/diagnostics/failure-briefs/sprint32-uc-a-uc-c-alternate-uc-readiness.md`
   — the earlier brief on the same shape, which names the forbidden fix.

Working branch: **`perf-replan-2026-07`**. Do not create a new branch and do
not merge to `main`.

## 1. Why this sprint exists (evidence, already collected — do not re-derive)

On 2026-07-25, WS-5 made intent switching observable for the first time.
`persona.drift_behavior` is non-`none` in **344 of 486** CaseSpecs but had
never been explained to the simulator, so the corpus had never actually
exercised a mid-conversation intent switch. The first runs that exercise it
fail it.

Five measured sessions across two `promotion/` specs and three draws (a sixth
excluded as `status=ERROR` / UNMEASURED after a backend 500):

| draw | case | stop | escalation reason |
|---|---|---|---|
| 1 | cs_interactive_179 | bot_ended | `agent_unable_to_resolve` |
| 1 | cs_interactive_185 | bot_ended | `agent_unable_to_resolve` |
| 2 | cs_interactive_179 | bot_ended | `agent_unable_to_resolve` |
| 2 | cs_interactive_185 | bot_ended | `clarification_budget_exhausted` |
| 3 | cs_interactive_185 | goal_impossible | customer asked for a human |
| 3 | cs_interactive_179 | — | UNMEASURED (backend 500 mid-session) |

`user_state` signals: **8 `new_request`**, 2 `working`, 1
`unresolved_after_help`. Drift fires reliably. **Zero of the five measured
sessions resolved.**

Representative transcript (`cs_interactive_185`, draw 3) — the customer opens
on "I can't find More Tools to clear cookies", then shifts:

> "I just realised — my two live ads are still up, but I haven't been able to
> log in to edit or renew them. Can you help me recover access to my account
> first? And also, one of the ads is for a sofa — I think it might've been
> reported by someone, and I haven't seen any notification about it."

Both halves of that second ask are self-serve classes under the current
product principle (explanation and read-only status query). The bot instead
exhausted its clarification budget, or gave up with
`agent_unable_to_resolve`.

**The LLM is not choosing badly. It has no correct action available.** All
four facts below are verified in code:

- `classify_use_case` appears in `tools_required` of **only**
  `server/src/main/resources/skills/discover_triage.yaml:9`. Outside DISCOVER
  the LLM cannot change the active use case;
  `ToolDispatcher.validateAgainstPlan` rejects it as `tool_not_in_plan`.
- `ContextProjectionBuilder` filters the projected `tool_schemas` to
  `plan.allowedTools`, so the LLM cannot even see the tool exists.
- The `alternate_candidate_use_cases` projection slot (Sprint 31 "Option β",
  `ContextProjectionBuilder.java:592-625`) is **already implemented** but is
  gated by `skillDeclaresSoftSignal(...)` and is declared **only** in
  `discover_triage.yaml`. Half the scaffolding you need already exists.
- `CONFIRM → DISCOVER` is a declared-but-dead edge: it is in
  `control-policy.yaml` and required by
  `phase3_detailed_technical_design.md:534`, but no code path writes it.
  `RerouteDecider.java:115`'s `SOFT_SHIFT_TO_DISCOVER` rewrites its own
  target to RESOLVE.
- The only mechanism that can change the use case today is runtime pattern
  matching: `DriftDetector.java:63-90` (24 keywords, `contains()`) plus
  `RuntimeIntentClassifier`'s five Sprint-10 regexes. Everything else returns
  `unknown()` → `CONTINUE_CURRENT` → state untouched.

Note that `discover_triage.yaml`'s own procedure text already tells the LLM
it "owns the judgement of whether to ask a clarifying question, **propose a
reroute**, or stay on the active UC" — language for a capability that does
not exist outside DISCOVER.

## 2. Layer-classification + anti-hardcode stanza (governance §7.1)

**Target failure layer:** `prompt_projection` primarily, `skill_state`
secondarily. Explicitly **not** `semantic_planner`: per §3.2 Q3 the LLM chose
validly within the options it was given, and the projection / capability
handed to it was impoverished.

**Tier-0 invariant:** This sprint adds no Tier-0 invariant. It does modify
the phase-transition surface, which is a normative freeze — see the §0.6
requirement in WP3.

**Semantic hardcode:** No semantic hardcode introduced. This sprint's net
effect on the deterministic surface is neutral-to-negative: it adds a
capability and a projection slot and removes nothing, and it explicitly does
**not** add any keyword, regex, or per-UC branch. Any diff that grows
`DriftDetector`'s keyword list or `RuntimeIntentClassifier`'s pattern set is
out of contract.

**Generalization coverage:** target / neighbor / negative / shadow counts:
2 / 2 / 2 / 0. Targets = `promotion/cs_interactive_179`,
`promotion/cs_interactive_185` (both `hard_shift`). Neighbors = two further
`hard_shift` specs of your choosing from `promotion/` (that is where
resolve-class drift lives; `anchor/` has zero `hard_shift`, `exploration/` is
almost all `hard_shift` but entirely intake UCs stamped `escalate`, and
`smoke/` + `bad_cases/` exclude drift by construction). Negative controls =
two `drift_behavior: none` specs, which must **not** start re-routing.
Shadow = none in this sprint; a shadow set is not yet built for drift.

## 3. Scope

### In scope

**WP1 — give the LLM a re-routing capability outside DISCOVER.**

Decide between two designs and justify the choice in the handoff with code
evidence:

- **(a)** add `classify_use_case` to the `tools_required` of
  `resolve_faq_grounded_answer.yaml`, `resolve_technical_diagnose_or_intake.yaml`
  and `confirm.yaml`; or
- **(b)** introduce a distinct `propose_reroute` tool.

Points that bear on the choice, which you must check rather than assume:
`classify_use_case` on DISCOVER has **commit** semantics — it writes
`session.activeUseCase` and `AgentRunLoopImpl` returns
`USE_CASE_IDENTIFIED`, which `ControlKernel` turns into a same-turn replan.
Re-using it mid-RESOLVE therefore inherits those side effects, which may be
exactly right or may be too blunt. A separate `propose_reroute` costs a new
tool but lets the runtime decide whether to honour the proposal.

Capability wiring for either option touches three surfaces that are
**Runtime §1.4 capability configuration, not the semantic fence**, and narrow
edits to them are pre-approved for this purpose: `tool-policy.yaml` (per-UC
availability), the skill `tools_required` lists, and
`SkillLoader.VALID_TOOL_NAMES` if you add a tool. Expect the golden tool-set
tests to need updating; update them, do not weaken them.

**WP2 — surface the evidence the LLM needs to use that capability.**

Declare `alternate_candidate_use_cases` as a soft signal on the RESOLVE and
CONFIRM skills so the existing projection at
`ContextProjectionBuilder.java:592-625` starts emitting it there. Assess
whether that slot alone is sufficient: it is populated from **intake-time**
router candidates, and a mid-session shift the router never anticipated may
not appear in it (the slot's own documentation says so). If it is
insufficient, add the missing projection as **fact**, not persuasion — the
same discipline used on 2026-07-25 for `search_attempts_this_turn`, where the
fix was to stop telling the LLM something false and start telling it what
actually happened.

**WP3 — decide and implement where a re-route lands in the phase machine.**

Either restore the `CONFIRM → DISCOVER` edge that `control-policy.yaml`
already declares, or establish in-place re-routing without a phase change,
and say which and why. Whichever you choose:

- The phase machine is a **normative freeze** (`phase0_normative_freeze.md`
  §0.3 row "control kernel state machine"). Register the change as a §0.6
  deviation entry following the format of the existing
  `### Deviation 2026-07-25 — …` entry, and add the standard
  `[DEVIATION <date> — 见 §0.6]` marker to the affected §0.3 row.
- `RerouteDecider.java:105-110`'s comment asserts the legal table forbids
  `RESOLVE → DISCOVER`. Respect that, or change the table explicitly and
  register it. Do not leave code and `control-policy.yaml` disagreeing.
- `SkillRegistry.indexSkills` throws on a `(phase, useCase)` collision, so a
  skill cannot serve a UC that another skill already claims for that phase.

### Out of scope — do not touch

- `eval_interactive/case_specs/**` — if a spec's expectation is wrong,
  record it in the handoff; do not edit it. (Known already:
  `cs_interactive_212` declares `should_escalate: true` on the same utterance
  shape where `cs_interactive_208` declares `false`. The corpus contradicts
  itself and that is a separate WS-2 item.)
- `e2e/**`, `autoloop/**`, `docs/sprints/**`, `docs/archive/**`.
- The `executor.py` `_compute_summary` denominator question — TIMEOUT/ERROR
  rows still fold `0.0` into `mean_composite_score` and nothing reads the new
  `measurement_valid` flag. Real, known, deliberately deferred: changing the
  denominator silently re-bases every recorded baseline.
- `extractor.py`'s dropped `seed_messages[1:]`, which makes
  `promotion/cs_interactive_208` structurally untestable for UC-FP. Needs
  spec regeneration; separate sprint.
- The `composite < 0.7` structural floor (proposal §1.3 "Loop C"). Every one
  of the five sessions above scored `composite = 0.000`, including on
  programmatic-authority specs, because advisory L3 dimensions force
  `judge_score = 0`. **This means composite cannot be your success metric.**
  Use the metric in §6 instead. Do not try to fix Loop C here.

## 4. Forbidden fixes (each has a named prior ruling)

- **Do not add keywords or regexes to `DriftDetector` or
  `RuntimeIntentClassifier`.** This is the tempting fix and it is barred
  twice: by §1.5/§1.7, and by
  `failure-briefs/sprint32-uc-a-uc-c-alternate-uc-readiness.md`, which lists
  it explicitly as the wrong remediation. Drift is a soft semantic judgement
  owned by the LLM under §1.3.
- **Do not implement this as prompt persuasion.** `AgentRunLoopImpl.java:674-682`
  records that the soft-signal approach to a related problem was
  *empirically falsified* — the model ignored it. State facts and grant
  capability; do not ask the model nicely.
- **Do not relax any CaseSpec so that escalating on a drifted conversation
  counts as success.** That is a §5.4 eval-side override of a real defect and
  is the exact mistake WS-2 exists to undo.
- **Do not cap or hard-fail re-routing.** If the LLM re-routes more than you
  expected, that is data, not a bug to suppress. The PARAPHRASE_STORM
  precedent is explicit: anti-wrong-kill outranks a numeric target.
- **Do not lower any budget** to make a symptom disappear.
  `CROSS_TURN_SUPPRESSION_BUDGET = 1` in `AgentRunLoopImpl.java:87` carries a
  comment saying lowering it is forbidden; honour that.

## 5. Environment

Bring the environment up yourself; do not assume it is running.

- **Postgres runs on port 5442, not 5432.** Port 5432 is held by an unrelated
  project's Docker container. The local `postgresql@17` instance holds the
  `csagent` database (218 rows in `kb_articles`). If it is not up:
  `pg_ctl -D /opt/homebrew/var/postgresql@17 -o "-p 5442" -l <logfile> start`
  with `/opt/homebrew/opt/postgresql@17/bin` on `PATH`.
- Redis must be running (`brew services start redis`); without it
  `/actuator/health` reports DOWN.
- Backend: `DB_PORT=5442 caffeinate -i make backend`. **`mvn spring-boot:run`
  has no hot reload** — restart it after every server-side change or you will
  be testing stale code. Before restarting, wait for port 8080 to actually
  free (`until [ -z "$(lsof -tnP -iTCP:8080 -sTCP:LISTEN)" ]; do sleep 2; done`);
  a `pkill` followed immediately by a start races the dying JVM.
- Simulator/judge credentials are in the **repo-root** `.env.local`
  (`SIMULATOR_*`, `JUDGE_*`, `CSAGENT_BACKEND_URL`). Note that
  `autoloop/autoloop/cli.py:45` loads a *separate* `autoloop/.env.local`
  first when you invoke `python -m autoloop`, and it wins on conflicts; that
  file does not currently exist. Three-way provider separation is deliberate:
  bot `deepseek-v4-flash`, simulator `qwen-plus`, judge `deepseek-v4-pro`.
  Do not collapse them onto one provider — the judge and simulator were the
  same model until 2026-07-25 and their error correlation was never isolated.
- Python: `eval_interactive/.venv`.
- **macOS proxy trap:** a system proxy runs on `127.0.0.1:7890`, and
  `httpx`/`requests` with `trust_env=True` route localhost through it and
  ignore the bypass list, so local HTTP fails. Use
  `httpx.Client(trust_env=False)` or `curl --noproxy '*'`.
- Long runs: wrap in `caffeinate -i`. A sleep mid-run makes the draws
  uncertifiable.
- A single simulator session takes **4–9 minutes**. Budget accordingly;
  `batch.timeout_per_session_seconds` is now 1800.

## 6. Acceptance — measure behaviour, not units

**§5.7 governs: mocked-LLM tests cannot be primary evidence that this
change worked.** On 2026-07-25 a fix in this same area passed its entire unit
suite and was then shown by a real run to be both ineffective and harmful; it
was withdrawn. Do not repeat that.

### Required test baselines

Record these **before** you change anything, then compare:

- `cd server && mvn -o test` → baseline **1481 tests / 1 failure / 0 errors /
  2 skipped**. The single failure is inherited:
  `SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly`
  requires the literal string `"Sprint 6"` in `system_prompt.txt`, and that
  anchor was removed long ago.
- `cd eval_interactive && uv run pytest -q` → baseline **764 passed /
  14 failed / 5 skipped**. All 14 failures come from the missing
  `data/human_review_annotations_2026-04-22_golden.csv`.

**Self-introduced failures must be 0.** If you cannot tell whether a failure
is yours, prove it: create a clean `git worktree` at HEAD, copy only your
changed files onto it, and re-run. Attribution by experiment, not by
argument.

### Required behavioural evidence

Primary metric — **not** composite, which is structurally 0 here:

> On the two target specs, across **3 draws each**, how many sessions end
> with the bot having engaged the drifted ask, versus ending in
> bot-initiated escalation (`agent_unable_to_resolve`,
> `clarification_budget_exhausted`, `turn_budget_exhausted`)?

Baseline to beat: **0 of 5 measured sessions engaged the drifted ask; 4 of 5
ended in bot-initiated escalation.**

How to run one draw:

```
caffeinate -i eval_interactive/.venv/bin/python -m eval_interactive run \
  --path <dir containing the target specs> --label "s103-draw<N>" --parallel 1
```

The CLI runs **one attempt per case**, so loop it for N draws. Results land
in `results/<timestamp>/results.json`; read `case_results[].stop_reason`,
`.escalation_reason` and `.user_state_signals`.

Also required:

- Run the **two negative controls** (`drift_behavior: none`) and show they do
  not start re-routing.
- Quote **actual transcript text** for at least one before/after pair. A
  table of stop reasons alone is not evidence that the bot engaged the ask.
- Report any session with `status=ERROR` / `measurement_valid: false` as
  **UNMEASURED**, not as a result. A backend 500 mid-session is now correctly
  labelled; do not silently count it.
- State your n explicitly and do not over-claim. 3 draws cannot prove
  elimination; say what the numbers do and do not support.

## 7. Handoff

Write `docs/sprints/sprint-103-handoff.md` covering:

1. WP1 design choice (a or b) with the code evidence that decided it.
2. WP2: whether `alternate_candidate_use_cases` alone sufficed, and what you
   added if not.
3. WP3: where a re-route lands, and the full §0.6 deviation entry text.
4. Test numbers before/after for both suites, with failures attributed.
5. The behavioural table + quoted transcripts + your explicit n and its
   limits.
6. Anything you found that is a real defect but out of contract — list it,
   do not fix it.
7. Any point in this contract you think is wrong, with evidence.

## 8. Commit discipline

- Commit in logical units on `perf-replan-2026-07`; do not squash the whole
  sprint into one commit and do not merge to `main`.
- Message bodies should say **why**, and name the evidence. Recent commits on
  this branch are the house style — read `git log` before writing yours.
- End every commit message with:
  `Co-Authored-By: Claude Opus 5 (1M context) <noreply@anthropic.com>`
- Do not commit `.env.local` (gitignored) or anything under `results/`.

## 9. Self-check before you claim completion

- [ ] Both test suites run, baselines recorded first, self-introduced
      failures provably 0.
- [ ] Backend restarted after the final server change, and the behavioural
      run executed against that restarted backend.
- [ ] 3 draws × 2 target specs completed, plus 2 negative controls.
- [ ] At least one before/after transcript pair quoted verbatim.
- [ ] `git grep` shows no new keyword or regex in `DriftDetector` or
      `RuntimeIntentClassifier`.
- [ ] §0.6 deviation entry written, and the affected §0.3 row marked.
- [ ] Golden tool-set / skill-loading tests updated, not weakened.
- [ ] No file touched under `case_specs/`, `e2e/`, `autoloop/`,
      `docs/sprints/*` (other than your own handoff), `docs/archive/`.
- [ ] Handoff written, including the out-of-contract findings list.

If the behavioural metric does not move, **say so plainly and stop.** A
truthful negative result is the outcome this sprint is designed to be able to
produce; reporting a fix that did not work as though it did is the specific
failure mode this project has been correcting all week.
