---
title: Sprint 47 / S-Cleanup-1 — Eval harness polish + R-item closure
doc_tier: current-runtime
status: proposal
implementation_status: not_started
source_of_truth: this file (current sub-sprint contract)
last_reviewed: 2026-05-23
review_cadence: per sub-sprint
supersedes: []
superseded_by: null
notes: >
  First sub-sprint of Milestone M4-Eval-Cleanup (the fourth milestone
  under `iteration_governance.md` §8 framework). Layer: `infra` +
  `eval_spec`. §7 stanza required (eval_spec touch via fixture seeding
  + stale-test path migration). Codex per-sub-sprint trigger NOT
  expected (default: milestone-shared at M4-Eval-Cleanup close per
  `iteration_governance.md` §4.3).

  Six small-scope polish items consumed from the post-M3-Eval cleanup
  audit (verified by deliver-agent 2026-05-23): #1 sets.py + CLI
  registration; #3 default parallel reduction; #6 user_goal_achievement
  fixture seeding; #7 cs_interactive_095 reference verification
  (disputed — confirm or document); #8 test_escalation_enum_sync v0_2
  → v0_3 migration; #10 compact dev-prompt --output cleanup. Two
  R-items closed/partially-closed: stale-test enum-sync CLOSE; parallel-
  session-establishment-flakiness PARTIAL-CLOSE.
---

# Sprint 47 / S-Cleanup-1 — Eval harness polish + R-item closure

## 1. Sprint identity

- **Sprint number**: 47 (global) / S-Cleanup-1 (M4-Eval-Cleanup sub-
  sprint 1; FIRST M4-Eval-Cleanup sub-sprint).
- **Milestone**: M4-Eval-Cleanup (`docs/milestone_objective.md`).
- **Branch**: `refactor/remove-the-shackles` (continuing).
- **HEAD prior to dev**: `7562a2d` (M3-Eval close — A — Clean PASS).
- **Estimated duration**: 1-2 dev days.

## 2. Goal

Land 6 small-scope post-M3-Eval polish items + close 2 R-items. No
governance-touch; lightest cleanup-flavored sub-sprint in M4-Eval-
Cleanup. Establish a clean baseline for S-Cleanup-2 (fixture schema
unification + executor suite-mode) and S-Cleanup-3 (governance
alignment).

## 3. Layer-classification + anti-hardcode stanza (per §7)

**Target failure layer**: `infra` (primary — CLI registration, default
value, compact prompt cleanup, stale-test path migration) + `eval_spec`
(secondary — fixture seeding for `user_goal_achievement`).

**Tier-0 invariant**: This sprint adds no Tier-0 invariant. Existing
Tier-0 invariants in `docs/runtime_freeze_and_risk_policy.md` §1 / §2
unchanged.

**Semantic hardcode**: No semantic hardcode introduced. The CLI
registration of `bad_cases` + `anchor_outcome` as set names is
configuration (a constant tuple), not a semantic decision; the default
parallel value change is an infra parameter; `user_goal_achievement`
fixture seeding is data on existing fixtures (no schema change to the
case_spec YAML format); test path migration is text replacement on a
stale reference; compact prompt cleanup is documentation.

**Generalization coverage**: Not applicable — this sub-sprint adds no
new semantic surface. Acceptance is per-item verification per §6.

## 4. Files in scope

| # | Item | Files | Expected change | Complexity |
|---|------|-------|-----------------|---|
| 1 | sets.py + CLI register `bad_cases` + `anchor_outcome` | `eval_interactive/eval_interactive/batch/sets.py`; possibly `eval_interactive/eval_interactive/cli.py` (if help text or routing requires update) | Add 2 entries to a NEW opt-in registry path; verify CLI `--set bad_cases` resolves to `case_specs/bad_cases/`. Same for `anchor_outcome`. **ENFORCED**: `bad_cases` + `anchor_outcome` MUST be EXCLUDED from `--set all` iteration — these are human-judgment suites (per `iteration_governance.md` §5.6) and accidental inclusion in `--set all` would risk treating their programmatic PASS/FAIL as hard gates. Implementation may be a separate registry tuple (e.g., `_OPT_IN_SETS`) or a flag on `_KNOWN_SETS` entries; both acceptable. | LOW |
| 3 | Default `parallel` 5 → 1 | `eval_interactive/eval_interactive/config.py:83` | One-line change to `parallel: int = 1`. Document the rationale in a one-line code comment. **Runtime-fallback condition**: post-change, measure anchor + smoke runtime vs. pre-change baseline. If anchor or smoke runtime is **>3x slower**, do NOT ship the global default change — instead introduce a per-suite default override (e.g., bad_cases + anchor_outcome default `parallel=1`; anchor/promotion/exploration/smoke retain their prior default). Document the measurement evidence + chosen path in handoff §investigation. | LOW |
| 6 | Seed `user_goal_achievement` on 2-3 production fixtures | 2-3 `eval_interactive/case_specs/anchor/*.yaml` (selected by dev — prefer cases that already cover diverse outcome classes) | Add `user_goal_achievement` to each selected fixture's `llm_judge_dimensions` block per S-Eval-5 advisory schema. NO new schema. | LOW-MEDIUM |
| 7 | Verify cs_interactive_095 references | grep across `eval_interactive/case_specs/`, `docs/`, `compact/`; possibly read `case_families` manifest if present | Either confirm audit dispute (file exists; no orphan) OR find a real orphan reference; document outcome in handoff §investigation. NO file edits unless real orphan. | LOW |
| 8 | Migrate `test_escalation_enum_sync` v0_2 → v0_3 path | `eval_interactive/tests/scoring/test_escalation_enum_sync.py:31` | Replace hardcoded `docs/customer_service_tool_spec_v0_2.yaml` with v0_3 equivalent. If v0_3 is a `.md` (no `.yaml` mirror), update the test to read the markdown schema OR mark the test pending with clear rationale + new R-item. | LOW-MEDIUM |
| 10 | Cleanup `--output` references in compact dev-prompt template | `compact/sprint-deliver-orchestrator.md` (if applicable — grep first); NEW templates only. **DO NOT EDIT** `compact/sprint-NNN-dev-prompt.md` archives (immutable). | Replace `eval-interactive run ... --output /tmp/...` with `eval-interactive run ...` (results land in `eval_interactive/results/{timestamp}/` automatically). Document the correct invocation. | LOW |

## 5. Files NOT in scope (hard fences)

- `server/src/main/java/**` — no runtime code touch.
- `server/src/main/resources/skills/*.yaml` — no Skill YAML edits.
- `server/src/main/resources/system_prompt.txt` — no edits.
- `docs/runtime_freeze_and_risk_policy.md` — no edits.
- `docs/current/iteration_governance.md` — no edits in S1 (S3 may touch
  §5.6 governance text).
- `docs/current/doc_governance.md` — no edits.
- `docs/current/agent_context_guide.md` — no edits.
- `docs/sprints/sprint-NNN-*` — immutable archives.
- `docs/milestones/*` — immutable archives.
- `docs/10-handoff.md` — deliver-agent maintains; no dev edits.
- `docs/action_bank.md` — deliver-agent maintains R-item flips at sub-
  sprint close; no dev edits.
- `docs/codex-findings.md` — review-agent territory; no dev edits.
- `docs/milestone_objective.md` — set at milestone planning; no dev
  edits.
- `eval_interactive/case_specs/bad_cases/*` — S2 + S3 territory; no
  edits in S1 (only reads allowed for #7 verification).
- `eval_interactive/case_specs/shadow/` — held-out, dev-blind.
- `compact/sprint-NNN-dev-prompt.md` archives (immutable; only
  `compact/sprint-deliver-orchestrator.md` template editable, and
  only if it contains a `--output` reference).
- `eval_interactive/eval_interactive/scoring/composite.py` — S3
  territory (handover_completeness demotion).
- `eval_interactive/eval_interactive/scoring/skill_procedure_check.py`
  — S3 territory (Tier-2 design decision).
- `eval_interactive/eval_interactive/batch/executor.py` Tier-2
  wiring (`_compute_tier2_result` lines 365-392) — S3 territory.

## 6. Success metrics

Per-item verification (each item must individually verify before sub-
sprint close):

- **#1**: `eval-interactive run --set bad_cases --parallel 1 --dry-run`
  (or equivalent smoke command) resolves to `case_specs/bad_cases/`.
  Same for `--set anchor_outcome`. Grep `_KNOWN_SETS` confirms both
  entries added.
- **#3**: `grep "parallel" eval_interactive/eval_interactive/config.py`
  confirms default = 1 (with one-line rationale comment).
- **#6**: `grep -r "user_goal_achievement"
  eval_interactive/case_specs/anchor/` returns ≥ 2 matches. The
  selected fixtures still run end-to-end (smoke verification —
  `eval-interactive run --path case_specs/anchor/<selected>`
  succeeds, no schema validation error).
- **#7**: handoff §investigation section documents either confirmation
  of audit dispute (file exists; no orphan) OR finds a real orphan
  reference (document path + nature; surface as deferred R-item if not
  trivially fixable in S1).
- **#8**: `cd eval_interactive && uv run pytest
  tests/scoring/test_escalation_enum_sync.py -v` PASSES. Python
  baseline 5 fail → 4 fail (or fewer).
- **#10**: `grep -r "run --output" compact/` returns 0 matches in
  templates (sprint archives still contain historical refs; that's
  intentional — they're immutable).

Sprint-level:
- **R-item flips** (deliver-agent does the flip at close based on dev
  handoff §11):
  - `R-stale-test-escalation-enum-sync-v0_2-to-v0_3-migration` →
    CLOSED.
  - `R-bad-case-parallel-session-establishment-flakiness` →
    PARTIAL-CLOSED (parallel default reduction; full closure requires
    deeper Tier-2 / session-establishment work).
- **Java baseline unchanged** — `1163 / 1-inherited / 0 / 2` at S1
  close.
- **Python baseline improved** — target 5 fail → 4 fail (depends on
  whether #8 alone fixes the relevant test or other tests also fix).
- **Dev handoff** at `docs/sprints/sprint-047-handoff.md` with
  §1-§11 complete; §12 reserved for deliver-agent + human close
  classification.

## 7. Stop conditions

- If **#1** CLI registration reveals deep coupling (`_KNOWN_SETS` is
  consumed by non-trivial code paths beyond `--set all` iteration),
  dev halts and surfaces to deliver-agent for scope-extension decision
  before continuing.
- If **#3** post-change bad-case suite smoke at `parallel=1` reveals
  NEW failures (cases that previously PASS now FAIL), halt and surface
  — this is a stop-condition per milestone §10.
- If **#6** `user_goal_achievement` fixture seeding requires schema
  changes to the case_spec YAML format (it should NOT — the dim is
  already part of `llm_judge_dimensions` block per M3-Eval), halt.
- If **#7** verification reveals a real orphan reference that's non-
  trivial to fix (e.g., a manifest needs cross-referencing across
  many files), document and surface as deferred R-item; do NOT
  expand S1 scope.
- If **#8** v0_3 spec path doesn't have an analogous yaml structure
  (v0_3 is markdown-only), dev decides between (a) updating the test
  to read markdown enum content, or (b) marking the test pending +
  opening a NEW R-item; surface to deliver-agent if option (b).
- If **#10** `compact/sprint-deliver-orchestrator.md` has NO `--output`
  reference (only sprint archives have it, and those are immutable),
  document the finding in handoff §investigation; #10 becomes a no-op.

## 8. Codex review plan

Per `iteration_governance.md` §4.3 default: **deferred to M4-Eval-
Cleanup milestone close**. No per-sub-sprint Codex trigger expected
(this is infra + char-test + R-item closure; no Tier-0 candidate; no
§1.7 cross; no hard-fence violation; no fix-iteration on prior sub-
sprint).

## 9. Generalization coverage stanza

Not applicable — see §3 stanza. This sub-sprint adds no new semantic
surface; per-item verification (§6) is the acceptance approach.

## 10. Bundle policy

- Dev stages ONLY S1-scope files in the dev commit (per `iteration_
  governance.md` §8.7 + the 2026-05-17 governance update on commit-
  at-end bundling).
- Deliver-agent files (`docs/milestone_objective.md`,
  `docs/sprint_objective.md`, `compact/sprint-047-dev-prompt.md`) are
  bundled by the human at the launch commit (NOT staged by dev).
- Dev MUST NOT `git add -A`; staged files MUST be enumerated
  explicitly.
- Dev MUST NOT touch any file under §5 (hard fences).

## 11. Handoff schema

Dev produces `docs/sprints/sprint-047-handoff.md` with these sections:

1. **Identity** — sub-sprint name (Sprint 47 / S-Cleanup-1), dev
   commit SHA, branch, HEAD before / after.
2. **Scope landed** — per §4 table, actual files + numstat (`git show
   --numstat <commit>`) per the `feedback_deliver_agent_cited_numbers
   _must_be_reproducible.md` discipline.
3. **§3 layer-classification + anti-hardcode self-walk** — re-state
   the stanza with delivered evidence (the §3 stanza claims must hold
   for the actual commit).
4. **Java baseline** — must be unchanged `1163 / 1-inherited / 0 / 2`.
5. **Python baseline** — target 5 fail → 4 fail (or fewer). Cite the
   specific tests that flipped.
6. **Per-item verification evidence** — per §6 (the §6 acceptance bar
   per item; cite the grep / test / smoke command output).
7. **#7 investigation** — document the cs_interactive_095 dispute
   resolution.
8. **OQs surfaced** — questions for deliver-agent + human disposition
   (deliver-agent + human decide at close).
9. **Drift items** — line counts vs estimate, test count vs estimate,
   etc.
10. **Hard fence honored checklist** — per §5 file-NOT-in-scope list;
    confirm zero touches.
11. **R-item flip request** — name the R-items + flip direction
    (deliver-agent does the actual flip at close).
12. **Closure verdict deferred** — template: A — Clean PASS / B fix-
    iteration / C in-flight downgrade / out-of-scope-review.

## 12. Closure verdict (deferred to sub-sprint close)

To be appended at S-Cleanup-1 close by deliver-agent + human jointly.
Reserved per existing convention.
