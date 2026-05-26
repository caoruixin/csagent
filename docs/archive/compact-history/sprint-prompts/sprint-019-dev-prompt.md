Paste the content below this line into a fresh Claude Code session in a worktree of this repo on branch `sprint-019-a-b-parallel` (or a similarly-named feature branch). The dev agent has zero conversation context; everything it needs to start is in the prompt + the repo.

---

You are the dev agent for **Sprint 19 — Smoke Regression Investigation + Orchestrator Tool-Call De-Dup (parallel A + B)**. This is an investigation-class sprint with a bundle-or-defer policy: when a per-case layer walk yields a clean layer, you MAY bundle a reversible fix; otherwise produce a remediation proposal only. The sprint runs **two disjoint tracks** under one PR.

## 1. Load the constitution chain (do this first; do not skip)

Read these files in order. Do not start coding until you have read all of them.

1. `AGENTS.md` — repo constitution chain.
2. `docs/current/doc_governance.md` — tier model, source-of-truth rules, fold-back cadence.
3. `docs/current/agent_context_guide.md` — per-task reading lists + Context Pack Prompt. Use the "Runtime, phase machine, drift" and "Eval, governance" reading lists for Track A and the "Tool schema, tool policy" list for Track B.
4. `docs/current/iteration_governance.md` — read §1 (Constitution) + §3 (Fix Layer Classification Checklist) + §5 (Eval Acceptance Rules) + §7 (Sprint-objective stanza). §3 is the operative gate you walk per case; §5.1 is the acceptance bar.
5. `docs/sprint_objective.md` — Sprint 19 scope. Read end-to-end. The §"Bundle-or-defer policy" section and the §"Layer-classification + anti-hardcode stanza" section are normative.
6. `docs/sprints/sprint-018-handoff.md` — context for the 6 regressed cases and the manual-probe trace.
7. `docs/diagnostics/failure-briefs/manual-probe-2026-05-13-ad-visibility-multi-layer-failure.md` — input for Track B.

Produce a Context Pack (per `agent_context_guide.md`) **before** writing any code. The Context Pack must include source-of-truth decisions for each track, implementation status of the orchestrator's de-dup contract, and the top 3–5 risks.

## 2. Track A scope (smoke regression investigation)

**R-item:** `R-smoke-regression-investigation`.

**Inputs:**

- `eval_interactive/results/20260505-235231/results.json` (label `sprint8-r2`; 9 / 14 pass).
- `eval_interactive/results/20260510-134558/results.json` (label `smoke_rerun_20260510-214558`; 3 / 14 pass).
- Trace directories beneath each results dir (per-case JSON traces).
- 6 regressed cases: cs_002, cs_011, cs_014, cs_038, cs_040, cs_066.
- 8 non-regressed cases (the remaining 8 of the 14 smoke cases) — treat as neighbors.

**Per-case walk (mandatory, all 6 cases):** for each, produce in the handoff:

- Two trace excerpts (2026-05-05 PASS, 2026-05-10 FAIL) with the divergence quoted.
- The first-match `iteration_governance.md` §3.2 question + the §3.1 layer.
- Bundle-or-defer per sprint objective §"Bundle-or-defer policy": bundle if `infra` OR narrow `judge_calibration` (not rubric-widening) OR `prompt_projection` adding a soft signal; defer otherwise.
- Cite the code path or config suspected to have changed between 2026-05-05 and 2026-05-10. Sprints 14 / 14.1 / 15 / 16 declared no-runtime-semantic-change; if trace evidence contradicts, name the contradiction.

**cs_192 stop-and-escalate:** if any per-case walk surfaces a `CONTRACT_VIOLATION:active_use_case` that would re-open Sprint 8 §K0, **stop bundling** for that case, flag `human_review_required` per §3.2 Q2, do not invent a Tier-0 invariant, and surface via the handoff's "open questions" section. Proceed on the other cases.

## 3. Track B scope (orchestrator tool-call de-dup)

**R-item:** `R-runtime-orchestrator-tool-call-deduplication`.

**Input:** the manual-probe-2026-05-13 brief and its source session `4a2f3680-02a8-4d13-8f0b-f99d7c249b57`. 1 LLM request → 3 identical `search_knowledge` executions (same params, same results).

**Required investigation:**

- Read the manual-probe trace. Confirm parameter / result identity across the 3 executions.
- Walk the 3 hypotheses (phase-transition re-trigger; Turn 1 failed-call replay; LLM emitted 3 `tool_use` blocks) against trace + AgentRunLoop / ToolDispatcher code. Cite code paths.
- Walk §3.2: typically `infra` per Q1; if LLM itself emitted 3 `tool_use` blocks, re-resolves to `semantic_planner` (deferred).
- Document the orchestrator's de-dup / idempotency contract: what it is today, what it should be, what enforcement is missing.
- If `infra`: bundle fix + regression test. If `semantic_planner`: proposal only.

## 4. Files in scope

You may edit:

- `server/**` runtime code paths needed by a bundled `infra` fix (Track B). Confine changes to the orchestrator / ToolDispatcher / phase-transition path the §3.2 walk identifies.
- `server/**` runtime code paths needed by a bundled `prompt_projection` soft-signal addition (Track A). Confine changes to the projection assembly.
- `server/test/**` (Java) or `eval/tests/**` / `eval_interactive/tests/**` (Python) regression tests for any bundled fix.
- `docs/10-handoff.md` — refresh to lead with Sprint 19.
- `docs/sprints/sprint-019-handoff.md` — NEW. Use the 12-section structure in §6 below.

## 5. Files NOT in scope (hard fence)

Do not edit:

- `docs/sprint_objective.md` — the deliver agent owns this; if scope needs to change, stop and surface to the human.
- `docs/sprints/sprint-017-*.md`, `docs/sprints/sprint-018-*.md`, or any other archived sprint file.
- `docs/codex-findings.md` — Codex writes this at sprint close.
- `docs/current/iteration_governance.md`, `docs/current/doc_governance.md`, `docs/current/agent_context_guide.md` — folded back on the cadence, not edited mid-sprint.
- `docs/foundational/**` — phase docs and normative freezes are not edited per sprint.
- `docs/runtime_freeze_and_risk_policy.md` — no new Tier-0 invariant in this sprint.
- `eval_interactive/case_specs/**`, `eval_interactive/case_spec_overrides.yaml`, `eval_interactive/personas*.yaml` — CaseSpec / override edits go through Wave A5 / A6 L3 review.
- `data/faq/**`, `FAQ-knowledge_include_help_url.csv` — corpus is out of scope.
- Any prompt edit beyond pure projection (no new prompt paragraph, no prompt if-else, no `system_prompt.txt` copy edit).
- Any judge rubric that widens an L1 / L2 / L3 bar to accept the bot's actual output.

## 6. Sprint 19 handoff structure (12 sections; required)

Write `docs/sprints/sprint-019-handoff.md` with these 12 sections, in this order:

1. **Context Pack** — per `agent_context_guide.md`'s Context Pack Prompt: relevant docs, code paths, doc status warnings, source-of-truth decisions, implementation status, top risks. Produced before code; included verbatim.
2. **Sprint-objective recap** — quote the goal and the two-track split from `docs/sprint_objective.md`.
3. **Track A — per-case §3.2 walk (6 cases).** One subsection per case: trace excerpts, §3.2 question, §3.1 layer, bundle / defer, evidence cite, regression test ref (if bundled).
4. **Track B — orchestrator de-dup investigation.** 3-hypothesis walk, code-path cites, de-dup contract write-up, §3.2 walk, bundle / defer, regression test (if bundled).
5. **Files changed** — list with one-line rationale per file.
6. **Layer-classification self-walk** — restate the multi-layer stanza and the per-case / per-track layers concluded.
7. **Anti-hardcode self-walk** — answer §4.1's 9 questions against the diff.
8. **Generalization-coverage table** — columns: case, target / neighbor / negative / shadow, source. Target = 6 regressed + manual-probe. Neighbor = 8 non-regressed smoke. Negative = ≥2 per bundled fix, named. Shadow = deferred to G2 (state explicitly).
9. **Sprint-objective-met check** — walk every "Do not implement" bullet and every "Success metrics" bar.
10. **Open questions** — including any cs_192 `human_review_required` flags.
11. **Action-bank deltas** — proposed new R-items + out-of-scope deferrals (record text; deliver agent applies on close).
12. **Next recommended action** — typically "open Sprint 20 — G2 case-family + shadow split".

## 7. Eval requirements

- Re-run the smoke set after any bundled fix: `pytest eval_interactive/...` per the existing smoke harness (see `eval_interactive/README.md` if present, else the most recent smoke-run command in `docs/sprints/sprint-018-handoff.md`).
- Capture the new `results.json` and reference it in the handoff.
- Confirm safety / grounding floors per `iteration_governance.md` §5.1.
- Shadow cases: do NOT consume any shadow set in Sprint 19. If a shadow file exists in the repo, do not read it. Shadow split lands in G2.

## 8. Bundle hard rules (non-negotiable)

- No keyword / regex / if-else / enum / per-UC matrix for a semantic decision in any bundled fix.
- No CaseSpec, override, persona, judge, or prompt edit in any bundled fix.
- `prompt_projection` bundles add soft signals only (a projected slot / candidate list / diagnostic flag).
- `judge_calibration` bundles do not widen rubrics (Constitution §1.7).
- Every bundled fix carries a regression test.
- Findings whose only fix would violate the above are **deferred** to a proposal. Out-of-scope means "write the proposal and stop", not "fix it anyway".

## 9. Stop conditions (when to ask the human before continuing)

Stop and surface to the human if:

- The per-case §3.2 walk yields `human_review_required` (no current Tier-0 covers a Java-guard-shaped finding).
- A bundled fix would require touching a file in the §5 NOT-in-scope list.
- The smoke rerun shows a regression on the 8 neighbor cases that the bundle cannot reverse.
- Sprint scope appears to need expansion (new R-item is materially in the way).

Surface via the handoff's "open questions" section and stop pushing the PR until the human responds.

## 10. Final PR shape

current branch