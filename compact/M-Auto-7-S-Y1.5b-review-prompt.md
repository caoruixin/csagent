# Codex review prompt — M-Auto-7 S-Y1.5b (Sprint 090 / S-Auto-35)

你是 **Anti-Hardcode Review Agent for M-Auto-7 S-Y1.5b** (Sprint 090 / S-Auto-35).
This is a **per-sub-sprint review (§4.3 trigger #3)** of a single commit that modifies
the autoloop propose-stage anti-hardcode detector. Your verdict gates the downstream
S-Y2 Part C `-n 3` verification re-tranche.

## Commit under review

- **`9c62a86`** — "M-Auto-7 S-Y1.5b — autoloop anti-hardcode baseline-whitelist
  suppression (S-Auto-35)". Diff: `git show 9c62a86`.
- Two files only:
  * `autoloop/autoloop/sandbox/anti_hardcode_check.py` (+104 / −59)
  * `autoloop/tests/test_anti_hardcode_check.py` (+144 / −0)

## Loader (minimal)

1. `AGENTS.md` (auto-loaded — governance chain; §1.7 forbidden list; §4.1 kernel).
2. **This prompt** (self-contained context + embedded kernel below).
3. Dev product (read, not embedded): `docs/sprints/sprint-090-handoff.md` (esp. §6
   decisions, §10 review plan, §11 open questions).
4. Source: `autoloop/autoloop/sandbox/anti_hardcode_check.py` at `9c62a86`.

## IMPORTANT — do NOT take the blanket infra exemption

The §4.1 kernel grants a scope exemption to "pure infra" PRs. **This PR is infra, but
it modifies the §1.7 anti-hardcode enforcement surface itself** (the detector that
implements the forbidden-list guard), so per-sub-sprint Codex is REQUIRED (§4.3 trigger
#3). **Do not auto-`approve` on the infra exemption.** Walk the focused questions below
— the risk here is not that the PR *adds* a hardcode, but that it *weakens the
detector's ability to catch* one.

## What the change does (claimed)

Adds **baseline-whitelist suppression** at the `anti_hardcode_check()` entry point: a
propose-stage rule match is ignored when the substring it matched (`m.group(0)`, after
`_normalize`) already exists in the candidate's untouched `before_value` (normalized
with the same `synonym_map_enabled` flag). Mechanism: a new `_first_new_match(regexes,
text, before_norm, *, predicate, render)` helper iterates every regex match
(`finditer`) and returns the first match that (a) satisfies the rule's existing
predicate and (b) whose span is not in `before_norm`. All 11 rule helpers
(Q1×3 / Q2×2 / Q4×2 / Q5×4) were refactored from `re.search` first-match to this helper.

**Why** (verified defect): the detector scans the full `after_value`, never a diff
(`anti_hardcode_check.py:353`). The baseline `resolve_faq_grounded_answer.yaml`
`$.procedure` contains `...when available...) -> record_outcome. Only escalate...`
which `_normalize(synonym_map_enabled=true)` turns into an `if ... → ...` arrow-tree
matching `_RE_Q1_ARROW_TREE`. Run-1 candidates exp-70 and exp-76 (pure-additive appends
to `$.procedure`) were both killed pre-eval with a **byte-identical, 100%-baseline**
`matched_substring`. RCA + design: `docs/solutions/2026-06-09-anti-hardcode-q1-pure-
additive-false-positive.md` (Option B).

## Focused review questions (answer each with diff snippet + reasoning)

**A. Suppression correctness / semantic equivalence.**
1. Is "suppress a match whose `m.group(0)` is in `before_norm`" semantically
   equivalent to "the candidate did not ADD this pattern"? Confirm `_normalize` is the
   **identical** function applied to both `before_value` and `after_value` with the
   **same** `synonym_map_enabled` flag (entry point: the `before_norm = _normalize(...)`
   line vs the existing `normalized = _normalize(text, ...)` line).
2. Is suppression tested against the matched **span** (`m.group(0)`) and never the
   rendered window (Q2-tier0 ±80, Q4-id_assign −20/+40)? Confirm the window is
   display-only.

**B. Detector strength — does the change WEAKEN catching genuinely-new hardcode?**
3. Confirm the Q1–Q5 rule-body regexes and `_normalize` (incl. `_SYNONYM_MAP`,
   `_RE_WHEN_WORD_BOUNDARY`) are **byte-identical** to the parent of `9c62a86` — the
   diff should show ONLY: the new `_first_new_match` helper, per-rule first-match→
   iterate refactors, and the entry-point `before_norm` line. Flag any regex/normalize
   delta.
4. **Iterate-semantics change (deliberate, dev §6):** the predicate rules (`_q2_must_
   always`, `_q2_tier0_invention`, `_q5_do_not_consider`) now iterate ALL regex matches
   and return the first predicate-qualifying one, where the OLD code tested only the
   FIRST regex match's predicate and returned None otherwise. This makes detection
   **stricter** (catches a later qualifying match the old code missed). Confirm this is
   (a) the safe direction for a §1.7 gate and (b) outcome-neutral on the existing
   fixtures + calibration. Flag if you believe it introduces a NEW false-positive class
   on legitimate prose.
5. **Double-encoding blind spot (accepted trade-off, OQ-S90.2):** a candidate that
   copies a baseline forbidden pattern verbatim to a NEW position is suppressed (still
   `in before_norm`) and will NOT fire. Is accepting this — visible to human/Codex
   audit, not blocked at propose-stage — acceptable for v1, or does it need a
   count-based guard now?
6. **Greedy-trailing-span residual (OQ-S90.1):** for rules with greedy trailing
   captures (`[\s\S]{1,120}` / `[\s\S]{0,80}`), a baseline pattern immediately followed
   by NEW appended text within the trailing window yields a span that is NOT a clean
   baseline substring → not suppressed → still FAIL. exp-70/76 are unaffected (appends
   start past the trailing window). Confirm this residual does not silently re-block the
   class this patch targets, and note whether the diff-anchored Option C is the durable
   follow-up.

**C. Scope / fences / D2.**
7. Only `anti_hardcode_check.py` + `test_anti_hardcode_check.py` touched; no
   `scoring/*` change (`scoring_code_baseline_sha` reproduces — no re-bless); no
   `_RULES` registry-order change; the empty/whitespace `after_value` early-return and
   the `Q5.standalone_must_borderline` FLAG_FOR_CODEX path preserved.
8. **D2 self-discipline:** `anti_hardcode_check.py` contains no eval-CaseSpec literal /
   user utterance / expected-answer / case-success label as a rule literal (the
   embedded exp-70 strings must live in the TEST file only, not the detector).
9. Test adequacy: the 5 new tests prove both directions — baseline-pattern → PASS AND
   candidate-introduced pattern → FAIL (non-vacuity: each "suppressed→PASS" case FAILs
   with an empty baseline). Confirm the suite is green (dev: full autoloop pytest
   331→336).

## Then walk the §4.1 nine-question kernel

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

- `approve` — the change is not a semantic hardcode, or is justified
  as protecting a current Tier-0 invariant with adequate generalization
  coverage and a clear rollback if temporary.
- `approve with downgrade-to-signal follow-up` — the change is
  acceptable as an interim measure, but a follow-up sprint must
  convert it into a soft signal projected to the LLM. Name the
  trigger that should fire the conversion.
- `reject as semantic hardcode` — the change encodes a soft semantic
  decision the LLM should own; questions 1 and 2 fail, or questions
  5 / 6 fail, with no Tier-0 claim and no sunset plan.
- `needs human architecture decision` — the change crosses an
  unresolved governance question (new escalation reason enum value,
  new Tier-0 candidate, LLM-vs-Java boundary shift, new tool surface)
  and a human reviewer must decide before merge.

Do not rewrite the PR. Do not propose a code fix beyond naming the
layer in docs/current/iteration_governance.md §3 that the fix should
target.
```

## Output format — write to `docs/codex-findings.md` using the §4.2 header

```
## Sprint Review Decision
decision: pass | fix_required | out_of_scope_review
blocking_count: <number>
summary: <one paragraph: the kernel verdict (approve / … / reject), the answers to
focus questions A–C, and the disposition of OQ-S90.1 / S90.2 / S90.3>
```

## Constraints

- Do NOT edit code. Do NOT rewrite the PR.
- This is a per-sub-sprint review confined to commit `9c62a86`; the cumulative
  M-Auto-7 milestone-shared review is separate.
- Your verdict must complete BEFORE the S-Y2 Part C `-n 3` verification re-tranche runs.
- Carry OQ-S90.1 (greedy-trailing span) / OQ-S90.2 (double-encoding) / OQ-S90.3 (no
  live Q6 rule — the contract named "Q1/Q2/Q4/Q5/Q6" but the registry has Q1/Q2/Q4/Q5
  only; suppression covers all registered rules) as non-blocking observations unless
  you judge one to be blocking, in which case state why.
