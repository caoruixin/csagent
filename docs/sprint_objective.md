---
title: Sprint objective — Sprint 54 / M-Auto-1A S-Auto-1 — Mutable-surface contract + YAML diff sandbox
doc_tier: current-runtime
status: current
implementation_status: not_started
source_of_truth: this file
last_reviewed: 2026-05-27
review_cadence: per sub-sprint
supersedes: [docs/sprints/sprint-053-objective.md]
superseded_by: null
notes: >
  DRAFT pending human approval (2026-05-27). FIRST sub-sprint of Milestone
  M-Auto-1A — Auto-Evolution Build (the SIXTH milestone under §8 framework;
  consumes `docs/solutions/auto_evolution_skill_driven_v1.md` 2026-05-26).
  S-Auto-1 stands up the auto-loop subsystem directory + the structural
  sandbox that GATES every meta-agent-proposed Skill YAML edit before it
  ever reaches build/eval. The sandbox is the first of three structural
  defences (sandbox + tier_evaluator + anti_hardcode_check) that make
  the auto-loop safe to run. **Zero touch** to `server/`, `eval/`,
  `eval_interactive/`, `data/`, `db/`, any Skill YAML, any case_spec —
  S-Auto-1 ships pure new code under `autoloop/`. §7 REQUIRED; Codex
  deferred to M-Auto-1A milestone-shared close (default per §4.3).
---

# Sprint 54 / M-Auto-1A S-Auto-1 — Mutable-surface contract + YAML diff sandbox

## Class

`infra` (§3.2 default — no semantic Java decision change; no projection change; no eval-scoring change). **§7 REQUIRED** — S-Auto-1 defines the mutable-surface CONTRACT for an auto-evolution capability, which is a long-running structural anchor for §1.7 enforcement; per §7 even pure-infra work that is the *foundation* of subsequent semantic change must carry the stanza so the contract is reviewable.

## Goal

Stand up the `autoloop/` subsystem skeleton + the YAML-diff sandbox that ENFORCES the M-Auto-1A §6 hard-fence #9 ("no cross-file diff; single-Skill, single-field-class") and the proposal §3.1 narrow mutable surface (4 LLM-soft field classes × 6 Skill YAMLs). At S-Auto-1 close, the sandbox should be able to take any input diff and answer ACCEPT / REJECT with a precise reason, with full positive + negative test coverage. The sandbox is consumed by S-Auto-3's loop orchestrator; without it, downstream sub-sprints cannot safely apply meta-agent proposals.

The `autoloop/program.md` artifact authored in this sub-sprint is the **human-readable contract** that future readers (deliver-agent, Codex, human auditing a kept iteration) reference to know what the auto-loop is and is not allowed to do. It mirrors `docs/proposals/autoloop_design.md` §9 in spirit but with the M-Auto-1A-specific mutable surface + hard fences embedded verbatim.

## Scope (numbered; this is the contract)

### #1 — `autoloop/` directory scaffold

Create the following directory structure under repo root:

```
autoloop/
  pyproject.toml                  # uv-managed Python package; depends on PyYAML, pytest (no new heavy deps)
  README.md                       # one-page synopsis + CLI usage table
  program.md                      # LOCKED human contract (see #2)
  config.yaml                     # runtime config (paths, baseline ref, K-iter window for lessons compaction, etc.)
  autoloop/
    __init__.py
    cli.py                        # subcommand router: check / dry-run / run / report / apply / audit
    sandbox/
      __init__.py
      yaml_diff_validator.py      # this sub-sprint's MAIN deliverable
      applier.py                  # skeleton only (S-Auto-3 fills git commit + branch logic)
  tests/
    __init__.py
    test_yaml_diff_validator.py
    test_cli_smoke.py             # minimal: each subcommand exists and prints --help
    fixtures/
      valid_diffs/                # 4 positive fixtures
        procedure_edit.diff
        grounding_instruction_edit.diff
        escalation_policy_edit.diff
        critical_steps_desc_edit.diff
      invalid_diffs/              # 12+ negative fixtures
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
```

`pyproject.toml` declares the package + pytest + PyYAML (no other deps in S-Auto-1; tier_evaluator/eval-runner deps land in S-Auto-2). Use `uv` per existing repo convention (`uv pip install -e .` in `autoloop/`).

`autoloop/config.yaml` initial content scopes:
- `mutable_surface.allowed_field_paths` — the 4 LLM-soft field path patterns (e.g. `$.procedure`, `$.grounding_instruction`, `$.escalation_policy`, `$.critical_steps[*].desc`).
- `mutable_surface.allowed_skill_files` — the 6 Skill YAML paths (e.g. `server/src/main/resources/skills/discover_triage.yaml`, ..., `terminal.yaml`).
- `paths.experiments_log` / `paths.iterations_index` / `paths.lessons_log` / `paths.runs_dir` — defaults under `autoloop/results/` (S-Auto-3 will exercise these).
- `lessons.compaction_window_k` — default 10 (S-Auto-3 will read this).

### #2 — `autoloop/program.md` human contract

Author `autoloop/program.md` as the canonical human-readable contract. Structure (mirroring `docs/proposals/autoloop_design.md` §9):

1. **Purpose**: one paragraph — auto-loop is a Skill-driven hill-climbing meta-agent constrained by the M-Auto-1A mutable surface; its outputs are CANDIDATES requiring human review before merging to main.
2. **Mutable surface (verbatim)**: 6 Skill YAMLs × {`procedure`, `grounding_instruction`, `escalation_policy`, `critical_steps[].desc`}. Anything else is locked.
3. **Hard fences (verbatim copy from `docs/milestone_objective.md` M-Auto-1A §6 + proposal §8)**: 12 milestone-level + 8 proposal-level fences. Numbered so Codex/human can cite "fence #N".
4. **Forbidden by construction (structural defences)**:
   - sandbox white-list (this sub-sprint)
   - 4-tier lexicographic fitness (S-Auto-2)
   - anti-hardcode auto-check (S-Auto-4)
   - shadow regression gate (built into the lexicographic verdict)
   - shadow result firewall (aggregate `yes/no` only to meta-agent; per-case never)
   - no main-branch cherry-pick by the loop itself (human cherry-picks via `python -m autoloop apply`)
5. **§1.7 forbidden-list mapping**: each §1.7 item → which structural defence catches it (the Appendix D table from the proposal verbatim).
6. **What the loop is NOT allowed to do** even if a propose looks like a great idea: change tools_required, modify trace_check, edit any case_spec, edit any Java code, edit any other Skill in the same diff, write to main branch directly.
7. **What the human gives up by running the loop**: the loop will sometimes propose changes that the human would have proposed manually — this is fine, the human gains time. The loop will never propose changes the human is forbidden to make (locked surface).
8. **Versioning**: `program.md` is locked at v1; future Stage-2 expansion (e.g. templates.yaml) requires a new milestone + a new `program.md` v2 with explicit human authorization.

This file is **immutable during M-Auto-1A execution** once committed. Any change requires a new sub-sprint authorization.

### #3 — `autoloop/sandbox/yaml_diff_validator.py` — the main deliverable

Implement a `validate_skill_yaml_diff(before_yaml: str, after_yaml: str, file_path: str) -> ValidationResult` API.

`ValidationResult` is a dataclass with fields:
```python
@dataclass
class ValidationResult:
    decision: Literal["ACCEPT", "REJECT"]
    reason: str                              # one-line human-readable
    rejected_paths: list[str]                # YAML AST paths that violated white-list (empty on ACCEPT)
    accepted_paths: list[str]                # YAML AST paths that were modified and pass white-list (only on ACCEPT)
    file_path: str                           # echoed back for audit
```

Algorithm:

1. **File-path check**: `file_path` MUST be in `config.mutable_surface.allowed_skill_files` (the 6 known Skill YAMLs). Anything else → REJECT (reason: "file outside mutable surface").
2. **YAML parse**: parse both `before_yaml` and `after_yaml` with `yaml.safe_load`. Parse failure on `after_yaml` → REJECT (reason: "malformed YAML in proposed edit"). Parse failure on `before_yaml` → REJECT (reason: "baseline YAML is malformed — refusing to compare"; treat as evaluator-side error, not a meta-agent fault).
3. **AST diff**: compute the set of YAML node paths that changed between `before` and `after`. Use a path notation like `$.procedure`, `$.critical_steps[0].desc`, `$.applicable_use_cases[2]`. Implement via recursive walk of the parsed dicts/lists (no external dep).
4. **White-list match**: for each changed path, check it matches one of `config.mutable_surface.allowed_field_paths` patterns. Wildcards: `[*]` matches any list index. So `$.critical_steps[*].desc` matches `$.critical_steps[0].desc`, `$.critical_steps[3].desc`, etc. A changed `$.critical_steps[0].trace_check` does NOT match — REJECT.
5. **Any rejected path** → REJECT (reason: "modification to non-mutable field(s): <paths>").
6. **All paths whitelisted** → ACCEPT.

Multi-file / cross-file diffs are handled at the CALLER level — the caller must invoke `validate_skill_yaml_diff` ONCE per file in a diff bundle; if the bundle contains >1 file, the caller (S-Auto-3 loop) rejects without even calling the validator. S-Auto-1's `applier.py` skeleton documents this contract.

**Edge cases the implementation must handle**:
- Adding a new `critical_steps` entry — REJECT (changes both `$.critical_steps[N]` structure AND adds new `$.critical_steps[N].id|trace_check|mandatory_for|severity|desc`).
- Deleting a `critical_steps` entry — REJECT (same reason).
- Modifying `$.critical_steps[N].desc` BUT also accidentally adding whitespace to `$.critical_steps[N].mandatory_for` (YAML re-serialization artifact) — REJECT; the implementation must AST-diff semantic content, not byte content.
- Reordering list entries with otherwise identical content — REJECT (treat reorder as a structural change; the meta-agent should never need to reorder).
- YAML anchors / references (`&anchor`, `*ref`) — REJECT any diff that adds, removes, or relocates an anchor; only direct value changes to whitelisted fields are accepted.
- Comment changes — neutral (PyYAML's safe_load drops comments; comment-only diffs become NO-OP at AST level → REJECT with reason "no whitelisted field changed").

### #4 — `autoloop/cli.py` subcommand scaffold

Implement `autoloop/cli.py` with the following subcommands (each a `click` or `argparse`-based handler):

- `check` — validates `config.yaml` (paths exist, allowed Skill files exist on disk) + runs a self-test that exercises one positive + one negative sandbox fixture and confirms expected output. Used by humans to confirm install is correct.
- `dry-run` — placeholder in S-Auto-1; prints "S-Auto-3 territory" + exits 1. Stub for the full implementation.
- `run` — same placeholder.
- `report` — same placeholder.
- `apply --experiment <id>` — same placeholder.
- `audit --experiment <id>` — same placeholder.

Each subcommand exposed as `python -m autoloop <subcommand>` (entry point in `pyproject.toml`).

### #5 — Tests

`autoloop/tests/test_yaml_diff_validator.py`:
- **Positive cases (4)**: one fixture per allowed field type, each modifying ONLY that field, asserts `decision == "ACCEPT"`.
- **Negative cases (12+)**: each fixture from `fixtures/invalid_diffs/`, each asserts `decision == "REJECT"` and the `reason` contains the expected substring (e.g. "applicable_use_cases" for the structural-field test).
- **Edge cases**: at least 3 of (add new critical_step, delete critical_step, reorder list, YAML anchor introduction, comment-only diff) — assert REJECT.
- **Cross-Skill diff**: caller responsibility; documented in a test docstring + a `test_cross_file_caller_rejects` test that simulates calling `validate_skill_yaml_diff` for each file in a bundle and confirms at least one returns REJECT.

`autoloop/tests/test_cli_smoke.py`:
- Each subcommand `python -m autoloop <name> --help` exits 0 and prints help text.
- `python -m autoloop check` against a known-good config exits 0; against a config with a non-existent skill path exits 1.

### #6 — Documentation polish

- `autoloop/README.md`: one page — what auto-loop is (3 sentences), CLI synopsis (table of subcommands), pointer to `program.md` for the full contract, pointer to `docs/milestone_objective.md` for milestone scope, pointer to `docs/solutions/auto_evolution_skill_driven_v1.md` for the design rationale.
- `pyproject.toml`: `[project]` with `name = "csagent-autoloop"`, `version = "0.1.0"`, dependencies = `["PyYAML>=6.0"]`, `[project.scripts]` exposing `autoloop = "autoloop.cli:main"`.

## Hard fences / STOP conditions (do NOT do)

- **No touch** to `server/`, `eval/`, `eval_interactive/`, `data/`, `db/`, `docs/foundational/`, `docs/runtime_freeze_and_risk_policy.md`, `docs/current/**` (including no edits to `iteration_governance.md`), `docs/sprints/*` archives, `docs/milestones/*` archives.
- **No touch** to any file under `server/src/main/resources/{skills,prompts,scripts,config,mock}/**`. The sandbox VALIDATES diffs against the Skill YAMLs but never writes to them.
- **No editing** of `docs/codex-findings.md` during execution (it is a scaffold; deliver-agent + Codex write at milestone close).
- **No new heavy dependencies** in `pyproject.toml`. PyYAML + pytest are the only S-Auto-1 deps. Anything else (e.g. PyYAML alternative, AST library) requires deliver-agent + human approval BEFORE adding.
- **No live LLM call** anywhere in S-Auto-1 code. The meta-agent LLM is wired in S-Auto-3; S-Auto-1 only validates synthetic fixture diffs.
- **No subprocess invocation** of `mvn` / `spring-boot` / `eval-interactive` / `uv run` from S-Auto-1 code. Those land in S-Auto-2 / S-Auto-3.
- **No `git add -A`** when staging the commit. Stage only the files this sub-sprint creates (`autoloop/**` paths enumerated above + `docs/sprints/sprint-054-handoff.md` at close).
- **STOP and surface** to deliver-agent if YAML AST diff implementation reveals an ambiguity in production Skill files (e.g. an existing Skill YAML uses a YAML feature that doesn't round-trip through `safe_load → re-serialize`). Do NOT silently work around it; the milestone §10 stop condition #1 covers this case.
- **STOP and surface** if the white-list field-path patterns can be expressed cleanly but ambiguously match an unintended field (e.g. a Skill that has a top-level field named `procedure` AND a nested `procedure` somewhere). Document the case; deliver-agent + human disambiguate before writing the validator.

## Test / eval requirements

- **Python**: `cd autoloop && uv run pytest -q` — all `autoloop/tests/` tests PASS. New test count expected: ~20-30 (12+ negative fixtures + 4 positive + edge cases + CLI smoke).
- **Existing Python suite unchanged**: `cd eval_interactive && uv run python -m pytest --tb=no -q` reproduces `486 passed, 3 failed` (no eval_interactive touch).
- **Java baseline unchanged**: `mvn test -B` from repo root reproduces `1183 / 1-inherited / 0 / 2` (no server/eval-Java touch). May skip this check if dev confirms zero Java edits via `git diff --stat | grep -E '(server|eval)/src/main/java'` returning empty.
- **No eval invocation** in S-Auto-1. No `eval-interactive run` calls.

## §7 — Layer-classification + anti-hardcode stanza

**Target failure layer:** `infra` (§3.2 default — pure infrastructure, no semantic decision change, no projection change, no scoring change). S-Auto-1 is the structural anchor for §1.7 enforcement in future sub-sprints; it does not itself make a semantic decision.

**Tier-0 invariant:** adds no Tier-0 invariant. The sandbox enforces the M-Auto-1A mutable-surface contract, which is a milestone-scoped boundary, not a Tier-0 runtime invariant per `docs/runtime_freeze_and_risk_policy.md` §1/§2. C2/C3 DEFER continues per M2-close verdict.

**Semantic hardcode:** No semantic hardcode introduced. The sandbox is REGEX-FREE for white-list matching (uses YAML AST path comparison, not text matching). The white-list itself is a small literal set of 4 YAML paths × 6 file paths, which is a registry / contract (the human-locked mutable surface from the proposal §3.1), not a semantic decision. If the dev finds the white-list cannot be expressed without text matching or fuzzy logic, STOP and surface — that signals the YAML structure has a gotcha that needs separate design.

**Generalization coverage:** target = the sandbox correctly accepts the 4 positive fixture diffs (one per allowed field type) and rejects the 12+ negative fixture diffs (structural field, trace_check, mandatory_for, severity, id, guardrails, state_inheritance, cross-Skill, cross-file, new file, deleted field, malformed YAML). Neighbor = the sandbox correctly handles edge cases (anchor introduction, reorder, comment-only diff). Negative = the sandbox correctly rejects a hand-authored adversarial diff that LOOKS like a valid `procedure` edit but smuggles in a `trace_check` change (REJECT). Shadow = N/A for S-Auto-1 (shadow is exercised in S-Auto-2 tier_evaluator, not the sandbox).

## Codex review plan (§4.3)

**Default**: milestone-shared at M-Auto-1A close. S-Auto-1 is `infra` + does not cross §1.7 (it ENFORCES §1.7 structurally rather than crossing it). No per-sub-sprint Codex trigger fires.

The deliver-agent does NOT dispatch Codex at S-Auto-1 close. Codex review consumes the S-Auto-1 commit range at M-Auto-1A close together with S-Auto-2/3/4 (the cumulative milestone range).

## Handoff requirements

- Author `docs/sprints/sprint-054-handoff.md` at S-Auto-1 close; leave **§12** empty (deliver-agent + human at milestone close).
- Record in handoff: `git show --numstat` for the S-Auto-1 commit; the `autoloop/tests/` test count + pass status; the `autoloop/program.md` final content (or a pointer + summary of any deviation from this sub-sprint's #2 outline); any STOP-surfaced ambiguity in YAML AST diff implementation (if applicable); a §7 self-walk confirming `infra` classification + no Tier-0 + no semantic hardcode.
- Document in handoff §"OQ surfaced": any unresolved-but-non-blocking question the dev wants deliver-agent to decide before S-Auto-2 begins (e.g. AST-diff library choice if PyYAML stdlib proves insufficient; baseline_loader.py contract preview).

## Commit discipline

Dev stages **only S-Auto-1 scope**: all new files under `autoloop/**` per #1 directory tree + `docs/sprints/sprint-054-handoff.md`. **No `git add -A`** — deliver-agent close-bundle files (`docs/milestone_objective.md` updates, `docs/10-handoff.md` refresh, `docs/action_bank.md` updates) are bundled by the human at sub-sprint or milestone close.

One commit at sub-sprint close (commit-at-end pattern). Commit message format: `Sprint 54 / S-Auto-1 — autoloop subsystem scaffold + YAML diff sandbox` followed by a brief paragraph summarizing the 6 scope items + the new test count.

## Scope size (§8.5 note)

M-Auto-1A with S-Auto-1 = 1 of 4 sub-sprints; within the §8.5 5-sub-sprint ceiling. S-Auto-1 is pure infrastructure; estimated 3 dev-days. No conditional / deferred scope items (everything is either in or out; no "if budget left" tail).

## OQ (open questions — filled during the sub-sprint)

_to be added by the dev session as ambiguities surface_
