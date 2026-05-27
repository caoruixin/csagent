# autoloop — program.md (v1, LOCKED)

> **Locked during M-Auto-1A execution.** Any change to this file
> requires a new sub-sprint authorization (per the §8 Versioning rule
> below). Future Stage-2 surface unlocks require a new milestone + a
> new `program.md` v2 + explicit human authorization.

This is the **human-readable contract** for the auto-evolution loop.
Future deliver-agent / Codex / human auditors of a kept iteration
should read this file FIRST to know what the loop is and is not
allowed to do — before reading any code under `autoloop/`.

The source-of-truth design rationale lives in
`docs/solutions/auto_evolution_skill_driven_v1.md`. The milestone
contract lives in `docs/milestone_objective.md` (active) or
`docs/milestones/M-Auto-1A_objective.md` (archived). This file is the
**executable view**: it states the contract in operational terms the
loop itself enforces.

---

## §1 Purpose

The auto-loop is a Skill-driven hill-climbing meta-agent constrained
to the M-Auto-1A mutable surface defined in §2 below. It iterates
on `autoloop/exp-N` test branches, proposes edits to allowed fields
of allowed Skill YAMLs, evaluates each proposal against the existing
four-tier evaluation framework, and keeps only proposals that strictly
improve the lexicographic fitness without regressing shadow cases. A
kept iteration is a **CANDIDATE**, not a merge: the human + deliver-
agent review the candidate via the bad-case suite manual review gate
(per `docs/current/iteration_governance.md` §5.6) BEFORE any merge to
`main` via `python -m autoloop apply`.

The loop does not replace human judgment. It is a search procedure
operating inside a structurally locked surface.

---

## §2 Mutable surface (verbatim)

Auto-loop's meta-agent may propose edits to **only** the following
field paths inside **only** the following Skill YAML files. Every
other byte of every other file in the repository is structurally
out of reach.

**Allowed Skill YAML files (6):**

- `server/src/main/resources/skills/discover_triage.yaml`
- `server/src/main/resources/skills/resolve_faq_grounded_answer.yaml`
- `server/src/main/resources/skills/resolve_intake_collect_and_handover.yaml`
- `server/src/main/resources/skills/confirm.yaml`
- `server/src/main/resources/skills/escalate.yaml`
- `server/src/main/resources/skills/terminal.yaml`

**Allowed field paths (4 LLM-soft field classes):**

- `$.procedure`
- `$.grounding_instruction`
- `$.escalation_policy`
- `$.critical_steps[*].desc`

The path notation matches the YAML AST diff representation produced
by `autoloop/sandbox/yaml_diff_validator.py`. `[*]` matches any list
index; the wildcard does not extend to other field names.

**Locked (structurally rejected by the sandbox):** every other
top-level field of every Skill YAML — `name`, `description`,
`objective`, `applicable_phases`, `applicable_use_cases`,
`tools_required`, `required_context_keys`, `max_tool_steps`,
`allow_interim_message`, `valid_terminal_outcomes`, `guardrails`,
`state_inheritance` — plus every per-critical-step field other than
`desc`: `id`, `trace_check`, `mandatory_for`, `severity`. Adding a
new `critical_steps` entry, deleting an existing entry, or reordering
entries is structurally rejected. Every other file in the repository
— Java code, prompts, configs, mocks, scripts, case_specs, data, db,
docs — is outside the surface entirely.

---

## §3 Hard fences (verbatim)

### §3.A Milestone-level hard fences

> Copied verbatim from `docs/milestone_objective.md` §6. Numbering is
> the milestone-fence numbering, **not** an internal numbering of this
> file. Future references in the loop code / handoffs / Codex prompts
> may refer to "fence #N" using these numbers.

These are the structural guarantees that distinguish M-Auto-1A from
any "auto-loop builds and accidentally ships a Skill YAML edit"
failure mode. They are **the milestone**, not optional.

1. **No edits** to any file under `server/src/main/java/**`. The
   Runtime side stays byte-identical to M5-close `c9390dc`.
2. **No edits** to any file under `eval/src/main/java/**`,
   `eval_interactive/eval_interactive/**`,
   `eval_interactive/case_specs/**`,
   `eval_interactive/case_specs_shadow/**`. The Evaluator side stays
   byte-identical.
3. **No edits** to any file under
   `server/src/main/resources/{skills,prompts,scripts,config,mock}/**`.
   The agent runtime-loaded artefact set stays byte-identical.
   (Auto-loop VALIDATES diffs that target Skill YAMLs but never
   WRITES them in M-Auto-1A.)
4. **No edits** to `data/**`, `db/migration/**`, `docs/foundational/**`,
   `docs/runtime_freeze_and_risk_policy.md`,
   `docs/current/iteration_governance.md`. Foundational + governance
   stay frozen for this milestone.
5. **No edits** to sprint archives `docs/sprints/sprint-001-*`
   through `docs/sprints/sprint-053-*` or any prior milestone archive
   under `docs/milestones/`.
6. **No `git add -A`** by the dev agent. Stage only S-Auto-N scope
   files explicitly. Deliver-agent close-bundle artefacts
   (`docs/milestone_objective.md`, `docs/sprint_objective.md`,
   `docs/10-handoff.md`, `docs/action_bank.md`, etc.) bundled by the
   human at close per
   `feedback_commit_at_end_bundles_deliver_artefacts.md`.
7. **No cherry-pick to main** of any `autoloop/exp-N` branch in
   M-Auto-1A. The S-Auto-3 live-iteration close gate accepts ANY
   decision (keep or discard) on the test branch; main branch carries
   only the `autoloop/` infrastructure code, never a Skill YAML edit
   from an auto-loop iteration.
8. **No new Tier-0 invariant**. C2/C3 DEFER continues per M2-close
   verdict.
9. **No cross-file diff** by meta-agent ever (sandbox enforces).
   Single-Skill, single-field-class diff per iteration.
10. **No shadow-set leakage to meta-agent**. The shadow result fed
    back to the loop is the aggregate
    `{shadow_regression_detected: yes|no, drop_pct: <float>}` only.
    Per-case shadow failures NEVER reach `meta_agent/proposer.py`.
    Implemented as a structural firewall in
    `autoloop/scoring/tier_evaluator.py` (separate API surface for
    the loop vs. for the human audit `report` subcommand).
11. **No mutation of `eval_interactive/results/` schema**. Auto-loop
    produces parallel output under `autoloop/results/` only; eval
    invocation reuses existing schema unchanged.
12. **No editing of `docs/codex-findings.md` during M-Auto-1A
    sub-sprint execution**. The live `docs/codex-findings.md` is a
    scaffold; S-Auto-4 per-sub-sprint Codex writes to it at S-Auto-4
    close; milestone-shared Codex review writes to it at M-Auto-1A
    close; archived to `docs/milestones/M-Auto-1A_codex-review.md` at
    milestone close per standard deliver-agent close-out.

### §3.B Proposal-level hard fences

> Copied verbatim from
> `docs/solutions/auto_evolution_skill_driven_v1.md` §8 "Hard fences
> (milestone-level; 无例外)". Numbering is the proposal's; the
> milestone-fence numbering above takes precedence on overlap. The
> proposal §8 was the source from which `docs/milestone_objective.md`
> §6 was derived, so the overlap is intentional.

1. M-Auto-1 不修改任何 `server/src/main/java/**`。
2. M-Auto-1 不修改任何 `eval/` / `eval_interactive/eval_interactive/**`
   代码（包括 scoring / batch / loader / simulator）。
3. M-Auto-1 不修改任何 case_spec 文件——包括
   `eval_interactive/case_specs/{anchor,anchor_outcome,bad_cases,case_families,smoke,exploration,probe,promotion}/**`
   和 `eval_interactive/case_specs_shadow/**`。
4. M-Auto-1 不修改 `docs/runtime_freeze_and_risk_policy.md`、
   `docs/foundational/**`、
   `docs/current/iteration_governance.md`（除非 Stage 1 完成后需要 fold-back
   §5.5/§5.6 关于 auto-loop 的角色描述，那是 M-Auto-1 close 之后的 doc
   governance 工作）。
5. M-Auto-1 不修改 `server/src/main/resources/{prompts,scripts,config}/**`
   下任何文件（V1 锁定面；Stage 2 才可能解锁 templates.yaml）。
6. auto-loop 的 meta-agent 不允许跨多文件 diff——每次 propose 只能改一
   个 Skill YAML 的一个字段或同一个 Skill YAML 的多个允许字段；不允许
   跨 Skill。
7. auto-loop 不写 `main` 分支；所有 kept 候选只在 `autoloop/keep-{N}`
   分支；human 评审后用 `python -m autoloop apply` 才 cherry-pick。
8. shadow set 失败信息**永不**喂回 meta-agent。meta-agent 只看见聚合
   `shadow_regression_detected: yes/no`。

---

## §4 Forbidden by construction (structural defenses)

Each of the §3 fences is enforced by a structural defense at a
specific layer. The loop cannot bypass these without code changes
that themselves cross the M-Auto-1A scope and would be caught by
review.

| Defense | Where | Catches | Sub-sprint |
|---|---|---|---|
| YAML-diff sandbox whitelist | `autoloop/sandbox/yaml_diff_validator.py` | meta-agent edits outside the 6 × 4 mutable surface; cross-file diffs; new / deleted / reordered `critical_steps`; YAML anchors; comment-only NO-OP diffs | S-Auto-1 (this sub-sprint) |
| 4-tier lexicographic fitness | `autoloop/scoring/tier_evaluator.py` | proposals that improve any tier by regressing a strictly-higher tier (Tier-0 safety > Tier-1 mandatory > Tier-2 advisory > Tier-3 trend); proposals that improve visible eval at the cost of shadow | S-Auto-2 — **DELIVERED** (Sprint 55; v1 narrow dataset: bad_cases + anchor_outcome + shadow; anchor 159 excluded per `docs/milestone_objective.md` §2; Layer 3 improvement-threshold mode = absolute case_count, human-locked 2026-05-27) |
| Anti-hardcode auto-check | `autoloop/sandbox/anti_hardcode_check.py` + `meta_agent/propose.txt` self-check | propose-stage rejection of patterns that look like raw eval phrases / UC-specific if-else / decision-tree narrations in `procedure` or `critical_steps[].desc` | S-Auto-4 |
| Shadow regression gate | built into lexicographic verdict | proposal kept only when `shadow_regression_detected: no`; any shadow regression auto-discards regardless of visible-eval gain | S-Auto-2 |
| Shadow result firewall | `autoloop/scoring/tier_evaluator.py` (two API surfaces) | per-case shadow detail surfaces only to the human audit `report` subcommand; meta-agent only ever sees the aggregate `{yes\|no, drop_pct}` | S-Auto-2 |
| No main-branch cherry-pick by the loop itself | `autoloop/cli.py apply` subcommand is the only writer to `main` | the loop runs on `autoloop/exp-N` branches; `apply` is human-invoked from a checked-out main with explicit `--experiment <id>`; no automation path writes to main | S-Auto-3 |

A defense that fails (e.g. the sandbox accepts a structural edit) is
**a milestone-level bug**, not an "expected occasional miss". Codex
S-Auto-4 + milestone-close review independently spot-check each
defense with adversarial inputs.

---

## §5 §1.7 forbidden-list mapping

> Copied from
> `docs/solutions/auto_evolution_skill_driven_v1.md` Appendix D.
> `§1.7` refers to `docs/current/iteration_governance.md` §1.7
> (Constitution → Forbidden).

| §1.7 forbidden item | M-Auto-1 v1 defense |
|---|---|
| encoding raw eval phrases into Java or prompt | sandbox `anti_hardcode_check.py` Q4: propose diff containing case_id / session_id / known eval phrase pattern → propose-stage discard |
| adding UC-specific hard rules for soft semantic decisions | sandbox: `tools_required` / `applicable_use_cases` / `guardrails` locked; if-else patterns inside `desc` caught by `anti_hardcode_check.py` Q1 + Codex S-Auto-4 |
| widening eval spec to accept a genuine bot mistake | M-Auto-1 hard fence #3: no edits to any case_spec; no edits to any scoring code |
| optimizing visible eval at the cost of shadow / generalization | Layer 4 shadow gate (3% threshold) + shadow result firewall (per-case shadow detail never reaches meta-agent) |
| using prompt as an if-else rule dump | meta-agent `propose.txt` explicitly forbids; `anti_hardcode_check.py` Q1; Codex S-Auto-4 per-sub-sprint review |

---

## §6 What the loop is NOT allowed to do (even if a propose looks great)

Some of these overlap with §3 fences; they are restated here in the
"don't do this even if it looks tempting" voice an auditor will use.

- **Change `tools_required`** for any Skill — even if the meta-agent's
  reasoning is impeccable. Tool surface is a Runtime-owned schema and
  is part of `docs/current/iteration_governance.md` §1.4. Auto-loop
  is `§1.3 LLM-owned`-only.
- **Change `trace_check`** on any `critical_steps` entry. The Tier-2
  measurement contract must stay stable across iterations so the
  evaluator measures the same thing. Changing `desc` while
  `trace_check` is locked is exactly the design intent.
- **Edit any `case_spec`** — anchor, anchor_outcome, bad_cases,
  case_families, smoke, exploration, probe, promotion, shadow. The
  evaluator side stays byte-identical.
- **Edit any Java** — Runtime side stays byte-identical. The auto-
  loop does not adjudicate Runtime invariants.
- **Bundle multiple Skill YAML edits into one diff.** The cross-file
  fence (#9) is the structural guarantee that one iteration changes
  exactly one Skill YAML's allowed fields and nothing else. A
  bundled diff is rejected by the caller layer
  (`autoloop/sandbox/applier.py`) before the validator is invoked.
- **Write directly to `main`.** All kept iterations land on
  `autoloop/keep-{N}` (or `autoloop/exp-N` for in-flight); only the
  human, invoking `python -m autoloop apply --experiment <id>` from
  a checked-out main, performs the cherry-pick.
- **Treat `record_outcome` / `request_handover` / any tool name as
  text to delete or rename inside `procedure`.** The tool surface is
  locked; references to tool names inside `procedure` describe
  behaviour for the LLM but do not redefine the surface. A propose
  that changes only `procedure` may still propose to alter which
  tools the LLM is *guided to call* in what order, but the tools
  themselves remain available — this is LLM-owned semantic guidance
  per §1.3.

---

## §7 What the human gives up by running the loop

This section names the trade. It is not a complaint surface; it is a
contract.

- **The loop will sometimes propose changes the human was already
  going to propose.** That is fine. Convergence on a fix the human
  also saw is not a violation; it is evidence the loop is searching
  in the right direction. The loop's value is rate, not novelty.
- **The loop will never propose changes the human is forbidden to
  make.** The mutable surface in §2 is the same surface the human
  would respect when manually editing a Skill YAML during a normal
  sub-sprint. The loop is a faster path to the same kind of edit,
  not a path to a different kind of edit.
- **The loop may surface candidates the human chooses to discard.**
  A kept iteration is a CANDIDATE, not a merge. Human + deliver-
  agent review via the bad-case suite manual review gate (§5.6 of
  the Constitution) is the merge gate. A candidate the human reads
  and rejects is not a failure of the loop; it is the loop working
  as designed.

---

## §8 Versioning

This `program.md` is **v1**, locked during M-Auto-1A execution.

The contract is **immutable during M-Auto-1A**: once committed at
S-Auto-1 close, any modification requires explicit sub-sprint
authorization (a new sub-sprint contract that names "modify
`autoloop/program.md`" as its scope).

Future Stage-2 surface unlocks — for example, allowing the loop to
propose edits to `templates.yaml`, or to a wider class of Skill YAML
fields — require:

1. A new milestone (NOT a mid-milestone scope change) with the
   unlock as a named goal in `docs/milestone_objective.md`.
2. A new `program.md` v2 produced as the first sub-sprint of that
   milestone (analogous to how S-Auto-1 produces v1).
3. Explicit human authorization recorded in the deliver-agent's
   milestone planning round.
4. A Codex review of the v2 contract against the §1.7 forbidden-
   list before the milestone's loop sub-sprints execute.

The v1 → v2 transition is itself a structural defense: it forces
the unlock to be a human-deliberated decision, not a code change
slipped into a mid-milestone PR.

---

## Appendix — Source-of-truth pointers

- Milestone scope (active): `docs/milestone_objective.md`
- Milestone scope (archived at close):
  `docs/milestones/M-Auto-1A_objective.md`
- Design rationale + alternatives + §1.7 mapping + risk table:
  `docs/solutions/auto_evolution_skill_driven_v1.md`
- Constitution: `docs/current/iteration_governance.md`
- Doc-governance: `docs/current/doc_governance.md`
- Repo constitution chain: `AGENTS.md`
