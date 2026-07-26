# Deliver-agent brief — the 2026-07 perf-replan wave is done; here is its state

> Paste this into a deliver-agent session **after** `docs/teams/deliver-activation.md`.
> Self-contained per `prompt-artifact-rules.md` §9.1: everything you need to
> orient is either here or at a path named here.

---

## 0. Read this before you trust the cold-start table

**`docs/10-handoff.md` §0 and `docs/sprint_objective.md` are stale by roughly a
month and describe a different line of work.** §0 is dated 2026-06-22 and says
the active milestone is **M-Auto-12** (autoloop objective-alignment
annotation), with `Sprint 102 / S-Auto-50` as the pending contract.
`sprint_objective.md` is that Sprint 102 draft, still `status: proposal /
not_started`.

None of that happened. What happened instead, on branch
**`perf-replan-2026-07`**, is a four-sprint performance re-plan wave (Sprints
103–106) that is now merged and reviewed. **Bringing §0 and
`sprint_objective.md` back in line with reality is your first task** — it is
your file per `doc_governance.md` ("§0 — Cold-start table: always current.
Deliver-agent replaces it at each sub-sprint or milestone close"), which is why
no dev agent touched it.

Authoritative for this wave, in reading order:

1. `docs/proposals/performance_priority_replan_2026-07.md` — the diagnosis
   (Loops A/B/C), the three product decisions of 2026-07-25 (§2), the seven
   workstreams (§3), and the sequencing (§4).
2. `docs/sprints/sprint-10{3,4,5,6}-handoff.md` — what each sprint actually
   delivered, each with its own "real defects found, out of contract" section.
3. `docs/codex-findings.md` — §4.1 verdicts for 105 and 106 (both
   `pass` / 0 blocking).

## 1. What landed

| sprint | workstream | outcome | merge |
|---|---|---|---|
| 103 | WS-6-A | Runtime re-routing of the use case outside DISCOVER. **Capability landed, outcome did not move.** | on branch before the wave |
| 104 | WS-6-B | DISCOVER clarification budget: the projection now tells the LLM the truth about `remaining` / `counts_toward_used` / `on_exhaustion`, and one mis-stamp is removed. Moved 2 of 3 draws on its target spec; **neither neighbour moved**. Budget itself unchanged — registered as a deferred runtime candidate. | PR #9 → `5f4f7112` |
| 105 | WS-4 + Loop C | The programmatic verdict was pinned at zero corpus-wide; stops scoring an ABSENT judge signal as a FAILED one, adds the D1/D2/D3 containment ladder, adds `rescore` for offline re-scoring of a recorded run. | PR #10 → `b1ffc566` |
| 106 | WS-2 | Finished the CaseSpec expectation rewrite on `promotion/` + `case_families/`: 14 flips, 12 two-stage conversions, 53 reviewed keeps, 2 removals, 7 escalation-trigger corrections. | PR #11 → `e0201801` |

**Sprint 107 / WS-7-A was written but never started.** Its contract is
`compact/sprint-107-dev-prompt.md`: can the deterministic Java replay harness be
revived, and what replaces the lost human ground truth. It is investigation-first
and its acceptable outcome includes "not worth reviving". No branch, no worktree.

## 2. Baselines as of `4931351f` on `perf-replan-2026-07`

| gate | value | note |
|---|---|---|
| `eval_interactive` pytest | **13 failed / 796 passed / 5 skipped** | was 14 failed / 764 passed before the wave. 105 fixed the stale Skill-count assertion (−1 failure) and added ~32 tests. **All 13 remaining failures are the missing `data/human_review_annotations_2026-04-22_golden.csv`** — unowned, see §4. |
| corpus load | **484 specs, 0 failures** | was 486; Sprint 106 removed 2 (§3). |
| `promotion/` lint | 99 specs, **40 R1 errors**, 1 R11 warning | every one of the 40 is a reviewed deliberate keep — see §4 item 2. |
| `server` mvn | **1503 run / 1 failure / 0 errors / 2 skipped** | re-measured on the merged tree, not copied from a handoff. Was 1493/1 before the wave; 104 added +10 and is the only sprint that touched `server/**`. The single failure is the inherited `SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly` — same one as before the wave, 0 self-introduced across all four sprints. |

**Worktree trap:** `data/*` is gitignored, so a fresh `git worktree` measures
one more pytest failure than the primary checkout, and cannot resolve
`data/filtered/*` at all. Copy or symlink `data/` into any new worktree before
measuring anything.

## 3. Two product-owner decisions that need a governance home

Both were taken by the human on 2026-07-26 and are currently recorded **only in
a sprint archive**, which is the wrong tier for a standing rule. Giving them a
durable home is a deliver task.

### 3.1 The corpus-cleaning principle

The eval corpus's raw material is real, human-handled sessions with complete
chat logs, but **everything layered on top — persona, `user_goal_summary`,
`hidden_facts`, `expected`, drift labels — was manufactured by an LLM pipeline
under partial human review**. A self-contradicting spec is therefore more likely
a generation artefact than a finding about the product. Routing, first match
wins:

1. the source transcript settles it → **fix the spec against the transcript**;
2. nothing can settle it, or a purely authored spec contradicts itself →
   **remove it from eval scope**, with a recorded ledger entry;
3. the answer turns on a product or compliance line nobody has drawn →
   **remove it**; do not carry it as a permanent lint error.

What makes branch 1 the default rather than a nice idea: **381 of 484 specs
derive from a real session, and every `source_session_id` resolves against
`data/eval_datasets/*_turns.csv` + `data/filtered/*` — 32,823 ids indexed from
45 CSVs, zero orphans.** "The generator got this wrong" is a checkable claim in
this repo. Recorded at `docs/sprints/sprint-106-handoff.md` §11.1–§11.2.

This does **not** license deleting a case the bot merely fails — that is §5.4
in reverse.

### 3.2 The generator is frozen; the corpus is a curated artefact

`case_spec/case_outcome_resolver.py:196-206` and `transcript_evidence.py:146-208`
still read "a human agent took this session over" as "the bot should escalate",
and the corpus is by definition sessions that reached a human. A regeneration
would silently revert WS-2's 80 rewritten specs and Sprint 106's 79. The
decision: **stop regenerating.** The generator may propose new cases; it may not
overwrite adjudicated ones, and a new case enters only after human or
transcript-backed review.

**This is currently protected by convention only.** The cheapest guard: the
generator refuses to overwrite any spec carrying an `expectation_revision_note`,
a field already present on all 79 adjudicated specs. That is a change to the
Python package. Recorded at `sprint-106-handoff.md` §11.6.

## 4. Open items, unowned

Grouped by what they block, not by which sprint found them. Every one is
sourced; none is invented here.

**Blocks reading any `promotion/` pass-rate**

1. **12 two-stage specs sit in programmatic buckets.** `promotion/` and
   `case_families/` resolve to `case_passed_authority: "programmatic"`
   (`batch/executor.py:48-70`), so escalate-after-grounded-help lands
   `CONDITIONAL_ELIGIBLE` and scores 0.0 until a per-trace entry exists in
   `case_specs/conditional_outcome_adjudications.yaml`. Deliberate — it refuses
   to auto-pay 1.0 for a handover — but it needs an adjudication owner.
   (106 §7.3; Codex flagged it again as a measurement caveat.)

**Baseline rot**

2. **R1 can no longer be read as a backlog.** 53 R1 errors across `promotion/`
   and `case_families/` are reviewed deliberate keeps, indistinguishable from
   unreviewed specs in the count. Proposed shape: a per-spec
   `lint_waiver: {rule, reason, reviewed}` that the linter downgrades to a
   warning. Python-package change. (106 §3.3, §7.2.)
3. **`data/human_review_annotations_2026-04-22_golden.csv` is missing from every
   checkout**, costing 13 permanently-red tests that no sprint owns. Regenerate,
   or make those tests skip on absence — the decision is unowned. (105 §7.3;
   also Sprint 107's Q2.)

**Corpus residue Sprint 106 could not reach**

4. `anchor/cs_interactive_132`, `145`, `146` still expect
   `clarification_budget_exhausted` — the reason `BudgetChecker.java:32-37`
   stamps without ever invoking the LLM. Same defect 106 fixed on its own seven.
   (106 §11.4.)
5. `anchor/cs_interactive_239`, `exploration/cs_interactive_171`,
   `exploration/cs_interactive_365` carry competitor brand names that were never
   checked against their transcripts. Two of the six found corpus-wide turned
   out to be faithful customer speech, so this is a check, not a defect list.
   (106 §11.5.)
6. `cs_interactive_227`'s transcript ends with the human agent resolving by
   explanation after escalating — the strongest candidate in `promotion/` for a
   flip to resolve on transcript evidence. Not re-opened; the trigger was the
   sanctioned task. (106 §11.8, seconded by Codex.)
7. **WS-2 is not finished**: `bad_cases/` still has 16 R1 violations and
   `anchor/` has 20 residual.

**Runtime, deferred by name**

8. **The per-issue clarification budget.** 104's evidence is the case for it —
   the budget is per-session while the need is per-issue — and
   `runtime_freeze_and_risk_policy.md` §1.3.2 defers it explicitly, requiring a
   new objective doc and a new runtime sprint. 104 says this is the sprint that
   should open next on this failure shape. (104 §5.2.)
9. **`turn_budget_exhausted` has no correct replacement in the frozen enum** for
   the per-turn tool-step budget; the runtime contradicts its own prompt on
   every MAX_STEPS exit. 104 proposes `tool_step_budget_exhausted` at priority
   43 but did not implement it — the enum is frozen and the change cross-cuts
   the eval side, so it needs the coordinated migration §1.3.5 requires.
   (104 §5.1.)
10. **Two of 17 recorded sessions died on a backend HTTP 500** — ~12% of the
    recorded substrate unmeasured for infra reasons, cause unexplained.
    (105 §7.4.) Failure brief:
    `docs/diagnostics/failure-briefs/sprint-105-2026-07-26-backend-500-unmeasured-sessions.md`.
11. **`results.json` drops 8 of 10 `TurnTrace` fields plus `events` and
    `handover`**, so full L1/L2 offline re-scoring is impossible; 105 fixed only
    the `severity` gap that blocked composite re-scoring. (105 §7.5.)
12. **The backend cannot start from a clean checkout without an undocumented
    `DB_PORT=5442`** — Flyway fails with a misleading auth error. (104 §5.4.)

**Method that should outlive the sprint**

13. The transcript-adjudication scripts used to settle Sprint 106's decisions
    (index `source_session_id` → `data/*_turns.csv`, dump spec-vs-transcript
    side by side) live in a session scratchpad and will be lost. If
    transcript-backed adjudication is now the standard method — and §3.1 says it
    is — it belongs in `eval_interactive/` as a supported command.
    (106 §11.8 item 5.)

## 5. What I suggest you do first, and what not to do

Suggested order — but scope is yours and the human's, not this brief's:

1. **Close maintenance**: rewrite `10-handoff.md` §0 for the perf-replan wave,
   archive the stale Sprint 102 `sprint_objective.md`, and file the §4 items
   into `action_bank.md` so they stop living in four separate handoffs.
2. **Give §3.1 and §3.2 a governance home**, and decide whether the generator
   guard (§3.2) is worth a small Python sub-sprint. Until it exists, the freeze
   is convention.
3. **Then** decide the next scope with the human. The obvious candidates are
   Sprint 107 / WS-7-A as written, the per-issue clarification budget (§4.8),
   and the WS-2 remainder (§4.7).

Do not:

- **Do not re-open the 79 adjudicated specs** without transcript evidence.
  Every one carries an `expectation_revision_note` recording the reasoning;
  re-litigating them is the churn §3.1 exists to stop.
- **Do not regenerate the corpus** (§3.2).
- **Do not read a `promotion/` pass-rate** as meaningful until §4.1 has an
  owner.
- **Do not treat the residual R1 count as a backlog** until §4.2 lands.
