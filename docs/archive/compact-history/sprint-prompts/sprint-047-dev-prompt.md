# Sprint 47 / S-Cleanup-1 — Dev Implementation Prompt

You are the dev agent for Sprint 47 / S-Cleanup-1, the first sub-sprint
of Milestone M4-Eval-Cleanup. This is a cleanup-flavored sub-sprint —
6 small-scope polish items + 2 R-item closures. **Read this prompt +
the contracts listed in §1 before writing any code.**

## 1. Read order (cold start)

1. `AGENTS.md` (auto-loaded via constitution chain — `doc_governance.md`,
   `agent_context_guide.md`, `iteration_governance.md`). Do not re-read
   if already in context.
2. `docs/milestone_objective.md` — M4-Eval-Cleanup milestone contract
   (read §3 for sub-sprint sequence; §5 for milestone-level
   acceptance bar; §6 for milestone-level hard fences; §10 for
   stop conditions).
3. `docs/sprint_objective.md` — S-Cleanup-1 contract (this is your
   primary contract; everything below is implementation guidance).
4. `docs/10-handoff.md` §1 (current phase narrative — confirms M3-Eval
   close baselines).

**Do NOT re-read** sprint archives (`docs/sprints/sprint-NNN-*`) or
milestone archives (`docs/milestones/M3-Eval_*`) unless you need a
specific reference for context; the contracts above carry the active
state.

## 2. Items to land (6 + 1 verify)

Detailed per-item implementation guidance. Read `docs/sprint_objective.md`
§4 for the canonical file table.

### Item #1 — sets.py + CLI register `bad_cases` + `anchor_outcome`

**File**: `eval_interactive/eval_interactive/batch/sets.py`.

**Change**: Extend `_KNOWN_SETS` from `("anchor", "promotion",
"exploration", "smoke")` to include `"bad_cases"` and
`"anchor_outcome"`. Verify CLI `--set bad_cases` correctly resolves to
`case_specs/bad_cases/` directory (the set name to directory mapping
should follow the existing convention — check how `anchor` /
`promotion` etc. resolve).

**Verification**: After change, `eval-interactive run --set bad_cases
--parallel 1 --dry-run` (or whatever smoke flag is available; if no
--dry-run flag exists, document and the smoke is the actual launch but
abort early) should resolve to bad_cases directory without error.
Same for `--set anchor_outcome`.

**CLI follow-on (ENFORCED per human approval 2026-05-23)**:
`bad_cases` + `anchor_outcome` MUST be **EXCLUDED** from `--set all`
iteration. These are human-judgment suites per `iteration_governance.md`
§5.6 — accidental inclusion in `--set all` would risk treating their
programmatic PASS/FAIL output as a hard gate. Implementation choice
(separate `_OPT_IN_SETS` tuple, or a per-entry flag on `_KNOWN_SETS`)
is dev's discretion; document the choice in the handoff. The CLI help
text for `--set` should make the opt-in nature explicit (e.g., "bad_cases,
anchor_outcome are opt-in human-judgment suites; not included in
--set all").

### Item #3 — Default parallel 5 → 1

**File**: `eval_interactive/eval_interactive/config.py:83`.

**Change**: `parallel: int = 5` → `parallel: int = 1`. Add a one-line
inline comment explaining the rationale ("parallel=1 default per
M3-Eval close evidence; bad-case suite stability requires sequential
runs; opt-in to higher parallel via CLI flag").

**Verification**: `grep "parallel" eval_interactive/eval_interactive/
config.py` confirms default = 1.

**Side-effect awareness + runtime fallback (per human approval
2026-05-23)**: this changes the default for ALL eval suites, not just
bad_cases. Measure anchor + smoke runtime vs. pre-change baseline as
part of S1.

- **If anchor or smoke runtime is < 3x slower** → ship the global
  default change as written.
- **If anchor or smoke runtime is ≥ 3x slower** → do NOT ship the
  global default; instead introduce a per-suite default override
  (e.g., bad_cases + anchor_outcome default `parallel=1`; anchor /
  promotion / exploration / smoke retain their prior default of 5).
  This fallback must land in S1, NOT deferred to S2.

Document the measured runtime delta (pre- vs post- on a representative
sample run — e.g., one anchor case + one smoke case) and the chosen
path in handoff §investigation. The decision is dev's based on
measured evidence; surface to deliver-agent only if measurement is
inconclusive or the fallback implementation is non-trivial.

### Item #6 — Seed `user_goal_achievement` on 2-3 production fixtures

**Files**: 2-3 selected `eval_interactive/case_specs/anchor/*.yaml`.
Selection criteria: prefer cases that cover diverse outcome classes
(resolve / escalate / decline / defer) to establish trend signal across
outcome shapes.

**Change**: Add `user_goal_achievement` to each selected fixture's
`llm_judge_dimensions` block per S-Eval-5 advisory schema. Read 1-2
M3-Eval bad cases (`eval_interactive/case_specs/bad_cases/*.yaml`)
that already wire L3 dims for reference shape — do NOT copy
verbatim; the seeded values must reflect each fixture's actual
expected user-goal-achievement state.

**NO schema change** to the case_spec YAML format — `user_goal_achievement`
is already a recognized advisory L3 dim per M3-Eval close (S-Eval-5
landing). If schema validation rejects the seeded dim, halt and
surface — likely indicates the dim isn't fully wired and S1 isn't the
right sub-sprint to seed it.

**Verification**: `grep -r "user_goal_achievement"
eval_interactive/case_specs/anchor/` returns ≥ 2 matches; selected
fixtures still run end-to-end via `eval-interactive run --path
case_specs/anchor/<selected>` without schema validation error.

### Item #7 — Verify cs_interactive_095 references (DISPUTED by audit)

**No code change unless real orphan found**. The audit claimed
cs_interactive_095 was deleted but still referenced; deliver-agent
verification found the file still exists at
`eval_interactive/case_specs/anchor/cs_interactive_095.yaml`.

**Action**: Grep `cs_interactive_095` across the repo:

```bash
grep -rn "cs_interactive_095" /Users/caoruixin/projects/csagent-latest \
  --include="*.yaml" --include="*.md" --include="*.py" \
  --exclude-dir=".git" --exclude-dir="node_modules"
```

Cross-reference each match against the actual file's existence. If a
manifest or report references the file and the file IS present,
audit claim is disputed (document in handoff §investigation). If a
manifest references a file that genuinely doesn't exist (e.g., a
different `cs_interactive_NNN` case), document the orphan + surface
as deferred R-item or fix if trivial (1-line manifest edit).

**Hard fence**: do NOT delete `cs_interactive_095.yaml` itself; the
file exists for a reason (likely a regression guard or anchor case).

### Item #8 — Migrate `test_escalation_enum_sync` v0_2 → v0_3 path

**File**: `eval_interactive/tests/scoring/test_escalation_enum_sync.py:31`.

**Change**: Replace hardcoded `docs/customer_service_tool_spec_v0_2.yaml`
path with the v0_3 equivalent. The v0_3 spec lives at
`docs/current/customer_service_tool_spec_v0_3.md` (markdown, not yaml).

**Branch decision**:
- **If v0_3 has an analogous yaml mirror**: update the path string.
- **If v0_3 is markdown-only and the test reads structured enum
  content**: rewrite the test to parse markdown enum content (likely
  a fenced code block or YAML front-matter) OR mark the test
  `pytest.skip("v0_3 enum format not yet structured for direct yaml
  parse")` and open a NEW R-item via the handoff §8 OQ surface.

**Verification**: `cd eval_interactive && uv run pytest
tests/scoring/test_escalation_enum_sync.py -v` PASSES (or SKIPs with
explicit rationale).

### Item #10 — Cleanup `--output` references in compact dev-prompt template

**Files in scope**: `compact/sprint-deliver-orchestrator.md` (IF it
contains a `--output` reference — grep first).

**Files NOT in scope**: `compact/sprint-NNN-dev-prompt.md` archives
(immutable per `doc_governance.md`). The audit found a reference in
`compact/sprint-046-dev-prompt.md` lines 92-100 — this is a sprint
archive; do NOT edit.

**Action**:
```bash
grep -n "eval-interactive run.*--output" compact/sprint-deliver-orchestrator.md
```

If matches found, replace the invocation per the correct CLI surface
— the `run` command does NOT accept `--output`; results auto-land in
`eval_interactive/results/{timestamp}/results.json` +
`eval_interactive/results/{timestamp}/report.html`. The correct
invocation is `eval-interactive run --path <path> --label <label>
--parallel 1` (no `--output`).

If `compact/sprint-deliver-orchestrator.md` has NO matches, document
the finding in handoff §investigation; #10 is a no-op.

## 3. Hard fences (from sprint_objective §5 — DO NOT VIOLATE)

- No `server/src/main/java/**` runtime code touch.
- No `server/src/main/resources/skills/*.yaml` touch.
- No `server/src/main/resources/system_prompt.txt` touch.
- No `docs/runtime_freeze_and_risk_policy.md` touch.
- No `docs/current/iteration_governance.md` touch (S3 territory).
- No `docs/current/doc_governance.md` or `agent_context_guide.md`
  touch.
- No `docs/sprints/sprint-NNN-*` touch (immutable archives).
- No `docs/milestones/*` touch.
- No `docs/10-handoff.md` touch.
- No `docs/action_bank.md` touch.
- No `docs/codex-findings.md` touch.
- No `docs/milestone_objective.md` touch.
- No `eval_interactive/case_specs/bad_cases/*` touch (S2/S3
  territory; only reads for #7 verification allowed).
- No `eval_interactive/case_specs/shadow/` reads.
- No `eval_interactive/eval_interactive/scoring/composite.py` touch
  (S3 territory).
- No `eval_interactive/eval_interactive/scoring/skill_procedure_check.py`
  touch (S3 territory).
- No `eval_interactive/eval_interactive/batch/executor.py:365-392`
  (`_compute_tier2_result`) touch (S3 territory).
- No `compact/sprint-NNN-dev-prompt.md` archive edits.

## 4. §4.1 anti-hardcode self-walk

Before producing the handoff, walk the §4.1 9-question kernel from
`iteration_governance.md` against your S1 commit and capture the
result in handoff §3. **Expected outcome**: clean `approve` per
Q1-Q9 because S1 is infra + char-test + R-item closure (no semantic
hardcode introduced). If any question's answer is non-trivially
"yes" or "concern", surface as OQ in handoff §8.

## 5. Tests to run

- **Python (item-specific)**: `cd eval_interactive && uv run pytest
  tests/scoring/test_escalation_enum_sync.py -v` — must PASS post-#8
  (or SKIP with rationale).
- **Python (full suite)**: `cd eval_interactive && uv run pytest` —
  baseline 5 fail → 4 fail (or fewer). Capture the full result for
  handoff §5.
- **Java (full suite)**: `./gradlew :server:test` — baseline `1163 /
  1-inherited / 0 / 2` must remain unchanged.
- **Eval harness smoke (OPTIONAL — not blocking S1 close)**:
  - `cd eval_interactive && uv run eval-interactive run --set
    bad_cases --parallel 1 --label s-cleanup-1-smoke` to confirm
    #1 CLI registration works end-to-end. If a dry-run flag exists,
    prefer that to avoid burning LLM tokens; otherwise abort early
    after CLI resolves the set path.

## 6. Handoff requirements

Produce `docs/sprints/sprint-047-handoff.md` per `docs/sprint_objective.md`
§11 schema. Section 12 (closure verdict) reserved for deliver-agent
+ human.

**Numbers-cite discipline** (per `feedback_deliver_agent_cited_numbers
_must_be_reproducible.md`): every numstat / line-count / test-count
number in the handoff MUST be derived from a reproducible command
(`git show --numstat <commit>`, `grep -c`, `wc -l`, `pytest
--tb=no`). Document the command alongside the number in handoff §2 /
§4 / §5.

## 7. Bundle policy (per `iteration_governance.md` §8.7)

- Stage ONLY S1-scope files. Run `git status` before committing;
  enumerate staged files in the commit message.
- Do NOT `git add -A` or `git add .`.
- Deliver-agent files (`docs/milestone_objective.md`,
  `docs/sprint_objective.md`, `compact/sprint-047-dev-prompt.md`) are
  NOT staged by you — the human + deliver-agent bundle those at the
  launch commit.
- Dev commit message format (per existing convention): summary line
  + per-item breakdown + R-item flip request + baseline citation.

## 8. Self-check (before claiming sub-sprint complete)

- [ ] All 6 items landed per §2 verification?
- [ ] #7 investigation documented (regardless of outcome)?
- [ ] Python `test_escalation_enum_sync` PASSES (or SKIPs with
  rationale + new R-item surfaced)?
- [ ] Python baseline improved (5 fail → 4 fail or fewer)?
- [ ] Java baseline unchanged (`1163 / 1-inherited / 0 / 2`)?
- [ ] Hard fences honored (zero touches under §3)?
- [ ] Handoff sections 1-11 complete; §12 reserved?
- [ ] Numbers-cite discipline followed (every quantitative claim has
  a reproducible command)?
- [ ] Bundle stages only S1-scope files; no deliver-agent files
  staged?
- [ ] §4 anti-hardcode self-walk produces clean `approve`?

If any checkbox is unchecked, surface to deliver-agent BEFORE
declaring sub-sprint complete — do not silently ship partial work.
