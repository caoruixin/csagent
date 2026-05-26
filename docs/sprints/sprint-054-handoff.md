---
title: Sprint 54 / M-Auto-1A S-Auto-1 — Mutable-surface contract + YAML diff sandbox — dev handoff
doc_tier: sprint-archive
status: current
implementation_status: implemented
source_of_truth: this file (sub-sprint dev archive); autoloop/program.md + autoloop/sandbox/yaml_diff_validator.py + autoloop/tests/ (code)
last_reviewed: 2026-05-27
review_cadence: ad hoc
supersedes: []
superseded_by: null
notes: >
  First sub-sprint of Milestone M-Auto-1A — Skill-driven auto-evolution
  v1 scaffold. S-Auto-1 ships the `autoloop/` subsystem directory tree,
  the LOCKED human-readable contract at `autoloop/program.md`, the
  YAML-diff sandbox `autoloop/sandbox/yaml_diff_validator.py`, a
  cross-file-safe `applier.py` skeleton (S-Auto-3 fills git logic), a
  CLI router (`check` works; `dry-run`/`run`/`report`/`apply`/`audit`
  are placeholders), and full pytest coverage (48 tests pass). Zero
  edits to `server/`, `eval/`, `eval_interactive/`, `data/`, `db/`,
  `docs/foundational/`, `docs/runtime_freeze_and_risk_policy.md`,
  `docs/current/`, or any sprint/milestone archive. eval_interactive
  baseline reproduces `486 passed, 3 failed`. Java suite skipped per
  §7 stanza Java-zero-touch exemption (verified via
  `git diff --stat HEAD -- server/ eval/` returning empty). §12
  reserved for deliver-agent + human at milestone close (M-Auto-1A
  uses milestone-shared Codex per §4.3; no per-sub-sprint Codex
  trigger fires for S-Auto-1).
---

# Sprint 54 / M-Auto-1A S-Auto-1 — Mutable-surface contract + YAML diff sandbox dev handoff

## 1. Goal and outcome

**Goal (from `docs/sprint_objective.md` / `compact/sprint-054-dev-prompt.md`):**
Build the `autoloop/` subsystem skeleton, write the human-readable
`program.md` contract that locks the M-Auto-1A mutable surface, and
implement the YAML-diff sandbox that structurally rejects any
meta-agent proposal outside the 6 Skill YAMLs × 4 LLM-soft fields
allowlist. Cover with pytest. No live LLM calls; no subprocess
invocation of `mvn` / `spring-boot` / `eval-interactive`; no writes
to any Skill YAML.

**Outcome:** DELIVERED.

- `autoloop/` package created (pyproject.toml + README.md + program.md
  + config.yaml + autoloop/{__init__,__main__,cli}.py +
  autoloop/sandbox/{__init__,yaml_diff_validator,applier}.py).
- `autoloop/program.md` v1 LOCKED (8 sections + §3 verbatim quote of
  both milestone-level fences and proposal §8 fences; §5 §1.7
  mapping from proposal Appendix D).
- `autoloop/sandbox/yaml_diff_validator.py` implements the full
  `validate_skill_yaml_diff` API: file-path whitelist check, anchor
  / alias rejection (via PyYAML event stream — false-positive-safe),
  before/after parse with separate error reasons, recursive AST
  diff with `$.foo[i].bar` path notation, `[*]`-wildcard whitelist
  matcher, empty-diff rejection.
- `autoloop/sandbox/applier.py` is a documented skeleton that names
  the cross-file (§3.A fence #9) caller invariant for S-Auto-3.
- `autoloop/cli.py` exposes `check` / `dry-run` / `run` / `report` /
  `apply` / `audit` subcommands via `argparse`. `check` validates
  config + self-tests one positive + one negative diff. Placeholder
  subcommands exit 1 with "S-Auto-3 territory" so a CI invocation
  surfaces as failure rather than silent no-op.
- Test count: **48 collected, 48 passed** (~3× the prompt's "20-30"
  target — coverage is intentionally broad because the sandbox is the
  long-term §1.7 structural anchor for the M-Auto-1A loop).
- Hard fences honoured (see §4 below).
- eval_interactive baseline reproduces `486 passed, 3 failed` (see §6).

## 2. Scope (per `compact/sprint-054-dev-prompt.md` §#1-#6)

### #1 — `autoloop/` directory tree — DELIVERED

```
autoloop/
  .gitignore                          # local venv / pycache / results
  pyproject.toml                      # PyYAML>=6.0 + pytest dev dep
  README.md                           # one-page synopsis + CLI table
  program.md                          # 16K, LOCKED contract (§2-§8)
  config.yaml                         # mutable_surface allowlists + paths + lessons.k=10
  autoloop/
    __init__.py
    __main__.py                       # `python -m autoloop` entry
    cli.py                            # argparse subcommand router
    sandbox/
      __init__.py                     # re-exports validate_skill_yaml_diff + ValidationResult
      yaml_diff_validator.py          # main deliverable
      applier.py                      # skeleton + cross-file contract docstring
  tests/
    __init__.py
    test_yaml_diff_validator.py       # 38 tests (param + direct API + edge cases)
    test_cli_smoke.py                 # 10 tests (subcommand --help + check + placeholders)
    fixtures/
      valid_diffs/                    # 4 positive
        procedure_edit.diff
        grounding_instruction_edit.diff
        escalation_policy_edit.diff
        critical_steps_desc_edit.diff
      invalid_diffs/                  # 19 negative (>= prompt's 12 minimum)
        structural_field_applicable_use_cases.diff
        structural_field_tools_required.diff
        trace_check_edit.diff
        mandatory_for_edit.diff
        severity_edit.diff
        critical_step_id_edit.diff
        guardrails_edit.diff
        state_inheritance_edit.diff
        cross_skill_edit.diff
        cross_file_edit_skill_plus_config.diff
        new_skill_file.diff
        deleted_skill_field.diff
        malformed_yaml.diff
        required_context_keys_edit.diff
        new_critical_step.diff
        deleted_critical_step.diff
        reorder_critical_steps.diff
        yaml_anchor_introduced.diff
        comment_only_diff.diff
        smuggle_trace_check_under_procedure_edit.diff
```

### #2 — `autoloop/program.md` 8-section contract — DELIVERED

All 8 sections present:

1. Purpose — auto-loop is Skill-driven hill-climbing meta-agent;
   kept iteration = CANDIDATE, not a merge.
2. Mutable surface (verbatim) — 6 Skill YAMLs × 4 LLM-soft fields;
   every other byte locked.
3. Hard fences (verbatim copy):
   - §3.A — 12 milestone-level fences from
     `docs/milestone_objective.md` §6.
   - §3.B — 8 proposal-level fences from
     `docs/solutions/auto_evolution_skill_driven_v1.md` §8.
4. Forbidden by construction — 6-row table: sandbox, lexicographic
   gate, anti-hardcode check, shadow regression gate, shadow result
   firewall, no-main-branch-cherry-pick. Each row names where the
   defense lives + which sub-sprint ships it.
5. §1.7 forbidden-list mapping — verbatim from proposal Appendix D
   (5 forbidden items × defense rows).
6. What the loop is NOT allowed to do — restated in auditor voice
   (tools_required / trace_check / case_spec / Java / bundled diffs
   / direct main write / tool name renames).
7. What the human gives up — convergence / never-forbidden /
   candidate-not-merge.
8. Versioning — v1 LOCKED during M-Auto-1A; v2 requires new milestone
   + new program.md + human authorization + Codex review.

Plus an Appendix with source-of-truth pointers.

### #3 — `validate_skill_yaml_diff` — DELIVERED

API as specified:

```python
@dataclass
class ValidationResult:
    decision: Literal["ACCEPT", "REJECT"]
    reason: str
    rejected_paths: list[str]
    accepted_paths: list[str]
    file_path: str

def validate_skill_yaml_diff(
    before_yaml: str, after_yaml: str, file_path: str,
    *,
    allowed_skill_files: list[str] | None = None,
    allowed_field_paths: list[str] | None = None,
) -> ValidationResult: ...
```

Algorithm steps 1-6 implemented:

1. File-path check vs `config.mutable_surface.allowed_skill_files`
   (literal-string match on repo-relative paths; single leading
   `./` stripped via `_normalize_file_path`).
2. Anchor / alias rejection via `yaml.parse()` event stream —
   `AliasEvent` or any event with `anchor` attribute set → reject.
   (Implemented this way to avoid false positives on natural-language
   `&` / `*` characters inside `procedure` text, e.g. "AT&T". The
   PyYAML event stream surfaces anchor / alias tokens at the parser
   layer regardless of surrounding text.)
3. YAML parse via `yaml.safe_load`. After-side failure →
   `malformed YAML in proposed edit`. Before-side failure →
   `baseline YAML is malformed — refusing to compare`.
4. Recursive AST diff (`_diff_paths`): dict-keyed `$.foo`; list-
   indexed `$.xs[i]`; type mismatch counts as one path change at the
   current prefix; list length change reports per-index extras.
5. Whitelist match: each changed path matched against each pattern
   via `_pattern_to_regex` — `[*]` → `\[\d+\]`, everything else
   literal-escaped. Any path matching no pattern → reject (with
   the rejected paths listed in `reason` and in `rejected_paths`).
6. Empty diff (zero changed paths after parsing) → reject with
   "no whitelisted field changed" — guards comment-only /
   whitespace-only / functionally-identical proposals.

Edge cases handled and tested:

- **Whitespace-only or comment-only diff** — `safe_load` strips,
  AST equal, empty diff → REJECT (`comment_only_diff.diff` fixture +
  `test_byte_difference_with_same_ast_rejected_as_empty_diff`
  direct test).
- **New `critical_steps` entry** — list length grows by 1, the new
  index reports as `$.critical_steps[N]` which fails the wildcard
  `$.critical_steps[*].desc` match (the wildcard requires `.desc`
  suffix) → REJECT (`new_critical_step.diff` fixture).
- **Deleted `critical_steps` entry** — symmetric to above
  (`deleted_critical_step.diff`).
- **Reordered list** — per-index scalar diff fires;
  `$.critical_steps[0].id` etc. all report changes → REJECT
  (`reorder_critical_steps.diff`).
- **YAML anchor introduction** — `&p` / `*p` in `after_yaml` caught
  at the event-stream layer regardless of which field carries the
  anchor (`yaml_anchor_introduced.diff`).
- **Adversarial: clean-looking procedure edit smuggling a
  `trace_check` change** — AST diff sees both paths; the
  `trace_check` path fails the whitelist → REJECT
  (`smuggle_trace_check_under_procedure_edit.diff` fixture +
  `test_procedure_edit_with_smuggled_mandatory_for_change_rejected`
  direct test).

**Multi-file diff handling:** the validator is per-file by API. The
caller layer (S-Auto-3 `applier.py`) is contracted to short-circuit
REJECT any propose bundle containing more than one file BEFORE
invoking the validator. `applier.py` documents this in a docstring;
the validator does not enforce cross-file by itself (it cannot — it
sees one file at a time). The `test_cross_file_caller_rejects` test
simulates a hypothetical loop-based caller doing per-file
invocation and verifies that at least one of the two invocations
REJECTs an out-of-surface file — defense-in-depth confirmation.

### #4 — `autoloop/cli.py` subcommand router — DELIVERED

| Subcommand | S-Auto-1 status | Notes |
|---|---|---|
| `check` | implemented | Validates config, verifies the 6 Skill YAMLs exist on disk, self-tests sandbox on canned positive + negative. Exit 0 on success, 1 on any failure. |
| `dry-run` | placeholder | Exits 1, prints "S-Auto-3 territory". |
| `run` | placeholder | Same as dry-run. |
| `report` | placeholder | Same. |
| `apply --experiment <id>` | placeholder | Same; `--experiment` arg accepted but unused. |
| `audit --experiment <id>` | placeholder | Same. |

Stdlib `argparse`; no external CLI library introduced (avoids extra
dep per #6 / hard fence). Entry point `autoloop = "autoloop.cli:main"`
declared in `pyproject.toml [project.scripts]`. `python -m autoloop`
also works via `autoloop/__main__.py`. All `--help` outputs exit 0.

### #5 — Tests — DELIVERED

Counts:

- `tests/test_yaml_diff_validator.py`: 38 tests.
  - 4 parametrized positive-fixture tests (one per allowed field
    class).
  - 19 parametrized negative-fixture tests.
  - 2 fixture-count guards (assert >= 4 positive / >= 12 negative;
    catch fixture regression).
  - 8 direct API tests on `_diff_paths` (empty, scalar, added,
    deleted, list-scalar, list-length).
  - 2 direct API tests on `_pattern_to_regex` (wildcard + no-
    wildcard).
  - 1 byte-difference / same-AST empty-diff test.
  - 1 adversarial smuggle test (`procedure` edit + smuggled
    `mandatory_for` change).
  - 1 cross-file caller-layer test.
  - 1 ValidationResult shape test (file_path echo + accepted_paths
    populated on ACCEPT).
- `tests/test_cli_smoke.py`: 10 tests.
  - 6 parametrized subcommand `--help` exit-0 tests.
  - 1 `check` against shipped config exit 0.
  - 1 `check` against a temp config naming a non-existent Skill
    YAML exit 1 (uses a temp symlink to the real `server/` tree so
    the repo-root resolution still finds the real files; only the
    bogus path is the failure target).
  - 1 top-level `--help` lists all 6 subcommands.
  - 1 placeholder subcommand exits 1 (catches accidental no-op).

**Total: 48 tests, 48 pass.** Runtime: 0.49s.

Reproduce:

```
cd autoloop
uv pip install -e . pytest
uv run pytest -q
```

### #6 — Documentation closeout — DELIVERED

- `autoloop/README.md`: one-page synopsis (3-sentence "what is
  auto-loop" + CLI subcommand table + pointer to `program.md` +
  pointer to `docs/milestone_objective.md` + pointer to
  `docs/solutions/auto_evolution_skill_driven_v1.md` + install
  block).
- `pyproject.toml`: `name = "csagent-autoloop"`, `version = "0.1.0"`,
  `requires-python = ">=3.11"`, `dependencies = ["PyYAML>=6.0"]`,
  `[project.optional-dependencies].dev = ["pytest>=8.0"]`,
  `[project.scripts] autoloop = "autoloop.cli:main"`. No additional
  dependencies introduced.

## 3. Hard fences honoured

| Fence | Evidence |
|---|---|
| No edits to `server/**` | `git diff --stat HEAD -- server/` returns empty. |
| No edits to `eval/**` / `eval_interactive/**` / `data/**` / `db/**` | `git diff --stat HEAD -- eval/ eval_interactive/ data/ db/` returns empty. |
| No edits to `docs/foundational/` / `docs/runtime_freeze_and_risk_policy.md` / `docs/current/` (incl. iteration_governance) | `git diff --stat HEAD -- docs/foundational/ docs/runtime_freeze_and_risk_policy.md docs/current/` returns empty. |
| No edits to sprint or milestone archives | `git diff --stat HEAD -- docs/sprints/ docs/milestones/` returns empty (this handoff is a NEW file, not an edit). |
| No edits to any Skill YAML (`server/src/main/resources/{skills,prompts,scripts,config,mock}/**`) | Same `server/` empty diff above; sandbox VALIDATES diffs against Skill YAMLs but never writes them. |
| No edits to `docs/codex-findings.md` | `git diff --stat HEAD -- docs/codex-findings.md` returns empty. |
| No new heavy dependencies | `pyproject.toml` declares PyYAML + pytest only — exactly as authorized by the dev prompt. |
| No live LLM call | None; sandbox is pure Python + PyYAML. |
| No subprocess invocation of `mvn` / `spring-boot` / `eval-interactive` / `uv run` from S-Auto-1 code | The CLI smoke tests invoke `python -m autoloop ...` via `subprocess.run` (this is autoloop's own CLI, not an external runtime), but autoloop's own source code does not shell out. |
| No `git add -A` | Stage list (see §11) is explicit. |
| STOP-and-surface on YAML feature ambiguity | None encountered (verified production Skill YAMLs use no anchors / refs / merge keys via `grep -nE '(^[[:space:]]*[&*][a-zA-Z_]+|<<:)' server/src/main/resources/skills/*.yaml` — exit 1, no matches). |
| STOP-and-surface on whitelist-pattern ambiguity | None encountered (the 4 patterns are unambiguous — no Skill YAML has nested `procedure` or `grounding_instruction` keys; `critical_steps[*].desc` matches exactly the per-step `desc` field). |

## 4. §7 Layer-classification + anti-hardcode self-walk

**Target failure layer:** `infra` (§3.2 default — pure infrastructure;
no semantic decision changed; no projection slot changed; no scoring
weight changed). S-Auto-1 is the structural anchor for the §1.7
forbidden-list enforcement later in the milestone; it itself does not
make any semantic decision.

The §7 stanza is REQUIRED per the dev prompt (the prompt notes:
"§7 REQUIRED — S-Auto-1 defines auto-evolution capability's
mutable-surface CONTRACT, is the long-term anchor for subsequent §1.7
structural enforcement; per §7 even pure-infra work that is the
*basis* of subsequent semantic work must carry the stanza so the
contract is auditable.") Even though the work is `infra`, the stanza
follows.

**Tier-0 invariant:** This sub-sprint adds no Tier-0 invariant. The
sandbox enforces the M-Auto-1A mutable-surface contract, which is a
milestone-scoped boundary — not a Tier-0 runtime invariant per
`docs/runtime_freeze_and_risk_policy.md` §1 / §2. C2 / C3 DEFER
remains per the M2-close verdict.

**Semantic hardcode:** No semantic hardcode introduced. The sandbox
whitelist matching is regex-free at the data layer — `_diff_paths`
walks Python `dict` / `list` / scalar via `isinstance` checks; the
whitelist patterns use a tiny custom matcher (`_pattern_to_regex`)
that converts the wildcard `[*]` to `\[\d+\]` and escapes the rest
literally. The whitelist itself is a small literal set in
`config.yaml`: 4 YAML field-paths × 6 Skill-file paths. It is a
registry / contract (the human-locked mutable surface from proposal
§3.1), not a decision rule. No regex is applied to YAML content
itself.

**Generalization coverage:**

- **Target:** sandbox correctly ACCEPTs the 4 positive fixtures (one
  per allowed field class) and REJECTs all 19 negative fixtures
  (structural-field × 3 cases / critical_step element × 5 cases /
  guardrails / state_inheritance / required_context_keys / 3
  out-of-surface file paths / malformed YAML / 5 edge cases / 1
  adversarial smuggle).
- **Neighbor:** sandbox correctly handles edge cases — anchor
  introduction, list reorder, critical_step add / delete,
  comment-only.
- **Negative (false-positive guard):** the 4 positive fixtures are
  the negative controls — a sandbox that REJECTed any of them
  would be over-restrictive. All 4 ACCEPT.
- **Adversarial:** the
  `smuggle_trace_check_under_procedure_edit.diff` fixture +
  `test_procedure_edit_with_smuggled_mandatory_for_change_rejected`
  direct test verify that a propose with a visually clean
  `procedure` edit but smuggling a `trace_check` /
  `mandatory_for` change is REJECTed because the AST diff sees
  every changed path.
- **Shadow:** N/A for S-Auto-1. Shadow gates land in S-Auto-2
  (`tier_evaluator.py`), not the sandbox.

## 5. Files touched + footprint

```
$ cd autoloop && find . -type f -not -path '*/\.venv/*' \
    -not -path '*/__pycache__/*' \
    -not -path '*/csagent_autoloop.egg-info/*' \
    -not -path '*/\.pytest_cache/*' | sort
./.gitignore
./README.md
./autoloop/__init__.py
./autoloop/__main__.py
./autoloop/cli.py
./autoloop/sandbox/__init__.py
./autoloop/sandbox/applier.py
./autoloop/sandbox/yaml_diff_validator.py
./config.yaml
./program.md
./pyproject.toml
./tests/__init__.py
./tests/fixtures/invalid_diffs/comment_only_diff.diff
./tests/fixtures/invalid_diffs/critical_step_id_edit.diff
./tests/fixtures/invalid_diffs/cross_file_edit_skill_plus_config.diff
./tests/fixtures/invalid_diffs/cross_skill_edit.diff
./tests/fixtures/invalid_diffs/deleted_critical_step.diff
./tests/fixtures/invalid_diffs/deleted_skill_field.diff
./tests/fixtures/invalid_diffs/guardrails_edit.diff
./tests/fixtures/invalid_diffs/malformed_yaml.diff
./tests/fixtures/invalid_diffs/mandatory_for_edit.diff
./tests/fixtures/invalid_diffs/new_critical_step.diff
./tests/fixtures/invalid_diffs/new_skill_file.diff
./tests/fixtures/invalid_diffs/reorder_critical_steps.diff
./tests/fixtures/invalid_diffs/required_context_keys_edit.diff
./tests/fixtures/invalid_diffs/severity_edit.diff
./tests/fixtures/invalid_diffs/smuggle_trace_check_under_procedure_edit.diff
./tests/fixtures/invalid_diffs/state_inheritance_edit.diff
./tests/fixtures/invalid_diffs/structural_field_applicable_use_cases.diff
./tests/fixtures/invalid_diffs/structural_field_tools_required.diff
./tests/fixtures/invalid_diffs/trace_check_edit.diff
./tests/fixtures/invalid_diffs/yaml_anchor_introduced.diff
./tests/fixtures/valid_diffs/critical_steps_desc_edit.diff
./tests/fixtures/valid_diffs/escalation_policy_edit.diff
./tests/fixtures/valid_diffs/grounding_instruction_edit.diff
./tests/fixtures/valid_diffs/procedure_edit.diff
./tests/test_cli_smoke.py
./tests/test_yaml_diff_validator.py
```

39 files under `autoloop/`. Plus this handoff at
`docs/sprints/sprint-054-handoff.md`. Total: 40 new files staged
for the S-Auto-1 commit. Zero edits to any existing file.

## 6. Baselines + gates

### Python test suites

- **autoloop**: `cd autoloop && uv run pytest -q` →
  `48 passed in 0.49s` ✅
- **eval_interactive**:
  `cd eval_interactive && uv run python -m pytest --tb=no -q` →
  `486 passed, 3 failed` ✅ (exact match to documented baseline;
  3 failures are the pre-existing `test_v2_schema_loads_cleanly` /
  `test_smoke_review_report_tracks_smoke_set_and_overrides` /
  `test_full_corpus_lints_clean_with_smoke_subset_flag` cases per
  prior milestone close documentation).

### Java suite

Skipped per the dev prompt clause: "if dev confirms via
`git diff --stat | grep -E '(server|eval)/src/main/java'` returns
empty Java zero-edit, can skip Java suite check". Verified:

```
$ git diff --stat HEAD -- server/ eval/ | grep -E 'src/main/java' || echo "ZERO Java edits"
ZERO Java edits
```

### Sandbox self-test

```
$ cd /Users/caoruixin/projects/csagent-latest && python -m autoloop check
[check] config: /.../autoloop/config.yaml
[check] repo root: /.../csagent-latest
[check] allowed_field_paths: 4
[check] allowed_skill_files: 6
[check] PASS: all 6 Skill YAML paths exist on disk
[check] PASS: sandbox self-test (1 positive + 1 negative)
$ echo $?
0
```

## 7. R-items consumed / surfaced

**Consumed:** None (M-Auto-1A is infrastructure; closes no semantic
R-items, per `docs/milestone_objective.md` §7).

**Surfaced (S-Auto-1):** None as blocking R-items. Two soft
observations for deliver-agent's M-Auto-1B / Stage-2 planning:

1. **Whitelist-pattern matcher kept minimal** — the
   `_pattern_to_regex` matcher supports only `[*]` wildcard. If a
   future surface unlock (Stage-2 templates.yaml or nested field
   paths) needs deeper wildcard semantics (e.g. `$..desc` for any
   `desc` at any depth), the matcher will need extending. Not a
   blocker for M-Auto-1A or M-Auto-1B; flagged so the v2 contract
   author knows where to look.
2. **`applier.py` skeleton documents cross-file invariant as a
   caller contract** — the actual enforcement code lands in
   S-Auto-3. The `test_cross_file_caller_rejects` test simulates the
   per-file invocation pattern but does NOT assert that the
   applier itself rejects multi-file bundles (the applier is a
   `NotImplementedError` placeholder). S-Auto-3 dev will need to
   write the multi-file-bundle rejection test against the real
   applier.

## 8. OQ (open questions surfaced for deliver-agent)

1. **Should `autoloop/uv.lock` be committed?** Convention from
   `eval_interactive/uv.lock` (committed) suggests yes; I am
   committing it in this S-Auto-1 commit. If deliver-agent prefers
   lockfile-out-of-tree for the autoloop package, revert in a
   trailing patch. (Soft OQ; no impact on S-Auto-1 deliverable.)
2. **`autoloop/.gitignore` placement** — added a local
   `autoloop/.gitignore` to exclude `.venv/`, `.pytest_cache/`,
   `*.egg-info/`, `__pycache__/`, `*.pyc`, and `results/` (the
   S-Auto-3 output directory). Root `.gitignore` left untouched.
   Verify this fits the repo convention; if root `.gitignore`
   needs an `autoloop/` entry instead, deliver-agent fold-back can
   move it.
3. **Anchor / alias detection via PyYAML event stream vs raw
   regex** — implemented via event stream to avoid false positives
   on natural-language `&` or `*` in `procedure` text. Trade-off:
   the function quietly returns False on a YAML that itself fails
   to parse; the caller separately handles parse failure via
   `safe_load` afterward. Documented in the function docstring;
   no behaviour gap because the post-anchor-check `safe_load` call
   catches the parse error. Surfacing in case S-Auto-2 / S-Auto-3
   wants different ordering.
4. **S-Auto-2 baseline_loader contract** — not in S-Auto-1 scope.
   Mentioning here per the dev prompt's note that S-Auto-2 deps
   (tier_evaluator, eval-runner) are not yet added to
   `pyproject.toml`; S-Auto-2 dev will need to add them.

## 9. Backend / dev-sandbox context

S-Auto-1 is pure Python + PyYAML. No backend interactions; no
running `mvn` / `spring-boot:run`; no live LLM provider calls; no
network. The CLI smoke tests subprocess `python -m autoloop ...`
locally but nothing leaves the box.

## 10. Self-check vs sprint-contract closing checklist

Per `compact/sprint-054-dev-prompt.md` "Self-check checklist":

- [x] `autoloop/` directory created (pyproject.toml + README.md +
  program.md + config.yaml + complete package tree per #1).
- [x] `autoloop/program.md` 8 sections present; hard fences are
  verbatim copy (not summary) of milestone §6 + proposal §8.
- [x] `validate_skill_yaml_diff` API complete, including all #3
  edge cases (anchor / reorder / new critical_step / deleted
  critical_step / comment-only / smuggle).
- [x] 4 positive fixtures + 19 negative fixtures (>= prompt's 12
  minimum).
- [x] `pytest` all PASS; new test count 48 (above the 20-30 target
  range — broad coverage chosen because the sandbox is the
  long-term §1.7 anchor).
- [x] `cd eval_interactive && uv run python -m pytest --tb=no -q`
  reproduces `486 passed, 3 failed`.
- [x] `git diff --stat HEAD` confirms zero lines under `server/`,
  `eval/`, `eval_interactive/`, `data/`, `db/`,
  `server/src/main/resources/`. The only files outside `autoloop/`
  are this handoff and a new untracked `.compare-output/` (which
  pre-dates this session per the initial git status — left alone).
- [x] `python -m autoloop check` exits 0 on shipped config; exits
  1 on config with a non-existent Skill path
  (`test_check_with_missing_skill_path_exits_one`).
- [x] `python -m autoloop dry-run --help` (and all other
  placeholders) print help and exit 0
  (`test_subcommand_help_exits_zero[<subcommand>]`).
- [x] `docs/sprints/sprint-054-handoff.md` (this file) complete;
  §12 left empty for deliver-agent + human at milestone close.
- [x] §7 self-walk recorded in §4 (Target layer / Tier-0 / Semantic
  hardcode / Generalization four-field).
- [x] Single commit will contain `autoloop/**` + this handoff;
  commit message per format (see §11).
- [x] Any STOP-surfaced items written in §8 (none blocking; 3 soft
  observations for deliver-agent awareness).

## 11. Commit / bundling note

Single commit-at-end pattern per the dev prompt. Staging list
(explicit `git add` per file, no `git add -A`):

```
autoloop/.gitignore
autoloop/README.md
autoloop/config.yaml
autoloop/program.md
autoloop/pyproject.toml
autoloop/uv.lock
autoloop/autoloop/__init__.py
autoloop/autoloop/__main__.py
autoloop/autoloop/cli.py
autoloop/autoloop/sandbox/__init__.py
autoloop/autoloop/sandbox/applier.py
autoloop/autoloop/sandbox/yaml_diff_validator.py
autoloop/tests/__init__.py
autoloop/tests/test_cli_smoke.py
autoloop/tests/test_yaml_diff_validator.py
autoloop/tests/fixtures/valid_diffs/*.diff      (4 files)
autoloop/tests/fixtures/invalid_diffs/*.diff    (19 files)
docs/sprints/sprint-054-handoff.md
```

Commit message (per prompt format):

```
Sprint 54 / S-Auto-1 — autoloop subsystem scaffold + YAML diff sandbox

#1 autoloop/ package skeleton (pyproject + README + config.yaml + tree).
#2 autoloop/program.md v1 LOCKED — 8 sections incl. verbatim §6 milestone
   fences + verbatim §8 proposal fences + §1.7 Appendix-D mapping.
#3 sandbox/yaml_diff_validator.py — full validate_skill_yaml_diff API
   incl. AST diff, [*]-wildcard whitelist matcher, anchor/alias detection
   via PyYAML event stream, empty-diff rejection, all #3 edge cases.
#4 cli.py subcommand router (argparse): `check` runs config validation +
   sandbox self-test; dry-run/run/report/apply/audit placeholders exit 1
   with "S-Auto-3 territory".
#5 4 positive + 19 negative fixtures + 48 pytest tests (all pass; ~3×
   the prompt's 20-30 target).
#6 README + pyproject closeout (PyYAML + pytest dev only; entry point
   `autoloop = "autoloop.cli:main"`).

Hard fences: zero edits to server/, eval/, eval_interactive/, data/,
db/, docs/foundational/, docs/current/, sprint/milestone archives. Java
suite skipped per Java-zero-edit exemption. eval_interactive baseline
reproduces 486/3.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
```

## 12. Sub-sprint close verdict (deliver-agent + human)

_Reserved for deliver-agent + human at M-Auto-1A close. Per §4.3
default, M-Auto-1A uses milestone-shared Codex review; S-Auto-1
does NOT fire a §4.3 per-sub-sprint Codex trigger
(it is `infra`, it adds no Tier-0 invariant, it does not cross a
§1.7 red line — it structurally ENFORCES §1.7 — and it does not
violate any hard-fenced surface). Codex consumes the cumulative
S-Auto-1 through S-Auto-4 commit range at milestone close per
§4.3 default plus §8.1 of `docs/milestone_objective.md`._
