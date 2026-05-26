# Sprint 48 / S-Cleanup-2 — Dev Implementation Prompt

You are the dev agent for Sprint 48 / S-Cleanup-2, the second sub-
sprint of Milestone M4-Eval-Cleanup. This sub-sprint has 2 items:
Alice bad-case fixture schema unification (Item #5) + executor suite-
mode annotation (Item #2). **Read this prompt + the contracts listed
in §1 before writing any code.**

## 1. Read order (cold start)

1. `AGENTS.md` (auto-loaded via constitution chain — `doc_governance.md`,
   `agent_context_guide.md`, `iteration_governance.md`). Do not re-read
   if already in context.
2. `docs/milestone_objective.md` — M4-Eval-Cleanup milestone contract
   (read §3 S2 sub-sprint paragraph for scope rationale; §10 stop
   conditions; §6 milestone-level hard fences).
3. `docs/sprint_objective.md` — S-Cleanup-2 contract (this is your
   primary contract).
4. `docs/sprints/sprint-047-handoff.md` — preceding sub-sprint (S-
   Cleanup-1) close; understand the `_OPT_IN_SETS` registration
   pattern landed in Item #1 — Item #2 of S2 will cross-reference it.
5. `docs/10-handoff.md` §1 lead — confirms baselines post S-Cleanup-1
   close.

**Do NOT re-read** the other M3-Eval sub-sprint archives unless you
need a specific reference; the contracts above carry the active state.

## 2. Items to land

### Item #5 — Alice bad-case fixture schema unification

**File**: `eval_interactive/case_specs/bad_cases/alice_uc_a_uc_h_misclass.yaml`.

**Pre-edit verification**: read the file end-to-end. Confirm Alice
carries `outcome_checks:` with 7 entries + `llm_judge_dimensions:` with
3 entries (per dev S-Cleanup-1 handoff §6 Item #7 reference). Read the
11 sibling bad cases briefly to confirm they have empty lists for both
(the M3-Eval design-intent schema).

**Strip direction (per milestone §3 S2 + human approval 2026-05-23)**:
Strip Alice's `outcome_checks:` list to empty + `llm_judge_dimensions:`
list to empty.

**PRESERVE (mandatory; STOP condition if any would change)**:
- `closure_criterion` — full content UNCHANGED. This is the §5.6
  human-judgment-gate input; load-bearing.
- `bad_case_metadata.source_session_id` — UNCHANGED.
- `bad_case_metadata.surfaced_by` — UNCHANGED.
- `bad_case_metadata.surfaced_date` — UNCHANGED.
- `bad_case_metadata.failure_shape` — UNCHANGED.
- `bad_case_metadata.expected_behavior` — UNCHANGED.
- All other Alice CaseSpec fields (case_id, primary_uc, secondary_ucs,
  turns, persona, expected, etc.).

**Verification**:

```bash
# Confirm strip
grep -A 3 "^outcome_checks:" eval_interactive/case_specs/bad_cases/alice_uc_a_uc_h_misclass.yaml
grep -A 3 "^llm_judge_dimensions:" eval_interactive/case_specs/bad_cases/alice_uc_a_uc_h_misclass.yaml
# Confirm preserve
grep -A 5 "closure_criterion:" eval_interactive/case_specs/bad_cases/alice_uc_a_uc_h_misclass.yaml
grep -A 8 "bad_case_metadata:" eval_interactive/case_specs/bad_cases/alice_uc_a_uc_h_misclass.yaml
# Confirm loads
cd eval_interactive && uv run python -c "from eval_interactive.case_spec.loader import load_case_spec; from pathlib import Path; cs = load_case_spec(Path('case_specs/bad_cases/alice_uc_a_uc_h_misclass.yaml')); print(cs.case_id, 'outcome_checks:', cs.scoring.outcome_checks, 'llm_judge_dimensions:', cs.scoring.llm_judge_dimensions)"
```

**STOP if S2 planning reveals reverse direction is correct** (i.e., 11
bad cases should grow legacy fields instead): halt; surface to deliver-
agent for re-scope. Most likely indicator: M3-Eval design docs +
S-Eval-4 archive evidence that empty-list schema was a TEMPORARY dry-
run shape, not permanent design intent. Read `docs/milestones/M3-Eval_objective.md`
and `docs/sprints/sprint-045-objective.md` (S-Eval-4) + `sprint-045-handoff.md`
to verify direction before stripping.

### Item #2 — Executor suite-mode annotation

**Goal**: For `bad_cases/` + `anchor_outcome/` suites (the §5.6 human-
judgment suites), make explicit in report output that programmatic
`case_passed` PASS/FAIL is NOT the gate; human review of
`closure_criterion` against `per_turn_trace` is the gate.

**Approach (dev's discretion to implement; preferred shape)**:

1. Add a `case_passed_authority: Literal["human_review", "programmatic"]`
   field (or equivalent) to `CaseResult` schema (or to the per-case
   result JSON object emitted by the executor).
2. In `executor.py:_build_case_result` (or equivalent location),
   determine the authority by checking whether the CaseSpec's source
   directory matches an opt-in suite name (cross-ref `_OPT_IN_SETS`
   from `eval_interactive/eval_interactive/batch/sets.py` landed in
   S-Cleanup-1).
3. Set authority = `"human_review"` for cases originating from
   `bad_cases/` or `anchor_outcome/`; `"programmatic"` for all others.
4. In stdout (per-case or per-run header), add an explicit annotation
   when authority = `"human_review"`:
   - Per-case: e.g., `  HUMAN_REVIEW {case_id}  (programmatic={PASS|FAIL}; human review of closure_criterion required per §5.6)`
   - Per-run header: e.g., `Suite type: human_judgment (per §5.6); programmatic PASS/FAIL is informational only`

**Files likely to touch**:
- `eval_interactive/eval_interactive/batch/executor.py` — `_build_case_result` and the stdout `click.echo` logic.
- `eval_interactive/eval_interactive/case_spec/schema.py` OR `eval_interactive/eval_interactive/batch/case_result.py` (whichever holds the `CaseResult` dataclass) — add the new field.
- `eval_interactive/eval_interactive/batch/sets.py` — possibly add a helper `is_opt_in_suite(suite_name: str) -> bool` or `is_human_judgment_suite(path: Path) -> bool` to keep the determination centralized.

**Files NOT to touch (S3 territory)**:
- `eval_interactive/eval_interactive/scoring/composite.py` (S3:
  `handover_completeness` / `case_id_present` demotion).
  **EXCEPTION**: if `case_passed_authority` needs to surface on
  `CompositeScore` for downstream consumers, that narrow extension is
  in scope, but no other composite.py touch.
- `eval_interactive/eval_interactive/scoring/skill_procedure_check.py`
  (S3 Tier-2 design decision).
- `eval_interactive/eval_interactive/batch/executor.py:365-392`
  (`_compute_tier2_result` — S3 Tier-2 wiring).

**Verification**:

```bash
# Bad-case suite smoke
cd eval_interactive && uv run eval-interactive run --set bad_cases --parallel 1 --label s-cleanup-2-smoke-bad-cases
# Check results.json for case_passed_authority
jq '.case_results[0:2] | .[] | {case_id, case_passed_authority}' results/<latest>/results.json
# Anchor smoke (1-2 cases) — should show programmatic
uv run eval-interactive run --path case_specs/anchor/cs_interactive_001.yaml --label s-cleanup-2-smoke-anchor
jq '.case_results[0] | {case_id, case_passed_authority}' results/<latest>/results.json
```

Expected: bad_cases authority = `"human_review"`; anchor authority =
`"programmatic"`.

**NEW tests (add for Item #2)**:
- Test that `_build_case_result` sets authority correctly based on
  suite origin (bad_cases / anchor_outcome → human_review; others →
  programmatic).
- Test that an unknown suite path defaults to `"programmatic"`
  (safety; should be unreachable but defensive).

## 3. Hard fences (from sprint_objective §5 — DO NOT VIOLATE)

- No `server/src/main/java/**` runtime code touch.
- No `server/src/main/resources/skills/*.yaml` touch.
- No `server/src/main/resources/system_prompt.txt` touch.
- No `docs/runtime_freeze_and_risk_policy.md` touch.
- No `docs/current/iteration_governance.md` touch (S3 territory).
- No `docs/current/doc_governance.md` or `agent_context_guide.md`
  touch.
- No `docs/sprints/sprint-NNN-*` touch (immutable archives; only NEW
  `sprint-048-handoff.md`).
- No `docs/milestones/*` touch.
- No `docs/10-handoff.md` touch.
- No `docs/action_bank.md` touch.
- No `docs/codex-findings.md` touch.
- No `docs/milestone_objective.md` touch.
- No `eval_interactive/case_specs/bad_cases/cs*.yaml`, `fg5q*.yaml`,
  `iwzx*.yaml`, `wmkb*.yaml` touch (only `alice_*.yaml`).
- No `eval_interactive/case_specs/bad_cases/_manifest.md` touch.
- No `eval_interactive/case_specs/shadow/` reads.
- No `eval_interactive/eval_interactive/scoring/composite.py` touch
  except narrow `case_passed_authority` surface extension if
  necessary.
- No `eval_interactive/eval_interactive/scoring/skill_procedure_check.py`
  touch.
- No `eval_interactive/eval_interactive/batch/executor.py:365-392`
  touch.

## 4. §4.1 anti-hardcode self-walk

Before producing the handoff, walk the §4.1 9-question kernel from
`iteration_governance.md` against your S2 commit and capture the
result in handoff §3. **Expected outcome**: clean `approve` per
Q1-Q9 because S2 is fixture migration + infra annotation (no
semantic hardcode introduced; no semantic decision LLM→Java; no
forbidden primitive). If any question's answer is non-trivially
"yes" or "concern", surface as OQ in handoff §8.

## 5. Tests to run

- **Python (full suite)**: `cd eval_interactive && uv run pytest
  --tb=no -q` — no new failures from S2; new tests for Item #2
  should PASS.
- **Java (full suite)**: `./gradlew :server:test` — baseline
  `1163 / 1-inherited / 0 / 2` must remain unchanged.
- **Eval harness smoke (REQUIRED for S2 close)**:
  - `cd eval_interactive && uv run eval-interactive run --set
    bad_cases --parallel 1 --label s-cleanup-2-bad-cases-smoke`
    (12 cases; required for Item #5 regression-safety verification
    + Item #2 authority field verification).
  - `cd eval_interactive && uv run eval-interactive run --path
    case_specs/anchor/cs_interactive_001.yaml --label
    s-cleanup-2-anchor-smoke` (1 case; required for Item #2
    `programmatic` authority verification).
- **Alice case regression-safety**: deliver-agent + human will review
  Alice's `per_turn_trace` + `closure_criterion` evidence post-close
  to confirm the failure-shape evidence is qualitatively unchanged
  from M3-Eval close. Dev should not block on this; just produce the
  evidence (the smoke results.json above suffices).

## 6. Handoff requirements

Produce `docs/sprints/sprint-048-handoff.md` per `docs/sprint_objective.md`
§11 schema. Section 12 (closure verdict) reserved for deliver-agent
+ human.

**Numbers-cite discipline** (per `feedback_deliver_agent_cited_numbers
_must_be_reproducible.md`): every numstat / line-count / test-count
number in the handoff MUST be derived from a reproducible command.
Document the command alongside the number in handoff §2 / §4 / §5.

## 7. Bundle policy (per `iteration_governance.md` §8.7)

- Stage ONLY S2-scope files. Run `git status` before committing;
  enumerate staged files in the commit message.
- Do NOT `git add -A` or `git add .`.
- Deliver-agent files (close-bundle artefacts) are NOT staged by you.
- Dev commit message format: summary line + per-item breakdown +
  R-item flip request (`R-bad-case-fixture-migrate-to-l3-judge-dims`
  → CLOSED) + baseline citation.

## 8. Self-check (before claiming sub-sprint complete)

- [ ] Item #5: Alice fixture stripped per §2 strip direction; all 6
  preserve-mandatory fields UNCHANGED?
- [ ] Item #5: Alice case still loads end-to-end (no schema
  validation error)?
- [ ] Item #2: `case_passed_authority` field added to schema;
  executor branches on opt-in suite membership?
- [ ] Item #2: bad_cases smoke shows authority = `"human_review"`;
  anchor smoke shows `"programmatic"`?
- [ ] Item #2: NEW tests for authority logic PASS?
- [ ] Alice strip direction verified against M3-Eval design + S-Eval-4
  archive (handoff §7)?
- [ ] Java baseline unchanged (`1163 / 1-inherited / 0 / 2`)?
- [ ] Python baseline preserved (no new failures from S2)?
- [ ] Hard fences honored (zero touches under §3)?
- [ ] Handoff sections 1-11 complete; §12 reserved?
- [ ] Numbers-cite discipline followed?
- [ ] Bundle stages only S2-scope files?
- [ ] §4 anti-hardcode self-walk produces clean `approve`?

If any checkbox is unchecked, surface to deliver-agent BEFORE
declaring sub-sprint complete — do not silently ship partial work.
