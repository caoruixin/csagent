# Context Pack — Governance/Collaboration Framework Optimization & Extraction

**Date:** 2026-06-06 · **Branch:** `auto-loop-branch` · **Repo:** `/Users/caoruixin/projects/csagent-latest`
**Purpose:** Hand off the multi-step "optimize the always-loaded governance docs → extract a portable A/B/C/D framework template" work to a fresh agent session. Self-contained; read this + `AGENTS.md` (auto-loaded) to continue.

---

## 背景 (Background)

The csagent repo runs an LLM-first customer-service agent under explicit governance. Four agent roles collaborate **only through repo docs (never shared chat history)**: **dev** (Claude Code), **deliver** (orchestrator), **review** (Codex), **research**.

The governance chain is **always-loaded** via `AGENTS.md` (`@`-includes), which transitively loads:
`docs/current/doc_governance.md` → `docs/current/agent_context_guide.md` → `docs/current/iteration_governance.md`.

`iteration_governance.md` was the problem: 44.8 KB loaded into **every** agent session. Its original §-map:
§1 Constitution · §2 Failure-Brief template · §3 Fix-Layer classification checklist · §4 Anti-hardcode review · §5 Eval acceptance rules · §6 Architecture-health metrics · §7 Sprint-objective stanza · §8 Milestone framework · §9 Prompt-artifact rules.

Always-loaded chain before this work: `AGENTS.md` 4.5KB + `doc_governance` 12.9KB + `agent_context_guide` 10.2KB + `iteration_governance` 44.8KB ≈ **72 KB/session**. Big on-demand files: `action_bank.md` 158KB, `platform_api_detailed_reference.md` 242KB, `10-handoff.md` 27KB, `milestone_objective.md` 28KB.

## 目标 (Goals)

1. Cut per-session context cost by splitting the always-loaded governance into a small **always-loaded Constitution** + **on-demand process** docs — *move, never delete*; preserve all history.
2. Continue the same "small live core + on-demand/archived detail" pattern across the state ledgers.
3. **Extract a portable, project-agnostic A/B/C/D framework template** from the now-proven split, so it can be copied into new projects. The csagent repo is the **reference instantiation**, not part of the template.

**A/B/C/D layer model** (the spine of everything):
- **A — Constitution** (always-loaded; timeless; ≤~20 KB target): objective, primary principle, X-owns/Y-owns boundary, iteration rule, eval rule, forbidden list, fix-layer classification, sprint stanza.
- **B — Process** (on-demand; mostly portable): doc-governance, milestone framework, prompt-artifact rules, bad-case lifecycle, anti-hardcode kernel, arch-health metrics, agent-context reading lists, **role registry** (dev/deliver/review/research).
- **C — State Ledgers** (per-project; retention-governed): backlog ledger + archive, cross-session handoff (§0/§1/§2), milestone/sprint contracts, review findings.
- **D — Prompt Artifacts** (generated; self-contained): dev + review session prompts.

## 已确认事实 (Confirmed facts)

**Git commit chain THIS task produced** (parent of all = `5970664`). NOTE: by 2026-06-06 the branch HEAD has since advanced to `588cc91` — csagent's own M-Auto-6 work continued independently after this task paused. The commits below are landed and are this task's output:
1. `ad4ca9b` — **Layer A/B split** of `iteration_governance.md` (44.8KB → **20.4KB**). Moved out to 5 new Layer-B docs, leaving one-line **pointer stubs** at the moved §-numbers so the ~190 archive citations still resolve (section numbers kept stable):
   - `docs/current/process/milestone-framework.md` ← §8 + §4.3
   - `docs/current/process/prompt-artifact-rules.md` ← §9
   - `docs/current/process/badcase-lifecycle.md` ← §5.6/.6.1/.6.2/.6.3 + §5.5 dated rationale
   - `docs/current/process/architecture-health-metrics.md` ← §6
   - `docs/current/governance-examples.md` ← §2 + §7.2 worked examples
   - `AGENTS.md` edited: still `@`-includes the same 3 docs; added an on-demand "process docs" pointer list. Always-loaded chain: ~72KB → **~49KB**.
2. `6121f69` — **action_bank §7.1 step-3 sweep** (158KB → **94.7KB**). ID-preserving: all **129** open `D-/G-/R-/OQ-` IDs retained; only inline landed-history prose stripped to one-line pointers. NOTE: `action_bank.md` had **already** been largely swept by the 2026-06-01 migration (`0323457`, the archive split into `docs/action_bank_archive.md` §A/§B/§C); `6121f69` did ~95% of the remaining step-3.
3. `e435c0c` — **state-file outcome dedup**. The current sub-sprint outcome (S-Auto-13) was repeated ~6×; collapsed to a single canonical owner (`docs/sprints/sprint-068-handoff.md`) + 1-line pointers. Edited `10-handoff.md` + `milestone_objective.md` only. `sprint_objective.md` deliberately NOT touched (it must stay self-contained per §9).
4. `59e19da` — **teams docs dedup + repoint**. Edited `docs/teams/{collaboration-guide,deliver-agent,research-agent}.md`: collapsed the eval-gate table (was tabulated 3×) to a pointer; shrank the §9 embed-vs-reference contract restatement to a pointer (kept the operational checklists); repointed all §4.3/§5.6/§6/§8/§9 citations to the Layer-B files. Path-1/Path-2 input flows kept in **both** blessed homes (deliver-agent "Workflow inputs" = operational SoT; collaboration-guide §3 = conceptual). research-agent Constitution §1 paraphrase kept (role-framing).
5. `bbd385e` — **framework-template seed**: created `framework-template/README.md` (the manifest) + `framework-template/A-constitution/constitution.template.md` (Layer-A skeleton).
6. `db8c022` — **genericization pass on `constitution.template.md`** (addresses Codex F1–F10 + 2 extra body leaks found in validation: §3.3 `human_review_required`; §1.7/§5.3 "shadow" → "held-out generalization"). Post-pass file = **414 lines, 25 `<<PROJECT:>>` slots, 20 `e.g. (csagent)` comments**; both body-leak grep checks empty (no concrete csagent token in portable prose).

`framework-template/` currently contains ONLY `README.md` + `A-constitution/constitution.template.md` (plus a stray `.DS_Store`). **B/C/D not built yet.**

**Codex review of the Layer-A skeleton** (run via `codex exec -s read-only`, model **gpt-5.5**): verdict = *needs another genericization pass*. 10 findings F1–F10:
- F1 §1.2 primary principle too strong to be unmarked. F2 §3.1 fix-layer "must-include" smuggles csagent taxonomy. F3 §3.2 ordering bakes in csagent diagnosis path. F4 §5.1/§7.1 target/neighbor/negative/shadow is a specific eval topology. F5 §5.4 leaks `eval_spec`. F6 §5.5 smoke-demotion asserted as generic rule. F7 §5.6 "bad-case suite" as primary gate is csagent-shaped. F8 §6 arch-health asserted as portable bar though §5.1 only lists it as example. F9 §4.1 "nine-question" frozen count. F10 §2 "deliver agent" / "bad-case pipeline" role names assumed.

**Working-tree note (2026-06-06):** `docs/codex-findings.md` is modified — that is **in-flight M-Auto-6 review work, NOT this task; do not touch or commit it**. This context pack is the only other new file.

## 决策记录 (Decision record)

- **Layer A keeps section numbers stable + pointer stubs** → near-zero blast radius (archives never edited). Adopted; proven (citations resolve).
- **Template placeholder convention:** `<<PROJECT: description>>` = a project-specific slot a new project fills; `<!-- e.g. (csagent): … -->` = example comment deleted on instantiation; **unmarked prose = portable-verbatim**. Completeness gate: `grep -r '<<PROJECT:'` over an instantiated project must be empty.
- **Portability classes:** PORTABLE-VERBATIM / PORTABLE-SKELETON / PROJECT-SPECIFIC (see README §4).
- **csagent role names** (dev/deliver/review/research) are the framework's role registry (Layer B); in Layer A they're referenced functionally with a one-line registry note.
- **Codex confirmed valid**; the genericization pass implements its F1–F10 recommendations.
- **Two-commit / specific-file-staging discipline** used throughout (never `git add -A`).
- **Framework lives in-repo** at `framework-template/` first (extract to its own repo later — a non-goal for now).

## 当前任务 (Current task)

The Layer-A genericization is **committed** (`db8c022`); the framework-template build-out is **paused** here. Between 2026-06-02 and 2026-06-06 the team continued csagent's own runtime/UI work (milestone **M-Auto-6**, Sprints 078–080) — unrelated to this framework task. The only still-open item from this task: **optionally re-send the genericized skeleton to Codex for a read-only confirm pass** (no confirm commit exists yet).

## 下一步 (Next steps)

1. Resolve the two pending decisions above (commit + optional Codex confirm).
2. **Continue framework-template build-out** (all TBD per README §2 target tree): `INSTANTIATE.md`, `AGENTS.template.md`, the **Layer B** files (`B-process/*` incl. `roles/*`), **Layer C** ledger skeletons (`C-state-ledgers/*`), **Layer D** prompt skeletons (`D-prompt-artifacts/*`), `examples/csagent-reference.md`. Source each by genericizing the csagent doc named in the README §8 mapping table; apply the SAME placeholder/example/body-leak discipline; run the validation greps.
3. **Deferred (do not do now):**
   - The optional ~2.9 KB `action_bank.md` Sprint 20/21/22/23 narrative-intro trim → fold into the **M-Auto-3 close** sweep (normal cadence).
   - Repointing the **regenerated contracts** (`docs/milestone_objective.md`, `docs/sprint_objective.md`, `docs/codex-findings.md`) → they adopt the new Layer-B citation targets on their **next regeneration**, not now.

## 注意事项 (Cautions / invariants)

- **Plan → confirm → implement.** The human gates structural work: produce a reviewable plan, get explicit approval, then implement + show validation evidence. Surface ambiguity; don't guess.
- **Scope fences for the framework task:** do NOT edit live csagent docs; do NOT touch `docs/sprints/*`, `docs/milestones/*`, `docs/archive/*`, `compact/*` (archives are immutable); do NOT create files outside the explicitly-approved scope.
- **Move, don't delete.** Every removed passage must land in an archive or be preserved as an `e.g. (csagent)` comment (round-trip fidelity).
- **Body-leak rule for template files:** no concrete csagent token (`eval_spec`, `java_guard`, `prompt_projection`, `skill_state`, `semantic_planner`, `judge_calibration`, `human_review_required`, `product_policy`, "shadow", "smoke", "deliver agent", "CaseSpec", "nine-question") may appear in portable prose — only inside `<!-- -->` comments or `<<PROJECT:>>` slots. Validation greps used (run on the file):
  - `grep -ni 'nine-question'` → empty
  - `grep -nE '<concrete layer names>'` → every hit on a comment/slot line
  - `grep -niE 'shadow|deliver agent|bad-case pipeline|smoke|CaseSpec|neighbor'` (minus comment/slot lines) → empty
  - `grep -c 'e.g. (csagent)'` should not drop (round-trip); `grep -c '<<PROJECT:'` should reflect added slots
- **Verify repo state after any interrupt.** Earlier this session, an execution agent appeared "rejected/interrupted" but had already committed (`6121f69`) and staged edits before the interrupt landed; this caused a wrong "savings don't exist" conclusion. Always run `git log`/`git status` + re-measure (`wc -c`) before assuming nothing ran or redoing work.
- **Trust-but-verify subagent reports.** A diagnosis agent overstated action_bank byte counts (claimed line-59 = 6871 chars; it was ~700 and already swept). Confirm load-bearing claims by direct read before planning cuts.
- **Codex CLI:** available at `~/.npm-global/bin/codex` (v0.134.0), model gpt-5.5. Use `codex exec -s read-only` for review (no edits). Per memory `reference_codex_crs_gateway`: keep `disable_response_storage=true` in `~/.codex/config.toml` (gateway hangs otherwise). Output can be large (saved to a tool-results file); extract the final answer near the end.
- **Repo advanced — verify before continuing.** This pack documents work done 2026-06-02; by 2026-06-06 the branch moved to M-Auto-6 (HEAD `588cc91`, 118 commits ahead of origin). `framework-template/` is unchanged since `db8c022`. Re-check `git log`/`git status` before acting. The working tree's modified `docs/codex-findings.md` is in-flight M-Auto-6 work — leave it alone.
- **Separate concern (NOT this task):** csagent has active runtime/eval/UI work — as of 2026-06-06 the live milestone is **M-Auto-6** (Sprints 078–080); read `docs/10-handoff.md` §0 for current state. The framework-extraction task must stay clear of those live docs.
- **README accuracy:** `framework-template/README.md` is the manifest; it marks every not-yet-created file `(TBD)`. Keep it honest as files land.
