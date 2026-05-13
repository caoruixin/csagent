Paste the content below this line into a fresh Codex session after the dev agent's commits land on `design-v1-without-human-review`. No PR will be opened; review the recent commit range (HEAD vs. the SHA before the dev agent's first Sprint 20 commit — deliver agent names the range).

---

You are Codex reviewing **Sprint 20 (A+B)** — G2 Interactive Case Family + Shadow Split (Track A) + Already-Called Soft Signal (Track B). Load `AGENTS.md` → `doc_governance.md` → `agent_context_guide.md` → `iteration_governance.md` → `docs/sprint_objective.md`.

## Sprint-20 context (read first)

Two disjoint tracks. Track A = case-family authoring on eval-spec + v0 shadow-split mechanism; **Track A is NOT subject to Bundle-or-defer — the deliverable IS the data.** Track B = `prompt_projection` soft-signal slot (`already_called`) + regression test; bundled per the canonical `prompt_projection` bundle shape (Sprint 19 precedent).

**Track B's slot is NOT scope drift.** Slot is observability only; LLM owns consumption. Do not flag as hardcode / enum / Java-ownership-shift.

**Generalization coverage IS the deliverable for Track A.** Handoff §9 must show 10 families × target/neighbor/negative/shadow counts (≥1/≥2/≥2/≥2 per family) + shadow-split enforcement story. Missing table OR missing family for any G1 brief OR below the bars = **blocking**.

**Track B coverage:** target = manual-probe + cs_011 T2 (Sprint 19 §4.2 + §3.3); neighbor ≥1 smoke turn without prior identical-args call; negative ≥2 turns where slot is absent and re-emission is correct; shadow deferred (Track A authors). Three behaviour bars (populated / empty / runtime-doesn't-enforce) must be tested. Missing test = blocking.

**cs_192 reminder.** If Track A surfaces a `CONTRACT_VIOLATION:active_use_case` target the dev agent flags `human_review_required` per §3.2 Q2 in handoff §10, that IS the correct exit. Confirm; do not push for a new Tier-0.

**Out-of-scope (record as `docs/action_bank.md` deferrals, NOT blocking):** opinions on other R-items, G3+ remediation order, shadow enforcement strength beyond v0, prompt copy to consume the slot.

## Per-PR Anti-Hardcode Review (kernel — paste verbatim from `iteration_governance.md` §4.1)

You are the Anti-Hardcode Review Agent. The PR below proposes a change to this repo. 

Note: this is a local review, Review scope: git diff HEAD^..HEAD. Please review only the changes introduced by this latest commit. Do not review unrelated historical code or pre-existing untracked files unless they directly affect this commit

Your job is to decide whether the change introduces a semantic hardcode — a keyword / regex / if-else / enum / per-UC matrix that encodes a decision the LLM is supposed to own under `iteration_governance.md` §1.3 — and to issue a verdict.


Scope exemption: pure infra, docs-only, config-governance, and characterization-test PRs are not subject. If purely one of those, return `approve` with a one-line exemption note.

For every other PR, walk these nine questions in order. For each "yes" or each concern, paste the diff snippet and the reasoning.

1. Does the PR add a keyword / regex / if-else / enum / per-UC matrix for a semantic decision (drift, escalation, UC selection, risk, follow-up, intake routing)?
2. If yes to (1), is the change justified as protecting a current Tier-0 invariant in `docs/runtime_freeze_and_risk_policy.md` §1 / §2?
3. Could the same outcome be achieved by projecting a soft signal to the LLM instead of a hard branch in Java or the prompt?
4. Does the change encode visible-eval case text, trace-specific phrasing, or a CaseSpec id into runtime, prompt, or judge config?
5. Does the change shrink what `iteration_governance.md` §1.3 says the LLM owns?
6. Does the change add an if-else block to the prompt instead of principle-level or observable-state guidance?
7. Does the change preserve tool schema, capability / permission boundary, PII / safety floor, and grounding floor?
8. Does the PR ship target + neighbor + negative + shadow generalization coverage?
9. If temporary, does the change carry an explicit rollback / sunset plan?

Return exactly one verdict: `approve` | `approve with downgrade-to-signal follow-up` | `reject as semantic hardcode` | `needs human architecture decision`. Do not rewrite the PR; do not propose a fix beyond naming the §3 layer.

**Sprint 20 application notes.** Track A CaseSpec content should pass Q1/Q4/Q5/Q6 — families describe user shapes and expected behaviours, not runtime decisions; a CaseSpec encoding per-UC matrix or regex into its rubric IS a violation. Track B slot passes Q1 (no), Q3 (yes — slot IS the soft signal), Q5/Q6 (no — observability), Q7 (floors held), Q8 (test + Track A delivers shadow). Q8 for Track A is satisfied by the deliverable itself.

## Sprint-close header (top of `docs/codex-findings.md`)

Use the §4.2 header:

```
## Sprint Review Decision
decision: pass | fix_required | out_of_scope_review
blocking_count: <number>
summary: <one paragraph>
```

`fix_required` triggers: Track B slot introduces a forbidden hardcode; Track B slot missing the three-behaviour-bar test; silently-bundled `human_review_required`; Java guard without current Tier-0; CaseSpec encodes keyword / regex / per-UC matrix; shadow-split fails the §5.1 contract (dev-agent-readable); family missing for any G1 brief; family below the ≥1/≥2/≥2/≥2 bars without justification in handoff §11.

Per finding below the header: file path, diff snippet, failing §4.1 question, §3.1 layer (name only; do not propose fix), severity (blocking / non-blocking / out-of-scope-defer; defer items name the `action_bank.md` section to record them in).
