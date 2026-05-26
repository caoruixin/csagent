# Sprint 49 / S-Cleanup-3 — Dev Implementation Prompt

You are the dev agent for Sprint 49 / S-Cleanup-3, the THIRD (and per
current plan, LAST) sub-sprint of Milestone M4-Eval-Cleanup, and the
highest-risk one. Two items: **#9 Tier-2 phase-plan-scoped fix** (a
confirmed `case_passed`-gate bug) + **#4 handover_completeness /
case_id_present demotion**. **Read this prompt + the contracts in §1
before writing any code.**

## 1. Read order (cold start)

1. `AGENTS.md` (auto-loaded constitution chain). Do not re-read if in
   context.
2. `docs/solutions/tier2_skill_traversal_design_memo.md` — **READ THIS
   FIRST AND IN FULL.** It contains the offline reproduction of the #9
   misflip, the mechanism analysis, and why Option (b) is the fix. Your
   implementation directly follows the memo §7 option (b) + §8.
3. `docs/sprint_objective.md` — S-Cleanup-3 contract (primary contract).
4. `docs/milestone_objective.md` §3 S3 + §6 hard fences + §10 stop
   conditions.
5. `docs/10-handoff.md` §1 lead (S-Cleanup-2 close baselines).

## 2. Item #9 — Tier-2 phase-plan-scoped evaluation (THE fix)

**The bug** (confirmed offline; see memo §3): `executor.py:_compute_tier2_result`
(lines 365-392) iterates ALL loaded Skills and evaluates each Skill's
`critical_steps` filtered ONLY by `active_use_case ∈ mandatory_for` —
with no filter for which Skill the runtime actually traversed. The
escalate Skill's `escalate-via-request-handover` step is `mandatory_for`
all 12 UCs with `trace_check: accumulated_tool_results.request_handover`,
so a correct UC-A FAQ-resolve session (no escalation) gets a mandatory
Tier-2 FAIL → `case_passed` flips to False. Wrong flip.

**The fix (Option b)**: scope Tier-2 evaluation to the critical-step ids
the runtime actually presented. The trace already carries them at
`per_turn_trace[].phase_plan.critical_steps[]` (each carries `{id,
desc}`; verified against `results/20260523-024518/results.json`).

Implementation outline (dev's discretion on exact structure):

1. In `_compute_tier2_result(per_turn_trace, active_use_case)`, FIRST
   collect the set of presented step ids:
   ```python
   presented_ids = set()
   for turn in per_turn_trace:
       pp = turn.get("phase_plan") or {}
       for step in (pp.get("critical_steps") or []):
           sid = step.get("id")
           if sid:
               presented_ids.add(sid)
   ```
2. When iterating loaded Skills' steps, evaluate a step ONLY if its `id`
   is in `presented_ids` (else treat as `N/A` — not applicable to this
   session's path). You may implement this by passing `presented_ids`
   into a new/extended extractor method, or by filtering the
   `all_results` post-hoc by step id. Keep the per-step N/A-by-
   `mandatory_for` semantics in `skill_procedure_check.py:extract()`
   UNCHANGED (do not change how a single step is evaluated; only change
   WHICH steps are fed/kept).
3. **Defensive default**: if `presented_ids` is EMPTY (older traces,
   error turns, or a run where no turn carried `phase_plan.critical_steps`),
   evaluate NOTHING → `tier2_results_to_gate(())` → inert PASS/advisory.
   Do NOT fall back to all-Skills (that re-introduces the bug). This
   preserves the S-Eval-2 empty-list backward-compat default.

**Empirical confirmation (REQUIRED — contract §4 step 1 + step 4)**:

- **Before** (step 1, BEFORE the fix): run `cd eval_interactive && uv run
  eval-interactive run --set anchor --parallel 1 --label s-cleanup-3-before`.
  Count how many of the 159 anchor cases hit the escalate-vs-resolve
  mandatory-pair misflip. Extract with jq from results.json (e.g., cases
  where `tier2` failed on `escalate-via-request-handover` while the case
  resolved without escalation). Record the number + the jq command in
  handoff §6. (This needs the backend up — `make backend` — and LLM keys.
  If the backend / keys are unavailable, STOP and surface; the offline
  repro in the memo is the logic proof but the contract wants the live
  before/after.)
- **After** (step 4, AFTER the fix): re-run the same command (`--label
  s-cleanup-3-after`); show the misflip-shape count dropped to 0. Record
  the before/after delta in handoff §6.

**NEW tests** (`tests/test_tier2_phase_plan_scoping.py` or extend an
existing Tier-2 test): per contract §3 generalization coverage —
- target: a synthetic resolve-no-escalate trace (phase_plan.critical_steps
  carries only resolve/discover/confirm/terminal step ids) → the escalate
  step is N/A → Tier-2 does NOT flip.
- neighbor: an escalate trace (phase_plan.critical_steps includes the
  escalate step id) → the escalate step IS evaluated.
- negative: a multi-phase trace that legitimately presents BOTH resolve
  and escalate step ids → both evaluated (fix must not over-narrow).
- defensive: an empty / absent phase_plan trace → inert (Tier-2
  PASS/advisory, no flip).

## 3. Item #4 — handover_completeness / case_id_present demotion

**File**: `eval_interactive/eval_interactive/scoring/composite.py` (lines
~69, 81, 84 — `_conditional_mandatory_l2`).

**Change**: remove `handover_completeness` + `case_id_present` from the
`outcome_class == "escalate"` mandatory-L2 path. They become advisory
Tier-3 per the M3-Eval four-tier pyramid — still computed + recorded, but
they no longer flip `case_passed`. Mirror the S-Eval-1 D-2.5 severity
convention (advisory results recorded, not gating).

**Test**: add / update a unit test asserting an escalate case with a
failing `handover_completeness` (or absent `case_id_present`) does NOT
flip `case_passed`.

**STOP if** these turn out to be load-bearing for a Tier-0 safety
invariant (not just a Tier-3 advisory) — demotion must not weaken a
safety floor. (They should be process-completeness checks, not safety
floors, but verify against the CaseSpec `hard_checks` schema first.)

## 4. Governance text (CONDITIONAL — prefer NOT to touch)

If a governance clarification is needed to record that
handover_completeness / case_id_present are Tier-3 advisory, keep it to
1-2 sentences in `docs/current/iteration_governance.md` §5.6 AND flag it
explicitly in the handoff for Codex verification. **PREFER** a handoff
note + leaving the governance text for the deliver-agent to fold in at
milestone close. Do NOT touch §5.5 or §1.7. Do NOT write a governance
rewrite.

## 5. Hard fences (from sprint_objective §5 — DO NOT VIOLATE)

- No `server/src/main/java/**` touch (the #9 fix is eval-side).
- No `server/src/main/resources/skills/*.yaml` touch — do NOT "fix" #9
  by editing `mandatory_for` on the escalate step (that is a Skill-
  contract change with runtime implications; the fix is eval-side
  consumption only).
- No `system_prompt.txt`, `runtime_freeze_and_risk_policy.md`,
  `doc_governance.md`, `agent_context_guide.md` touch.
- No `eval_interactive/case_specs/**` fixture edits (gate-logic fix, not
  fixture).
- No `eval_interactive/case_specs/shadow/` reads.
- No `skill_procedure_check.py:extract()` per-step N/A-by-mandatory_for
  semantics change (add a helper if needed, but the single-step eval is
  unchanged).
- No `sets.py` `_OPT_IN_SETS` / `is_human_judgment_suite` change.
- No `docs/sprints/*` (except NEW sprint-049-handoff.md), `docs/milestones/*`,
  `docs/10-handoff.md`, `docs/action_bank.md`, `docs/codex-findings.md`,
  `docs/milestone_objective.md` edits.

## 6. §4.1 anti-hardcode self-walk

Walk the §4.1 9-question kernel against your S3 commit; capture in
handoff §3. **Expected**: clean `approve`. Key points to articulate:
Q1 — #9 scopes by runtime-emitted `critical_steps[].id` (observable trace
state), not a keyword/regex/per-UC matrix; #4 REMOVES a mandatory gate
(demotion). Q5 — no semantic ownership moves LLM → Java (eval-side gate
correctness fix). If anything reads as a "concern", surface as OQ.

## 7. Tests to run

- **Python (item-specific)**: `cd eval_interactive && uv run pytest
  tests/test_tier2_phase_plan_scoping.py -v` (or the Tier-2 test you
  extend) + the #4 demotion test — all PASS.
- **Python (full suite)**: `cd eval_interactive && uv run pytest --tb=no
  -q` — no NEW failures vs the `7 failed / 441 passed` baseline (dev env;
  the 7 are pre-existing env-specific per OQ-S47.3).
- **Java (full suite)**: `./gradlew :server:test` — `1163 / 1-inherited
  / 0 / 2` unchanged (no server/ touch).
- **Anchor before/after** (REQUIRED; needs backend): per §2 empirical
  confirmation.
- **Bad-case regression-safety**: `eval-interactive run --set bad_cases
  --parallel 1` — distribution qualitatively unchanged (human_review
  authority; the #9 fix should not change closure_criterion judgments).

## 8. Handoff + bundle

- Produce `docs/sprints/sprint-049-handoff.md` per `docs/sprint_objective.md`
  §11. §12 reserved for deliver-agent + human.
- Numbers-cite discipline: every numstat / count / before-after number
  from a reproducible command (`git show --numstat`, `jq`, `pytest`).
- Stage ONLY S3-scope files; no `git add -A`; enumerate in commit msg.
- If you made the conditional §5.6 governance edit, flag it for Codex.

## 9. Self-check (before claiming complete)

- [ ] Read the Tier-2 memo in full first?
- [ ] #9: phase-plan-scoped evaluation implemented; empty-phase_plan
  defensive default → inert (NOT all-Skills fallback)?
- [ ] #9: before/after anchor-suite empirical numbers recorded (misflip
  shape → 0 post-fix)?
- [ ] #9: target / neighbor / negative / defensive tests PASS?
- [ ] #4: handover_completeness + case_id_present removed from escalate
  mandatory-L2; demotion test PASS?
- [ ] §1.7 self-walk clean approve (scopes by observable trace state, not
  hardcode)?
- [ ] Java baseline unchanged; Python no new failures?
- [ ] Hard fences honored (no server/, no Skill YAML, no fixture edits)?
- [ ] Handoff §1-§11 complete; §12 reserved; numbers reproducible?

If any checkbox is unchecked, surface to deliver-agent BEFORE declaring
complete — do not silently ship partial work. This is a `case_passed`-
gate fix; correctness + the before/after evidence are the close gate.
