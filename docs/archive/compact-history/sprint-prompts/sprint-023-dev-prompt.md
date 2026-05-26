# Sprint 23 — Dev Agent Prompt

Paste the content below this line into a fresh Claude Code session on branch `design-v1-without-human-review`. Working tree at session-start carries deliver-agent-owned files — see §11.

---

You are the dev agent for Sprint 23: a two-track investigation+bundle sprint targeting (A) repeated FAQ / `search_knowledge` tool calls and (B) LLM instability / deadline / placeholder / stall behaviour. UX-over-pass-rate framing: evidence quality + matrix completeness + correct bundle-or-defer choice = the bar; pass-rate is observed, not the bar.

## 1. Loader (read in order)

1. `AGENTS.md`.
2. `docs/current/doc_governance.md`, `docs/current/agent_context_guide.md` (Runtime/phase machine/drift list).
3. `docs/current/iteration_governance.md` §1, §3 (first-match-wins), §5, §7.
4. `docs/sprint_objective.md` — ALL 16 sections. Treat this as the contract. Sections below refer to it.
5. `docs/sprints/sprint-019-handoff.md` §3 (per-case §3.2 walks) + §4 (orchestrator de-dup investigation).
6. `docs/sprints/sprint-020-handoff.md` §5 (`already_called: [{tool, arguments_hash, at_step}]` slot — wired but unconsumed by prompt).
7. `eval_interactive/results/20260510-134558/results.json` — evidence source.
8. `docs/diagnostics/failure-briefs/manual-probe-2026-05-13-ad-visibility-multi-layer-failure.md`.

Do not load other docs until you have a specific evidence question.

## 2. First two tasks (re-derive target sets — DO BEFORE anything else)

Before walking any hypothesis or touching any code:

**Track A target set.** Find cases in `results.json` with ≥2 successful `search_knowledge` dispatches with same / near-same `arguments_hash` within one session, plus the manual-probe case. Document your search method (grep / parse), the case-by-case enumeration, and the count. Do NOT inherit any list from prior summaries.

**Track B target set.** `grep "Sorry, I'm a bit slow right now" eval_interactive/results/20260510-134558/results.json`; map matches to case_id boundaries. Classify each: clean loop (≥2 consecutive); emits+recovers (single + recovery); zero placeholder; interleaved. Sprint 19 §3.7 named {cs_002, cs_011, cs_014, cs_038, cs_040} as sharing this surface — planning-turn premise verification found this list is internally inconsistent with Sprint 19 §3.2 (cs_011 has no placeholder) and missed cs_066. **Re-derive independently** and report any deviation in handoff §3 with supporting evidence.

A case can be in both target sets, only one, or neither.

## 3. Track A — walk five hypotheses (objective §6.1)

Per case in your Track A target set, walk: (a) LLM re-emission; (b) orchestrator/dispatcher amplification; (c) missing `already_called` prompt consumption (verify with grep on `system_prompt.txt`); (d) projection failure (prior FAQ results not visible/salient); (e) phase-plan directive non-followship.

Cite concrete evidence per hypothesis per case: ToolEvent stream excerpts, serialized prompt / projection contents if recoverable, code-path walk. Build the root-cause matrix at handoff §3 or §4: `case_id × turn × LLM raw tool calls × dispatched tool calls × projection contents × accumulated tool results × root-cause layer`.

**Bundle-or-defer (objective §6.3):** one conclusive root cause → bundle safest narrow fix (typically `prompt_projection` soft signal — land `R-already-called-prompt-consumption` if (c) is conclusive). Multiple conclusive → bundle safest only, defer rest. Inconclusive → investigation-only output (acceptable).

## 4. Track B — walk six hypotheses (objective §7.1)

Per case in your Track B target set, walk: (a) model latency [follow-on R-item only]; (b) deadline budget [follow-on R-item only — DO NOT change config]; (c) retry/backoff; (d) `PhaseEvaluator` fallback handling at lines 754–770 (deliver-agent-verified); (e) session-scope placeholder repetition (Sprint 19 §3.1 diagnosis); (f) turn-budget / phase-transition mapping.

Distinguish in the matrix: clean loops / emits+recovers / `semantic_planner` failures / interleaved.

**cs_040 fence (objective §5):** cs_040 likely appears as a clean placeholder loop AND has a UC-K→UC-C routing failure (Sprint 19 §3.5, `prompt_projection`). These are SEPARATE issues at different layers. Track B's narrow fix addresses placeholder UX only. Name the routing failure explicitly in the handoff; propose a disposition (new R-item, or broaden existing `R-prompt-phase-plan-directive-followship` if n≥2 threshold is now met — verify action_bank state). Regression-test rationale must NOT claim to fix routing.

**Bundle-or-defer (objective §7.4):** bundle session-scope coalesce + honest next-step ONLY if root cause is purely user-facing repeated fallback messaging. If evidence points to model latency / timeout config: defer with R-item carrying latency data. If inconclusive: investigation-only. **Deadline budget MUST NOT be widened under any circumstance in this sprint.**

Honest message rules: name slowness ("This is taking longer than expected") + offer actionable next step (handover offer, retry). Do NOT mask slowness with reassuring filler. Do NOT promise a specific recovery timeline.

## 5. Hard fences (verbatim — blocking if breached)

- Do not treat placeholder coalescing as the root cause unless evidence shows root cause is purely user-facing repeated fallback messaging.
- Do not assume the 5-case Cluster C list is accurate; re-derive from results.
- Do not conflate cs_040's placeholder loop with its UC-K → UC-C routing.
- Do not ship eval-only bookkeeping.
- Prioritize user-visible reliability over pass-rate.
- Do not widen the deadline budget — period.

## 6. Files in scope vs NOT in scope

**In scope (read-only):** `eval_interactive/results/20260510-134558/results.json`; the manual-probe brief; Sprint 19 §3 + §4; Sprint 20 §5; `docs/action_bank.md` (propose deltas in handoff §11).

**In scope (edit only if evidence supports bundled fix):** `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java`, `AgentRunLoopImpl.java`, `service/tools/ToolDispatcher.java`, `service/runtime/ContextProjectionBuilder.java`; `server/src/main/resources/system_prompt.txt`; regression tests under `server/src/test/java/com/gumtree/csagent/**`.

**Write:** `docs/sprints/sprint-023-handoff.md` (NEW).
**Refresh:** `docs/10-handoff.md` (Sprint 23 lead).

**NOT in scope (hard fence):** all eval-spec surfaces (`eval_interactive/case_specs/**`, `case_spec_overrides.yaml`, `personas*.yaml`, judge rubric); Sprint 20 case families (`case_families/**`, `case_specs_shadow/**`); foundational docs; governance docs; all sprint archives `sprint-001..022-*.md` (read-only); `docs/runtime_freeze_and_risk_policy.md`; FAQ corpus; deadline/timeout config; model/provider config.

## 7. §1.7 hard gate

- No keyword/regex/per-UC matrix for a semantic decision.
- No eval-rubric widening.
- No prompt if-else. If Track A lands `R-already-called-prompt-consumption`, the `system_prompt.txt` teaching text must be principled: teach the LLM what the observable slot means and what its content implies; do NOT branch on tool name or UC.
- Track B's honest message must NOT mask slowness with reassuring filler.
- No new Tier-0 invariant — if §3.2 Q2 stop-gate fires, route to `human_review_required` per §3.2 tail rule.

## 8. Handoff contract (12 sections in `docs/sprints/sprint-023-handoff.md`)

1. Context Pack. 2. Sprint-objective recap. 3. Track A — per-case §3.2 walk + matrix. 4. Track B — per-case §3.2 walk + matrix. 5. Files changed (or "no files changed"). 6. Layer-classification self-walk. 7. Anti-hardcode self-walk (§4.1 nine questions). 8. Generalization-coverage table (target / neighbor / negative / shadow; negative + shadow may be explicit gap deferred to G2). 9. Sprint-objective-met check (16 sections per-bullet PASS/PARTIAL/GAP). 10. Open questions for human. 11. Action-bank deltas (proposed). 12. Next recommended action.

§12 may end with a verdict-section placeholder for human + deliver agent to fill on close.

## 9. Stop conditions (report to human, do not proceed)

- Tempted to widen the deadline budget.
- About to inherit a case-list without independent verification.
- About to edit a §6 NOT-in-scope file.
- About to conflate cs_040's placeholder shape with its routing shape.
- Evidence supports a fix requiring keyword/regex/if-else/per-UC matrix or a new Tier-0.
- §3.2 Q2 stop-gate fires.
- Sprint 19 §3.7 disagrees with what you observe in the results JSON — surface in handoff §3, do not silently follow.

## 10. Eval + suite

If you ship a bundled fix: run full server suite (Sprint 20 baseline 894/0/0/1, no regressions); run eval harness on smoke set; safety / grounding / wrong-containment / over-escalation must not regress. Pass-rate is reported but not the bar. Shadow not collected (skip).

The regression test (if you ship) must demonstrate: for Track A, the fix reverses the repeated-FAQ shape on at least one target case; for Track B, ONE placeholder + ONE honest next-step across 2 consecutive deadline turns (NOT 2 identical placeholders).

## 11. Working tree at session start (commit-at-end heads-up)

Expect deliver-agent-owned files to be uncommitted at session start: `docs/sprint_objective.md` (Sprint 23 objective, NEW; prior content archived to `docs/sprints/sprint-022-objective.md` in commit `68413de`); `compact/sprint-023-dev-prompt.md`; `compact/sprint-023-review-prompt.md`; `compact/sprint-deliver-orchestrator.md` (possibly updated); possibly `docs/diagnostics/codex-findings.md` (pre-existing deletion, Sprint 17 era — not your scope).

**Do not stage these yourself.** Do not `git add -A` or `git add .` for your commit. Stage only files you authored under §6. If the human bundles deliver-agent files at commit time, that is expected — do not pre-empt.

Commit message shape:
```
sprint 23: <one-line UX outcome>

<short paragraph: tracks delivered or deferred; matrix completeness>

<bundle-or-defer outcome per track>
```

## 12. Output discipline

- Root-cause matrix = primary deliverable. Bundle safest narrow fix only where evidence is conclusive.
- Every cited line / file must exist. Verify before citing.
- Every R-item proposal: name, layer per §3.1, source evidence excerpt, rationale, conditional-broadening note if n=1.
- Honest message text (if Track B bundles) reviewed against §7.

Stop. Read `docs/sprint_objective.md`. Begin with §2 (re-derive both target sets) in parallel.
