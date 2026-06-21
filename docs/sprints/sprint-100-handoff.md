---
title: "Sprint 100 / S-Auto-48 (M-Auto-10 WP2 route-(a) follow-up) — dev handoff: align eval_spec + characterization + docs to the OQ-S93.1 ROUTE (a) canonical-closure verdict"
doc_tier: sprint-archive
status: current
implementation_status: implemented
source_of_truth: this file (dev handoff) + docs/proposals/m-auto-10-wp2-closure-path-canonical-decision.md (the approved verdict)
last_reviewed: 2026-06-21
review_cadence: ad hoc
supersedes: []
superseded_by: null
notes: >
  Dev handoff for Sprint 100 / S-Auto-48, the approved bounded ROUTE (a) follow-up
  to the Sprint 099 / S-Auto-47 (M-Auto-10 WP2) design verdict (commit 9882b93e;
  Codex design-review APPROVE c602ee3f; human Route (a) sign-off). Aligns the
  companion eval_spec, the Sprint 097 companion characterization test, and the
  closure-path documentation to the approved canonical contract: grounding-gated
  isResolvedSuccessTerminal is the canonical one-shot satisfiable RESOLVE closure;
  the explicit record_outcome→CONFIRM→CLOSE path is a sound multi-turn closure (not
  a runtime defect), structurally unreachable on a one-shot goal_achieved flow.
  eval_spec + docs only; NO server / runtime / prompt / simulator / scoring /
  baseline / canonical-pointer / PRIMARY-CaseSpec change; NO real-LLM run; §5.6
  bad-case rerun NOT run (deliver-side close gate). conditional_outcome_acceptance
  is byte-for-byte unchanged; no scored acceptance bar changed.
---

# Sprint 100 / S-Auto-48 (M-Auto-10 WP2 route-(a) follow-up) — dev handoff

**Sub-sprint:** M-Auto-10 WP2 **route-(a) follow-up** (the eval-contract + docs
corrections named in the verdict §4.1). **Not WP3** — route (a) cancels the
conditional WP3 runtime repair; this sub-sprint repurposes the freed, previously
WP3-reserved id **Sprint 100 / S-Auto-48** (the next valid unused Sprint / S-Auto
ids; history-checked — `S-Auto-48` / `Sprint 100` appeared only as the *reserved*
WP3 pointer in `milestone_objective.md` / `10-handoff.md` / `action_bank.md`, with
no sprint-100 archive or dev prompt on disk).

**Class:** `eval_spec` (advisory trace-expectation correction) + docs
reconciliation. **§7 stanza: EXEMPT** — ships no prompt / runtime-semantic /
eval-spec-scored / judge behaviour (the only eval_spec edit is to the **advisory**
`tool_sequence_match` input; no scored bar changes).

**Authoritative inputs:** the verdict doc
`docs/proposals/m-auto-10-wp2-closure-path-canonical-decision.md` (§4.1 named the
exact route-(a) corrections); Codex APPROVE `c602ee3f`; human Route (a) sign-off.

## 0. One-line result

The companion `cs_uc_a_loaded_listing_resolvable` `expected_tool_sequence` no
longer requires the structurally-unreachable `record_outcome`; the Sprint 097
characterization test is updated to pin the corrected sequence (and to positively
prove the canonical grounding-gated terminal still records RESOLVE); the OQ-S93.1
brief, the M-Auto-9 charter §3, the Sprint 097 handoff §5, and the verdict-doc /
sprint-099-handoff "CONFIRM-promoted" shorthand are annotated. **No scored
acceptance bar changed; no CaseSpec widened; no runtime/prompt/simulator/scoring/
baseline/canonical-pointer/PRIMARY change.**

## 1. Exact files changed (7)

| File | Change | Tier touched |
|---|---|---|
| `eval_interactive/case_specs/bad_cases/cs_uc_a_loaded_listing_resolvable.yaml` | Retire `record_outcome` (5th tool) from the **advisory** `expected_tool_sequence`; add an inline route-(a) rationale + non-widening proof comment. | eval_spec (advisory field only) |
| `eval_interactive/tests/test_sprint_097_uc_a_resolvable_companion.py` | Rename + update the tool-sequence test to assert the corrected 4-tool sequence and positively pin `record_outcome` retired; clarify the docstring of the canonical-terminal test (`test_satisfied_resolve_lands_pass`) and the module docstring. Negative controls unchanged. | characterization test |
| `docs/diagnostics/failure-briefs/oq-s93.1-confirm-record-vs-handover.md` | RESOLUTION banner (route (a); runtime-defect hypothesis REJECTED) + frontmatter `resolution:` + `last_reviewed`. | diagnostic |
| `docs/proposals/runtime-closure-record-outcome-design-milestone-oq-s93.1.md` | §3 design-target route-(a) annotation (grounding-gated is canonical one-shot; explicit path is multi-turn). | proposal (M-Auto-9 charter) |
| `docs/sprints/sprint-097-handoff.md` | §5 **additive forward annotation**: the OQ-S93.1 residual is resolved route (a); the delivered §5 record is preserved unchanged. | sprint-archive (additive only) |
| `docs/sprints/sprint-099-handoff.md` | §3 "CONFIRM-promoted" shorthand clarification (additive). | sprint-archive (additive only) |
| `docs/proposals/m-auto-10-wp2-closure-path-canonical-decision.md` | §3.1 "CONFIRM-promoted" shorthand clarification; frontmatter `implementation_status: implemented` + `implemented_by:`. | proposal (the verdict) |

**Archive-edit transparency:** `docs/sprints/sprint-097-handoff.md` and
`docs/sprints/sprint-099-handoff.md` are sprint-archive-tier. They were annotated
**additively** (clearly-dated forward notes; no delivered fact rewritten or
deleted) under (a) the explicit human instruction to "update the Sprint 097
handoff qualification" + "clarify any shorthand such as CONFIRM-promoted", and
(b) the verdict §4.1 #2, which named the Sprint 097 handoff §5 as a route-(a)
docs surface. This follows doc_governance's "annotate, don't delete" rule rather
than a fold-back rewrite. Flagged here for deliver/human/Codex visibility.

## 2. The route-(a) correction is NOT a scored-bar change (proof)

The verdict's load-bearing claim — retiring `record_outcome` from the required
sequence is **not** a `conditional_outcome_acceptance` relaxation — is verified in
code:

- **`tool_sequence_match` is advisory.** `OutcomeChecker._check_tool_sequence_match`
  (`eval_interactive/eval_interactive/scoring/outcome_checks.py:310-348`) returns
  `severity="advisory"` for every path; the module docstring (`:12-22`) and S-Eval-1
  (M3-Eval) state the composite scorer **excludes advisory results from the
  outcome-score mean and the mandatory-L2 gate**. So `expected_tool_sequence`
  feeds only a diagnostic LCS value, never a scored gate.
- **`correct_outcome` does not read the sequence.** `_check_correct_outcome`
  (`outcome_checks.py:200-243`) delegates to `evaluate_conditional_outcome` when a
  `conditional_outcome_acceptance` block is present; that evaluator reads the
  Phase-1 `user_state` series + the closure-quality marker, **not**
  `expected_tool_sequence`. The companion carries the block, so its scored
  `correct_outcome` is independent of the retired tool.
- **`conditional_outcome_acceptance` is byte-for-byte unchanged.**
  `git diff` on the companion shows **no `+`/`-` line** touching
  `schema_version` / `satisfied_outcome: resolve` / `unresolved_accept_outcome:
  escalate` / `require_closure_precondition: true`. Loader confirms the parsed
  block is identical: `{schema_version: 1, satisfied_outcome: resolve,
  unresolved_accept_outcome: escalate, require_closure_precondition: True}`.
- **Lint-clean.** `lint_spec_file` on the companion returns **0 errors** (lone
  pre-existing R11 `hidden_fact_not_duplicating_form` *warning*, unrelated to the
  tool-sequence field). Linter R6 only flags tools **not allowed** for the UC;
  removing a tool cannot introduce a violation.
- **No widening.** `closure_criterion`, persona, `hidden_facts`, `form_context`,
  and the `scoring` section are untouched; nothing about what counts as SATISFIED
  / grounded / resolved changed.

## 3. Test results

| Suite | Result |
|---|---|
| `tests/test_sprint_097_uc_a_resolvable_companion.py` (focused, zero-LLM) | **9 passed** — corrected tool-sequence test green; `test_satisfied_resolve_lands_pass` green (canonical grounding-gated terminal records RESOLVE with **no** `record_outcome` call); all negative controls preserved green (false-resolve→FAIL, unsatisfied-escalation→CONDITIONAL_ELIGIBLE, generic-answer→FAIL, goal_impossible→neutral/FAIL, acceptance keys on `user_state` not case-id). |
| `tests/test_s_eval_1_schema_and_scoring.py` + `tests/test_schema.py` | **30 passed** |
| `lint_spec_file(companion)` | **0 errors** (1 pre-existing R11 warning) |
| Full `uv run pytest` (eval_interactive) | **639 passed, 5 errors** — identical to the clean-tree baseline (`639p/0f/5e`, milestone_objective §3 WP1). The 5 errors are the documented pre-existing `test_rescore_s_auto_38_full_baseline.py` `KeyError: 'form_context'` (June-8 baseline reconstruction artifact); unrelated to this change. **No new failure; no test-count change** (one test renamed, none added/removed). |

No real-LLM run (none required; this is an advisory-field + docs change, not a
behaviour change — iteration_governance §5.7). The §5.6 curated bad-case rerun was
**NOT** run — it remains a **deliver-side milestone-close gate** after this
follow-up.

## 4. Residual status

- **OQ-S93.1 — RESOLVED / runtime-defect hypothesis REJECTED.** Route (a):
  grounding-gated `isResolvedSuccessTerminal` is the canonical one-shot satisfiable
  closure; the explicit `record_outcome→CONFIRM→CLOSE` path is a sound multi-turn
  closure (demonstrated landing end-to-end on the PRIMARY `cs_uc_a_loaded_listing`,
  `…015324`→`current_phase=CLOSE`), not a runtime closure defect; mechanism (iv) is
  ruled out. Recorded in the brief RESOLUTION banner + frontmatter `resolution:`.
- **OQ-S99.1 — PRESERVED, untriggered + unscheduled.** The optional Gate-D
  robustness / measurement-completeness question (Gate-D missed 3/11 satisfied
  one-shot users via two Gate-D-internal gaps) is preserved as a candidate open
  question in the verdict §4.2 and reaffirmed in the brief banner + verdict
  frontmatter. It is **NOT** a closure defect, **NOT** mechanism (iv), and was
  **NOT** scheduled or implemented in this sub-sprint (no action_bank scheduled row
  added).

## 5. Scope discipline / fences honoured

- No `server/` change; no runtime / phase-machine / prompt / `record_outcome`
  guard change; no simulator change; no scoring-code change; no baseline move; no
  canonical-pointer flip; no PRIMARY CaseSpec change
  (`cs_uc_a_loaded_listing.yaml` / `cs_uc_a_no_ad_id_ad_specific.yaml` untouched);
  no CaseSpec widen; no Tier-0 invariant added.
- The only eval_spec edit is to the **advisory** `expected_tool_sequence`; the
  scored `conditional_outcome_acceptance` bar is unchanged (§2).
- **Codex re-review:** not required — the implementation stayed within the
  approved bounded follow-up (verdict §4.1; the design review `c602ee3f` already
  assessed point 6 that this retirement is not a bar relaxation). Re-review would
  only be required if the implementation exceeded the approved bounds, which it did
  not.

## 6. Recommended verdict + remaining close gates

**Recommended verdict: COMPLETE** (bounded route-(a) follow-up delivered). The
M-Auto-10 close now needs the **deliver-side** gates: the §5.6 curated bad-case
suite rerun (no-regression vs the M-Auto-9 close baseline; safety + grounding
floors at 100%) + the milestone-shared Codex `pass`/0 + the deliver archive +
cold-start pointer updates (`sprint_objective.md` / `milestone_objective.md` WP3
row → cancelled by route (a) / `10-handoff.md`). Stopping after this dev handoff
per the contract.
