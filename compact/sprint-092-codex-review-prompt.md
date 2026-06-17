You are the Anti-Hardcode Review Agent (Codex) for Sprint 092 / S-Auto-38.
Read-only review. Do NOT edit files. Return a written verdict only.

# What to review

Two independent commits on branch `auto-loop-branch` (parent 9c6f15c1):

- **WP-A** `57cd93c5` — splits the bundled `escalation_compliance` hard-check
  into Part-1 (`escalation_compliance`, escalate-vs-don't behaviour; STAYS in
  `autoloop/.../tier_evaluator._TIER0_PY_FAMILY`) and Part-2
  (`escalation_reason_family_match`, the escalation_reason FAMILY match;
  OBSERVATION-ONLY — `severity="advisory"`, NOT in `_TIER0_PY_FAMILY`). Files:
  `eval_interactive/eval_interactive/scoring/hard_checks.py`,
  `eval_interactive/eval_interactive/scoring/escalation_reason_match.py` (new),
  `autoloop/autoloop/scoring/tier_evaluator.py`, + tests.
- **WP-B** `8ed65cc6` — adds the unified `escalation:` override block to the v2
  override registry loader (`eval_interactive/eval_interactive/case_spec/extractor.py`)
  + a header doc-comment in `case_spec_overrides.yaml` + tests. Hard rules:
  `accepted_reasons` XOR `accepted_families`; `enforcement_level` ∈
  {observation, tier1_confirmed, tier0}; a `tier0` block is rejected unless the
  entry is `status: approved` AND `safety_critical: true` AND a `citation` is set.

Inspect the diffs with:  `git show 57cd93c5`  and  `git show 8ed65cc6`.

# Context / intent (sprint-088 OQ-E forensic)

The bundled check mixed a deterministic safety floor (Part-1) with a stochastic,
LLM-owned `escalation_reason` *label* (Part-2). At shadow n=5 Part-2's sampling
noise flipped the identical exp-82 candidate KEEP↔DISCARD via the tier-0 delta
gate. This sprint demotes ONLY Part-2 (the reason label, §1.3 LLM-owned) out of
the zero-tolerance tier-0 family; it weakens NO deterministic safety floor.

# Replay evidence to review ALONGSIDE the diff

Read `eval_interactive/analysis/out/replay_s_auto_38_output.txt` (zero-LLM
OLD/NEW re-score of baseline + exp-81..85 + exp82-reval). Key results: self-check
341/341; 43 tier-0 flips ALL `PART2_DEMOTION` (0 outside); 0 regressions
(OLD-keep→NEW-discard); flip ELIMINATED for cs11s01 + cs40s02; negative control
(should-escalate-but-didn't, risk=high) still fails Part-1 → discard; the 4 other
deterministic safety checks textually identical. The override consumption proof
(observation non-gating / tier1_confirmed composite-gate / tier0 Part-1-gate) is
in `eval_interactive/tests/scoring/test_escalation_override_wpb.py`.

# Your task

Apply the nine-question anti-hardcode kernel in
`docs/current/anti-hardcode-review-kernel.md` to (a) the WP-A scoring diff and
(b) the WP-B schema, reviewing them against the replay evidence. Note in
particular: does this change REMOVE an implicit hardcode (the eval-internal
reason-family→tier-0 binding) rather than add one? Is the override narrow,
explicit, and human-reviewed (citation/reviewer/rationale per entry) rather than
a keyword/enum dump? Does it preserve the tool schema, capability boundary, PII /
safety floor, and grounding floor?

Return exactly one verdict from the kernel's four options, with a one-paragraph
justification and any per-question concerns. Also emit the sprint-close header:

```
## Sprint Review Decision
decision: pass | fix_required | out_of_scope_review
blocking_count: <number>
summary: <one paragraph>
```
