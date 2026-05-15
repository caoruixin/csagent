# Sprint 23 — Review Agent Prompt (Codex)

Paste below into a fresh Codex session after the dev's commits land. No PR; review commit range `68413de..HEAD`.

---

You are the review agent for Sprint 23: two-track investigation+bundle, semantic-touching, §7 stanza required. Run §4.1 kernel + Sprint-23 layer below + write §4.2 sprint-close header.

## 1. Loader

1. `AGENTS.md`.
2. `docs/current/iteration_governance.md` §1, §3, §4.1, §4.2, §5, §7.
3. `docs/sprint_objective.md` (all 16 sections; especially §3 hard fences, §10 §1.7 guardrail, §11 stanza, §16 review rule).
4. `docs/sprints/sprint-023-handoff.md`.
5. `git diff 68413de..HEAD`.

## 2. §4.1 Anti-Hardcode kernel

Run the §4.1 kernel verbatim as loaded from `iteration_governance.md`. Sprint 23 is NOT exempt (semantic-touching). For Q8 (target/neighbor/negative/shadow): Sprint 23 defers negative + shadow to G2 — accept when called out in handoff §8. Verdict: `approve` | `approve with downgrade-to-signal follow-up` | `reject as semantic hardcode` | `needs human architecture decision`.

## 3. Sprint-23 blocking items (beyond §4.1)

- **Root-cause matrix required.** Handoff §3 or §4 must contain `case_id × turn × LLM raw tool calls × dispatched × projection × accumulated tool results × root-cause layer` across both tracks. Missing → blocking. Investigation-only is acceptable; missing matrix is not.
- **Bundle-without-evidence.** Each bundled fix must cite conclusive evidence per objective §6.3 (Track A) / §7.4 (Track B). Bundle without conclusive evidence → blocking.
- **Deadline-budget widening.** Any change to deadline / timeout config → blocking, period (objective §3 + §15). Grep diff for `application.properties`, `application.yml`, `*deadline*` / `*timeout*` config, or constant renames in `AgentRunLoopImpl` / `LlmInvocationService` adjusting a deadline.
- **cs_040 conflation.** If cs_040 appears in either target set, handoff must separate placeholder shape from UC-K→UC-C routing as distinct layers. Bundled fix claiming to fix routing via placeholder-coalesce → blocking (objective §5).
- **§1.7 violations.** Rubric widening → blocking. Honest-message text masking slowness with filler → blocking. Prompt if-else branching on tool name / UC → blocking. (Track A's `R-already-called-prompt-consumption` teaching text must be principled — teach the LLM what the observable slot means in principle-level language.) New keyword/regex/per-UC matrix → blocking. New Tier-0 → blocking; §3.2 Q2 stop-gate routes to `human_review_required`.
- **Inherited case lists.** Sprint 19 §3.7 named {cs_002, cs_011, cs_014, cs_038, cs_040}; premise check found cs_011 has no placeholder, cs_066 was missed. Dev MUST have re-derived both target sets from `eval_interactive/results/20260510-134558/results.json` (grep / parse method documented, per-case enumeration listed, any deviation from §3.7 reported with evidence). Silent inheritance → blocking.
- **§5.1 generalization-coverage table** required in handoff §8 IF a fix lands. Negative + shadow may be explicit gap deferred to G2. Missing / under-specified → blocking.

## 4. Packaging rollforward (NOT scope drift)

Commit-at-end accumulates deliver-agent-owned files: `docs/sprint_objective.md` (Sprint 23 objective); `compact/sprint-023-dev-prompt.md`; `compact/sprint-023-review-prompt.md`; `compact/sprint-deliver-orchestrator.md`; possibly `docs/diagnostics/codex-findings.md` (pre-existing deletion).

If the dev's commit bundles these alongside authored fix files: **packaging artefact, NOT scope drift.** Note in verdict for the record (file paths). Do NOT block. Substantive findings on fix surfaces (PhaseEvaluator, AgentRunLoopImpl, ToolDispatcher, ContextProjectionBuilder, system_prompt.txt, tests) are what block. The human treats path-based packaging findings as A-with-packaging-note rollforward.

## 5. Per-track checklist

**Track A:** target set re-derived (method documented); five hypotheses walked per case with evidence; matrix complete; if bundled — layer in {`prompt_projection`, `semantic_planner`, `infra`, `human_review_required`}, safest narrow form, regression test reverses repeated-FAQ shape on ≥1 target, §1.7 clean; deferred R-items carry evidence.

**Track B:** target set re-derived (placeholder grep, method documented); six hypotheses walked per case; matrix distinguishes clean loops / emits+recovers / `semantic_planner` / interleaved; cs_040 placeholder + routing separated; if bundled — root cause is purely user-facing repeated fallback messaging, honest message is honest (no filler, no specific recovery promise), regression test demonstrates ONE placeholder + ONE honest next-step across 2 consecutive deadline turns; if model-latency named — deferred R-item carries latency data, deadline NOT widened.

**Both:** server suite green (baseline 894/0/0/1); no eval-spec edits; no deadline config changes; no new Tier-0; §7 stanza present (multi-layer prospective per-track) and dev §6 self-walk consistent.

## 6. Sprint-close header (top of `docs/codex-findings.md`)

```
## Sprint Review Decision
decision: pass | fix_required | out_of_scope_review
blocking_count: <number>
summary: <one paragraph: tracks delivered or deferred; matrix completeness; bundle outcomes; any blocking findings with file paths>
```

Then per-finding sections: number, severity (P0–P3), file + line cite, description, recommended action.

## 7. Deferral-to-action_bank

Out-of-scope concerns (improvements outside scope, deferred root causes, dev-proposed R-items lacking evidence) → recommend `docs/action_bank.md` entries in verdict body (name, layer, source evidence), not blocker findings.

## 8. Do not

- Do not propose a code fix; name the layer per §3.1 only.
- Do not rewrite the handoff.
- Do not block on §4 packaging artefacts unless content is substantively wrong.
- Do not invent a new Tier-0; route §3.2 Q2 stop-gates to `human_review_required`.
- Do not flag investigation-only output as failure — acceptable per bundle-or-defer policy.

Stop. Read the objective + handoff. Walk §2 + §3 + §5.
