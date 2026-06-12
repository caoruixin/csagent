# Codex review prompt — M-Auto-7 S-Y1.5b/c combo (Sprint 090, per-sub-sprint §4.3)

你是 **Anti-Hardcode + per-sub-sprint Review Agent for the M-Auto-7
S-Y1.5b/c combo (Sprint 090)**. This is a per-sub-sprint §4.3 review
(trigger #3 — the PR modifies the §1.7 anti-hardcode enforcement
detector). It runs BEFORE the downstream S-Y2 Part C `-n 3` re-tranche.

**Cumulative scope:** `git diff 9c62a86^..75b302c -- autoloop/` — the
combined S-Y1.5b + S-Y1.5c diff on six autoloop files. (The intervening
commit `1b18474` is docs-only — the plan + dev prompts — and is excluded
by the `-- autoloop/` path filter; ignore it.)

- **S-Y1.5b** = commit `9c62a86` (baseline-whitelist suppression).
- **S-Y1.5c** = commit `75b302c` (severity calibration). **THIS is the
  unreviewed half — focus your full nine-question walk here.**

## Prior-state note — read first

S-Y1.5b (`9c62a86`) **already passed a standalone §4.1 review**: Kernel
verdict `approve`, `blocking_count: 0`, with three non-blocking
observations (OQ-S90.1 greedy-trailing-span residual; OQ-S90.2
substring-suppression duplicate/context bypass accepted for v1; OQ-S90.3
no live Q6 surface). That review body is currently in
`docs/codex-findings.md`. Your task for b is to **re-confirm it under the
combined lens** (does c interact with the suppression logic in any way
that changes the b verdict?) — do NOT re-derive it from scratch. Your
task for c is a **full review**.

## Loader (minimal)

1. `AGENTS.md` (auto-loaded — governance chain).
2. **This prompt** — self-contained; the kernel + focus questions are
   embedded below.
3. The cumulative diff: `git diff 9c62a86^..75b302c -- autoloop/`.
4. Dev handoff (both sections): `docs/sprints/sprint-090-handoff.md` —
   the S-Y1.5b section + the appended `## S-Y1.5c` section (Class, §7c
   stanza, severity table, §3.4 dry-run evidence exp-77, reversibility
   example). Dev produces this; not embeddable.
5. Existing b review body: `docs/codex-findings.md` (current content).

Binding contract (read-only reference, do not re-review the doc itself):
`docs/solutions/2026-06-11-sy15bc-combo-implementation-plan.md` §3.

## What S-Y1.5c shipped (the diff to judge)

Six files, `git show --stat 75b302c`: `anti_hardcode_check.py` (+46),
`config.yaml` (+22), `meta_agent/prompts/propose.txt` (+35),
`tests/test_anti_hardcode_check.py` (+221), `tests/test_propose_prompt.py`
(+23 new), `tests/test_real_meta_agent_calibration.py` (+11 docstring).

**The behavioral change is severity only.** In `_RULES`
(`anti_hardcode_check.py:364-375`) six entries flip `_FAIL`→`_FLAG`:
`Q1.enumerated_or_keywords`, `Q1.if_then_decision_tree` (this single id
also covers the arrow form via `_q1_if_then` — there is no standalone
`Q1.arrow_tree` id), `Q2.must_always_against_soft_dimension`,
`Q5.bot_must_always`, `Q5.do_not_consider_soft_dimension`,
`Q5.force_assistant_to`. Four entries stay `_FAIL`:
`Q1.contains_or_matches_literal`, `Q2.tier0_invariant_invention_attempt`,
`Q4.case_id_literal`, `Q4.id_assignment_literal`.
`Q5.standalone_must_borderline` was already `_FLAG`. A new
`_effective_severity(rule_id, default, config)` helper
(`anti_hardcode_check.py:379`) lets `anti_hardcode.severity_overrides`
(config.yaml:413, default `{}`) re-promote any rule per-experiment; it is
consulted in the rule loop at line 445. `propose.txt` gains an
"# Acceptable patterns" section (line 65) clarifying that few-shot
examples / principle-level narrative / sequence narration / schema
legibility are NOT §1.7 rule dumps.

## Scope exemption check

This PR is **NOT** eligible for the pure-infra blanket exemption: it
changes what the §1.7 anti-hardcode enforcement detector treats as a
FAIL-vs-FLAG outcome. (The standalone b review made the same call.) Walk
the full nine questions.

## §4.1 Nine-Question Anti-Hardcode Kernel (embedded — canonical)

```text
You are the Anti-Hardcode Review Agent. The PR below proposes a change
to this repo. Your job is to decide whether the change introduces a
semantic hardcode — a keyword / regex / if-else / enum / per-UC matrix
that encodes a decision the LLM is supposed to own under
docs/current/iteration_governance.md §1.3 — and to issue a verdict.

Scope exemption: pure infra, docs-only, config-governance, and
characterization-test PRs are not subject to this review. If the PR
is purely one of those, return `approve` with a one-line note naming
the exemption.

For every other PR, walk these nine questions in order. For each
"yes" or each concern, paste the diff snippet and the reasoning.

1. Does the PR add a keyword / regex / if-else / enum / per-UC matrix
   for a semantic decision (drift detection, escalation, UC
   selection, risk classification, follow-up, intake routing)?
2. If yes to (1), is the change justified as protecting a current
   Tier-0 invariant named in docs/runtime_freeze_and_risk_policy.md
   §1 / §2?
3. Could the same outcome be achieved by projecting a soft signal to
   the LLM (an additional projected slot, a candidate list, a
   diagnostic flag) instead of a hard branch in Java or the prompt?
4. Does the change encode visible-eval case text, trace-specific
   phrasing, or a CaseSpec id into runtime, prompt, or judge config?
5. Does the change move semantic ownership from the LLM to Java —
   that is, shrink what docs/current/iteration_governance.md §1.3
   says the LLM owns?
6. Does the change add an if-else block to the prompt instead of
   principle-level or observable-state guidance?
7. Does the change preserve tool schema, capability / permission
   boundary, PII / safety floor, and grounding floor?
8. Does the PR ship generalization eval coverage — target, neighbor,
   negative, and shadow cases — and not only the target case?
9. If the change is temporary, does it carry an explicit rollback or
   sunset plan (downgrade-to-signal trigger, retirement sprint id)?

Return exactly one verdict:

- `approve`
- `approve with downgrade-to-signal follow-up` (name the trigger)
- `reject as semantic hardcode`
- `needs human architecture decision`

Do not rewrite the PR. Do not propose a code fix beyond naming the
layer in docs/current/iteration_governance.md §3 that the fix should
target.
```

## S-Y1.5c focus questions (apply in addition to the kernel)

1. **Severity-demote scope correctness.** Is the FAIL set reduced to
   ONLY rules whose surface form == constitutional intent
   (`Q4.case_id_literal`, `Q4.id_assignment_literal`,
   `Q2.tier0_invariant_invention_attempt`,
   `Q1.contains_or_matches_literal`)? Are the six demoted rules
   genuinely semantic-judgment-requiring per kernel Q3/Q6? Read each
   rule's regex + predicate against `_SOFT_SEMANTIC_DIMENSIONS`.
2. **Demoted-fixture per-fixture review.** For each individual
   per-pattern test in `test_anti_hardcode_check.py` flipped
   FAIL→FLAG, read the added Kernel Q3/Q6 rationale comment; verify the
   demote is justified. ≥1 unjustified demote ⇒ this half FAILs.
3. **Q4 default unchanged.** Confirm the default `_RULES` registry still
   FAILs `cs011` / `cs_uc_a_no_ad_id` literals + `session_id=` /
   `case_id=` assignments. (The override map MAY demote Q4 — intentional
   reversibility — but the DEFAULT must stay FAIL.)
4. **propose.txt clarification compatibility.** Read the new
   "# Acceptable patterns" section (line 65). Does it open any §1.7
   bypass the proposer could exploit (framing real rule dumps as
   "few-shot examples")? Compare against kernel Q3 + Q6.
5. **FLAG_FOR_CODEX surfacing path.** Verify the iteration record schema
   (`anti_hardcode_flag_for_codex` + `anti_hardcode_verdict`) is
   preserved end-to-end through `loop.py` (the FLAG-attach path at
   ~line 271), the `experiments.jsonl` row, and
   `runs/<id>/anti_hardcode_verdict.json`. Without surfacing, the demote
   becomes a §1.7 bypass (FAILs would silently vanish).
6. **Shadow firewall unaffected.** Confirm neither b nor c reads or
   routes shadow data; the detector reads only `before_value` +
   `after_value` strings.
7. **Scoring-code SHA stable.** Confirm no file in
   `autoloop/autoloop/scoring/` is touched;
   `gaming.scoring_code_drift.sha_changed` must NOT fire post-merge
   (`scoring_code_baseline_sha = 0d86b08f…`, config.yaml:340, unchanged).
8. **Reversibility actually reversible.** Confirm a unit test sets
   `severity_overrides: {"Q1.if_then_decision_tree": "FAIL"}` and a NEW
   Q1.if_then pattern then returns `verdict=FAIL`. Without this hatch the
   demote is one-way.

## Deliver-agent pre-verified facts (independent confirmation expected)

The deliver-agent verified the following at dispatch; re-verify rather
than trust:

- `75b302c` touches `autoloop/**` only (6 files above).
- `_RULES` severities: 4 FAIL / 6 demoted-to-FLAG / 1 already-FLAG = 11.
- `scoring/` untouched; `scoring_code_baseline_sha` reproduces → no
  re-bless required.
- §3.4 dry-run sanity (handoff): exp-77 clean on-target snapshot
  (anti_hardcode PASS, principle-level narrative, no Q4/always-must
  leakage); exp-78 was a proposer-LLM `APITimeoutError` that never
  reached the detector (env/network, non-load-bearing). Verdict PROCEED.
- No re-bless, no §5.6 bad-case rerun (no agent semantic surface
  touched), no Skill YAML / CaseSpec / Java change.

## Output format — write into `docs/codex-findings.md`

Emit **two split §4.2 stanzas** (one per sub-sprint), replacing the
current single-header b content with the per-sub-sprint format:

```
## Sub-sprint Review Decision — S-Y1.5b
decision: <pass | fix_required | out_of_scope_review>
blocking_count: <number>
summary: <one paragraph — may cite the existing standalone approve;
  note any combined-lens re-confirmation>

## Sub-sprint Review Decision — S-Y1.5c
decision: <pass | fix_required | out_of_scope_review>
blocking_count: <number>
summary: <one paragraph>
```

Below the two headers, keep your focused-review detail + the §4.1
nine-question kernel walk + the per-PR kernel verdict for each half.
Preserve the existing b non-blocking observations (OQ-S90.1/2/3); add any
new c observations as R-items / OQs for the deliver-agent to triage.

## Constraints

- Do NOT edit code or tests. Review only.
- Do NOT re-judge the §3.4 human/deliver dry-run verdict; assess only
  whether the evidence supports PROCEED.
- This is a per-sub-sprint review (trigger #3); the milestone-shared
  M-Auto-7 review still fires separately at milestone close.
- If you find a real demote that leaks hardcode (a FLAG that should be
  FAIL), name the rule_id + the kernel question it fails; the
  reversibility hatch is `anti_hardcode.severity_overrides` (re-promote
  to FAIL with no code change) — name it as the remedy, do not write it.
