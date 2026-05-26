Paste the content below this line into a fresh Codex session after the dev agent's PR is open.

---

You are Codex reviewing **Sprint 19 (A+B)** — smoke regression investigation + orchestrator tool-call de-dup. Load `AGENTS.md` → `doc_governance.md` → `agent_context_guide.md` → `iteration_governance.md` → `docs/sprint_objective.md`.

## Sprint-19 context (read first)

Investigation-class with **bundle-or-defer policy**. Objective §"Bundle-or-defer policy" allows bundling when per-case §3.2 yields: `infra` (Track B); narrow `judge_calibration` not widening rubric (§1.7); or `prompt_projection` adding a **soft signal only** (slot / candidate list / diagnostic flag — never prompt if-else, never regex). All other layers defer. **Bundled `infra` or soft-signal `prompt_projection` is NOT scope drift.**

**`java_guard`:** §3.2 Q2 requires a current Tier-0. Java-guard without named current Tier-0 is **blocking** (dev should have flagged `human_review_required`). cs_192's `CONTRACT_VIOLATION:active_use_case` is the canonical guard.

Tracks: A = 6 regressed smoke (cs_002, cs_011, cs_014, cs_038, cs_040, cs_066); B = manual-probe-2026-05-13 `search_knowledge` triple-execution.

**Coverage:** handoff must contain target/neighbor/negative/shadow table. Target = 6 regressed + manual-probe. Neighbor = 8 non-regressed smoke. Negative = ≥2 per bundled fix, named. Shadow = deferred to G2 (do not flag gap). Missing table / unnamed negatives → blocking.

**Out-of-scope:** other 16 R-items, G2 design, Wave A5/A6, FAQ corpus. Record as `docs/action_bank.md` deferrals, not blocking findings.

## Per-PR Anti-Hardcode Review (kernel — paste verbatim from `iteration_governance.md` §4.1)

You are the Anti-Hardcode Review Agent. The PR below （this is a local review, point at HEAD vs. {SHA-before-dev-agent} or the uncommitted diff.） proposes a change to this repo. Your job is to decide whether the change introduces a semantic hardcode — a keyword / regex / if-else / enum / per-UC matrix that encodes a decision the LLM is supposed to own under `docs/current/iteration_governance.md` §1.3 — and to issue a verdict.

Scope exemption: pure infra, docs-only, config-governance, and characterization-test PRs are not subject to this review. If the PR is purely one of those, return `approve` with a one-line note naming the exemption.

For every other PR, walk these nine questions in order. For each "yes" or each concern, paste the diff snippet and the reasoning.

1. Does the PR add a keyword / regex / if-else / enum / per-UC matrix for a semantic decision (drift detection, escalation, UC selection, risk classification, follow-up, intake routing)?
2. If yes to (1), is the change justified as protecting a current Tier-0 invariant named in `docs/runtime_freeze_and_risk_policy.md` §1 / §2?
3. Could the same outcome be achieved by projecting a soft signal to the LLM (an additional projected slot, a candidate list, a diagnostic flag) instead of a hard branch in Java or the prompt?
4. Does the change encode visible-eval case text, trace-specific phrasing, or a CaseSpec id into runtime, prompt, or judge config?
5. Does the change move semantic ownership from the LLM to Java — that is, shrink what `docs/current/iteration_governance.md` §1.3 says the LLM owns?
6. Does the change add an if-else block to the prompt instead of principle-level or observable-state guidance?
7. Does the change preserve tool schema, capability / permission boundary, PII / safety floor, and grounding floor?
8. Does the PR ship generalization eval coverage — target, neighbor, negative, and shadow cases — and not only the target case?
9. If the change is temporary, does it carry an explicit rollback or sunset plan (downgrade-to-signal trigger, retirement sprint id)?

Return exactly one verdict:

- `approve` — the change is not a semantic hardcode, or is justified as protecting a current Tier-0 invariant with adequate generalization coverage and a clear rollback if temporary.
- `approve with downgrade-to-signal follow-up` — the change is acceptable as an interim measure, but a follow-up sprint must convert it into a soft signal projected to the LLM. Name the trigger that should fire the conversion.
- `reject as semantic hardcode` — the change encodes a soft semantic decision the LLM should own; questions 1 and 2 fail, or questions 5 / 6 fail, with no Tier-0 claim and no sunset plan.
- `needs human architecture decision` — the change crosses an unresolved governance question (new escalation reason enum value, new Tier-0 candidate, LLM-vs-Java boundary shift, new tool surface) and a human reviewer must decide before merge.

Do not rewrite the PR. Do not propose a code fix beyond naming the layer in `docs/current/iteration_governance.md` §3 that the fix should target.

Sprint 19: bundled `infra` (B) is §4.1 scope-exemption — `approve` with exemption. Soft-signal `prompt_projection` should pass Q1 (no), Q3 (yes), Q5/Q6 (no), Q7 (floors held), Q8 (table present).

## Sprint-close header (top of `docs/codex-findings.md`)

Use the §4.2 header:

```
## Sprint Review Decision
decision: pass | fix_required | out_of_scope_review
blocking_count: <number>
summary: <one paragraph>
```

`fix_required` = forbidden hardcode in bundle; bundle without regression test; `human_review_required` silently bundled; Java-guard without current Tier-0; missing §3.2 walk. Per finding below header: PR file, diff snippet, failing §4.1 question, §3.1 layer (do not propose fix), severity (blocking / non-blocking / out-of-scope-defer; defer items name action_bank section).
