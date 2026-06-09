# Deliver-agent context handoff — Sprint 45 / S-Eval-4 close (A — Clean PASS) → Sprint 46 / S-Eval-5 launch

**Authored:** 2026-05-22 by deliver-agent (Sprint 45 S-Eval-4 close session, post-classification, post-S-Eval-5-launch-draft)
**For:** the next deliver-agent instance picking up at S-Eval-5 dev dispatch OR S-Eval-5 close OR M3-Eval milestone close
**Branch:** `refactor/remove-the-shackles`
**Repo:** `/Users/caoruixin/projects/csagent-latest`
**HEAD at this handoff authoring:** `8b7ff40` (Sprint 45 / S-Eval-4 dev commit). At this handoff authoring, the deliver-agent close-out bundle is STAGED in working tree (NOT yet committed by human). Human will bundle + commit; HEAD will advance.

Read order on cold start: this file → `AGENTS.md` (auto-loads `iteration_governance.md` + `doc_governance.md` + `agent_context_guide.md`) → `compact/sprint-deliver-orchestrator.md` (deliver-agent role definition — long-term doctrine) → `docs/milestone_objective.md` (M3-Eval north star) → `docs/sprint_objective.md` (live S-Eval-5 contract; ~415 lines; 12 sections) → `docs/sprints/sprint-045-handoff.md` (S-Eval-4 dev archive + §12 closure verdict the deliver-agent + human jointly filled) → `compact/sprint-046-dev-prompt.md` (S-Eval-5 dev brief drafted but NOT committed at this handoff authoring) → `compact/context-handoff-sprint-044-post-close.md` (PRIOR cross-session handoff at S-Eval-3 close; cumulative carry-overs).

---

## 1. 背景 (Background)

- Repo builds a customer-service agent for an online classifieds marketplace. LLM-first; Java/Python runtime owns deterministic boundaries.
- Governance chain (auto-loaded via `@AGENTS.md`): `docs/current/doc_governance.md` + `docs/current/agent_context_guide.md` + `docs/current/iteration_governance.md` (Constitution §1 / Failure Brief §2 / Fix Layer §3 / Anti-Hardcode §4 / Eval Acceptance §5 incl. §5.5 smoke→observation + §5.6 / §5.6.1 / §5.6.2 / §5.6.3 bad-case suite as human-judgment gate / Architecture Health §6 / §7 stanza / **§8 Milestone framework**).
- Multi-agent collaboration: Human → Research-agent (proposes) → Deliver-agent (plans + drafts contracts + prompts) → Dev-agent Claude Code (implements) → Review-agent Codex (reviews); agents do NOT share chat history; cross-session continuity via repo docs + compact handoff files.
- Active milestone: **M3-Eval — Coarse-to-Fine Evaluation Architecture** (third milestone under §8 framework; M1 closed 2026-05-17, M2 closed 2026-05-18, M3-Eval IN PROGRESS as of 2026-05-22; **4 of 5 sub-sprints CLOSED Clean PASS; 1 remaining — S-Eval-5**).

## 2. 目标 (Goal)

### Milestone-level (M3-Eval, 5 sub-sprints; 4 of 5 CLOSED)

Replace implicit L1/L2/L3 equal-weighted eval scoring with a **four-tier pyramid**:

- **Tier-0** safety floor (unchanged hard gate).
- **Tier-1** outcome — bad-case suite manual review (PRIMARY GATE per §5.6) + outcome-only `anchor_outcome` suite + (NEW S-Eval-5) supplementary L3 `user_goal_achievement` dim.
- **Tier-2** per-UC critical-flow correctness via Skill `critical_steps[]` (shipped at S-Eval-2 schema + S-Eval-3 content close 2026-05-22; populated 18 steps; mandatory:advisory = 10:8). **Tier-2 is structurally INERT in production eval-harness path** because `executor.py:252` does NOT pass `tier2_result=` to `compute_composite(...)` (OQ-S44.1 + OQ-S45.5 carry-over).
- **Tier-3** advisory only (post-S-Eval-5: relevance / tone_appropriateness / groundedness demoted from composite to Tier-3 advisory; new tiered pyramid landed).

**Single-source-of-truth property** (proposal §5.2): `critical_steps[].desc` consumed by BOTH runtime LLM AND eval-side scoring. Same YAML; two consumers. Shipped at S-Eval-3.

### Current sub-sprint (Sprint 46 / S-Eval-5 — LAST M3-Eval sub-sprint)

S-Eval-5 ships: (a) demote 3 current L3 dims (`relevance`, `tone_appropriateness`, `groundedness`) from composite to Tier-3 advisory; (b) add NEW `user_goal_achievement` L3 dim as Tier-1 supplementary advisory; (c) update rubric prompts to close R-l3-judge-form-context-trust + R-l1-source-citation-quality; (d) close 4 M3-Eval R-items in action_bank; (e) **monotone-relaxing check** (no PASS → FAIL transitions across smoke + anchor + anchor_outcome). Layer `eval_spec` (judge config + rubric narrative). **Codex review deferred to M3-Eval milestone-shared close** per §4.3 default. Estimated 2-3 dev-days.

**LOAD-BEARING planning-round decision** (per `docs/sprint_objective.md` §2.6): **Option A AUTHORIZED at S-Eval-5 planning round 2026-05-22 (human direction)** — land the executor wiring fix at `executor.py:252` as part of the S-Eval-5 bundle (one-line edit + 1-2 NEW tests). Rationale: the S-Eval-5 monotone-relaxing rerun doubles as the first real-LLM Tier-2 surface (consumes OQ-S44.5 + OQ-S44.3+S44.4 + OQ-S45.5+S45.6 simultaneously); avoids the degenerate M3-Eval close state where milestone-shared Codex review would have zero real-LLM Tier-2 evidence to evaluate.

## 3. 已确认事实 (Confirmed facts)

### Recent commits (chronological)

```
8b7ff40  sprint 45 / S-Eval-4: bad-case suite expansion + trial milestone-close dry-run (NEW M3-Eval sub-sprint 4)  ← HEAD at this handoff authoring
4dafaf5  docs: S-Eval-3 close (A — Clean PASS) + S-Eval-4 launch + per-sub-sprint Codex pass archived
01770ac  sprint 44 / S-Eval-3: populate critical_steps for the 6 Skills (NEW M3-Eval sub-sprint 3)
db19a47  docs: S-Eval-2 close (A — Clean PASS) + S-Eval-3 launch + v0_2 supersession landing
69ed77f  sprint 43 / S-Eval-2: Skill critical_steps schema + extractor + projection wiring
357e949  docs: S-Eval-1 close (A — Clean PASS) + M3-Eval setup-bundle (deliver-agent)
d91bd3d  sprint 42 / S-Eval-1: schema simplification + anchor_outcome suite
6ceae7c  上传知识库和 mock data
```

### Working tree state at handoff authoring (HEAD `8b7ff40`; deliver-agent close-out + S-Eval-5 launch bundle STAGED)

```
M  docs/sprints/sprint-045-handoff.md                      (§12 closure verdict appended)
M  docs/sprint_objective.md                                (REPLACED with S-Eval-5 contract; supersedes sprint-045-objective.md)
M  docs/10-handoff.md                                      (§1 lead refreshed for S-Eval-4 → S-Eval-5 transition)
M  docs/action_bank.md                                     (Sprint 45 close-action row appended to §6; NEW R-item R-bad-case-suite-uc-ghij-seed-from-real-sessions opened in §5)
?? compact/sprint-046-dev-prompt.md                        (NEW; S-Eval-5 dev brief)
?? compact/context-handoff-sprint-045-post-close.md        (THIS FILE; NEW; cross-session continuity for next deliver-agent)
?? docs/sprints/sprint-045-objective.md                    (NEW; S-Eval-4 contract archive)
   docs/codex-findings.md                                  (UNCHANGED — stays scaffold; no per-sub-sprint Codex archive for S-Eval-4 since Codex deferred to M3-Eval close)
```

Suggested human commit message:
```
docs: S-Eval-4 close (A — Clean PASS) + S-Eval-5 launch + R-bad-case-suite-uc-ghij-seed-from-real-sessions opened
```

### Baselines at S-Eval-4 close (deliver-agent independently re-reproduced 2026-05-22)

- **Java**: `Tests run: 1163, Failures: 1, Errors: 0, Skipped: 2` UNCHANGED (S-Eval-4 added 0 Java code; verified by construction).
- **Python**: `5 failed, 396 passed in 13.70s` via `uv run python -m pytest` UNCHANGED from S-Eval-3 close baseline (5 pre-existing failure classes preserved; net Python delta = 0 — checkpoint-test update 1 → 12 restored parity after the 1 transient post-YAML-add failure). **MUST cite via `uv run python -m pytest`** per OQ-S44.6 carry-over.
- **Bad-case suite size at close**: **12** (1 Alice + 11 new S-Eval-4). Per-primary-UC: `{UC-A: 2, UC-C: 3, UC-FP: 3, UC-D: 2, UC-K: 2}`. UC-G/H/I/J primary = 0 (documented gap; new R-item opened).
- **Source pool**: `grep -c "status: approved" eval_interactive/case_spec_overrides.yaml` = **17** (reconciled vs proposal §4 mistake of 29).

### S-Eval-4 dev artefacts (commit `8b7ff40`; 14 files; +1295 / -2)

- **11 NEW bad-case YAMLs** (921 lines): `cs012` UC-FP / `cs015` UC-FP / `cs066` UC-K / `cs029` UC-D / `cs095` UC-D / `cs011` UC-C D4 / `cs014` UC-C D4 / `cs001` UC-C D4 / `wmkb` UC-A UC-H-secondary / `iwzx` UC-K / `fg5q` UC-FP. Tier distribution: core × 7, scope-relevant × 4.
- **D-dimension coverage**: D1 × 9, D2 × 2 (cs066/iwzx), **D3 × 0** (documented GAP — Alice retains D3), D4 × 3 (cs011/cs014/cs001).
- **`_manifest.md` EDIT** (+66): 11 lifecycle ledger rows + S-Eval-4 trial dry-run calibration notes section.
- **§7-a planned-drift checkpoint test EDIT** (+11/-2): `test_s_eval_1_schema_and_scoring.py` count assertion 1 → 12 per S-Eval-3 precedent.
- **657-line dev handoff** at `docs/sprints/sprint-045-handoff.md`.
- **Numbers-cite slip**: handoff §3 + §11 say "13 files" but actual is 14 (under-counted itself; fourth+ instance of pattern; fold-back candidate at M3-Eval close).

### S-Eval-4 verdict + OQ dispositions

- **Classification: A — Clean PASS** (deliver-agent + human, 2026-05-22). No Codex per-sub-sprint review (deferred to M3-Eval close per §4.3 default).
- **Real-LLM run posture**: **Option A (synthetic / mocked trace)** authorized by deliver-agent + human at S-Eval-4 planning round 2026-05-22 (rationale per dev §9).
- **Fallback path (a)** for UC-G/H/I/J gap authorized at planning round (accept narrower coverage).
- **6 OQs disposed non-blocking** (4 NEW + 2 carry-overs from S-Eval-3):
  - **OQ-S45.1** (3 legacy-migrated low-confidence): routed to M3-Eval close manual review queue.
  - **OQ-S45.2** (UC-G/H/I/J gap): NEW R-item `R-bad-case-suite-uc-ghij-seed-from-real-sessions` OPENED in `docs/action_bank.md` §5 (status `proposed; impl deferred to M4+ planning`).
  - **OQ-S45.3** (cs095 + anchor_outcome session co-use intentional dual-encoding): flagged for M3-Eval close confirmation.
  - **OQ-S45.4** (cs095 placeholder framing): fold-back candidate for future authoring.
  - **OQ-S45.5** (carry-over OQ-S44.1+S44.2 executor wiring still inert): PRESERVED in M3-Eval-shared review queue + S-Eval-5 planning round decision (Option A / B).
  - **OQ-S45.6** (carry-over OQ-S44.3+S44.4 UC-FP moderation calibration): PRESERVED in M3-Eval-shared review queue; calibration via real-LLM run at S-Eval-5 (if Option A taken) OR M3-Eval close.

### S-Eval-5 carry-overs CONSUMED at S-Eval-5 planning round 2026-05-22 (LOAD-BEARING)

1. **OQ-S45.5 executor wiring decision — DECIDED: Option A AUTHORIZED**. `executor.py:252` fix lands in S-Eval-5 bundle (one-line edit + 1-2 NEW tests). Monotone-relaxing rerun doubles as first real-LLM Tier-2 surface.
2. **OQ-S45.6 UC-FP moderation calibration** — calibration surface IS the S-Eval-5 monotone-relaxing rerun (via Option A). If a legitimate UC-FP no-context path surfaces (escalates / defers instead of answering), STOP per S-Eval-5 contract §10 #2 — deliver-agent + human decide S-Eval-3 fix-iteration (loosen `trace_check` OR downgrade severity to advisory).
3. **OQ-S44.6 Python baseline runner discipline** — preserved; dev MUST use `uv run python -m pytest`.

### M3-Eval-shared Codex review queue (at S-Eval-4 close)

To be addressed at M3-Eval milestone-shared Codex review (NOT S-Eval-5 close):
1. **OQ-S42.3** (S-Eval-1 D-2.2 demotion interpretation).
2. **OQ-S43.1** (S-Eval-2 DSL parser blocklist surface).
3. **OQ-S43.4** (S-Eval-2 `tool_event_seq` unsuccessful-dispatch `success=false` ignored).
4. **OQ-S44.6** (Python baseline runner-invocation confirmation — `uv run python -m pytest` vs `uv run pytest`).
5. **OQ-S45.1** (3 legacy-migrated bad cases low-confidence — real-LLM evidence needed).
6. **OQ-S45.3** (cs095 dual-encoding intentional pattern confirmation).
7. **OQ-S45.4** (cs095 placeholder framing fold-back).
8. **OQ-S45.5** (executor wiring carry-over if Option B taken at S-Eval-5 planning).
9. **OQ-S45.6** (UC-FP moderation calibration carry-over if Option B taken).

### M3-Eval bad-case suite at S-Eval-4 close

Total 12 cases: 1 Alice (UC-A↔UC-H mis-classification regression guard; D1 + D3) + 11 S-Eval-4 new (per §3 above).

**Primary acceptance gate at M3-Eval close** per `iteration_governance.md` §5.6: deliver-agent + human walk through each case's trace at M3-Eval close + judge PASS / FAIL / IMPROVING against `closure_criterion`. Human-judgment gate, NOT programmatic.

## 4. 决策记录 (Decision records)

### M3-Eval launch + S-Eval-1/2/3 closes (already captured in `compact/context-handoff-sprint-044-post-close.md` §4)

Refer to prior cross-session handoff for full S-Eval-1 / S-Eval-2 / S-Eval-3 close decision records.

### S-Eval-4 close (2026-05-22)

- **Classification: A — Clean PASS**. Codex deferred to M3-Eval close.
- 11 new bad-case YAMLs landed within §2 envelope (10-12); at lower edge; UC-G/H/I/J gap surfaced + fallback (a) authorized + new R-item opened.
- Option A (synthetic) dry-run posture; 11/11 closure_criterion well-calibrated.
- §7-a planned drift checkpoint-test update accepted.
- 6 OQs disposed; carry-overs to S-Eval-5 + M3-Eval close documented.
- Minor numbers-cite slip (13 vs 14 files; fourth+ instance of pattern; fold-back candidate at M3-Eval close).

## 5. 当前任务 (Current task)

**Stage**: S-Eval-4 close-out + S-Eval-5 launch bundle STAGED in working tree; awaiting human commit + S-Eval-5 dev launch.

Deliver-agent close-out work at S-Eval-4 close — ALL COMPLETE:

- ✅ §12 closure verdict appended to `docs/sprints/sprint-045-handoff.md`.
- ✅ `docs/sprints/sprint-045-objective.md` archive created.
- ✅ Live `docs/codex-findings.md` STAYS scaffold (no per-sub-sprint Codex archive for S-Eval-4).
- ✅ Live `docs/sprint_objective.md` REPLACED with full S-Eval-5 contract (~415 lines).
- ✅ `docs/10-handoff.md` §1 lead REFRESHED (S-Eval-4 → S-Eval-5 transition).
- ✅ `docs/action_bank.md` §6 Sprint 45 close-action row APPENDED + NEW R-item `R-bad-case-suite-uc-ghij-seed-from-real-sessions` OPENED in §5.
- ✅ NEW `compact/sprint-046-dev-prompt.md` drafted.
- ✅ NEW `compact/context-handoff-sprint-045-post-close.md` (this file).

Pending (NOT deliver-agent work):

- ⏸ **Human commits the bundle** (suggested commit message above).
- ⏸ **Human dispatches Claude Code** dev session against `compact/sprint-046-dev-prompt.md` for S-Eval-5 implementation.
- ✅ **S-Eval-5 planning round decision DECIDED 2026-05-22**: Option A AUTHORIZED — executor wiring fix at `executor.py:252` lands in S-Eval-5 bundle (per `docs/sprint_objective.md` §2.6 + `compact/sprint-046-dev-prompt.md` §2.2).
- ⏸ **Dev session** ships S-Eval-5 bundle (~6-10 files; L3 dim repositioning + new dim + rubric updates + R-item closures + monotone-relaxing tests + executor.py:252 wiring fix + handoff).
- ⏸ **S-Eval-5 close cycle** (deliver-agent + human classify; Codex deferred to M3-Eval close).
- ⏸ **M3-Eval milestone close** (milestone-shared Codex review + bad-case suite manual review as PRIMARY GATE + R-item closures + milestone archive).

## 6. 下一步 (Next steps)

### Immediate (human cycle)

1. **Human commits the close-out + launch bundle** (suggested message above).
2. **Human dispatches Claude Code** dev session against `compact/sprint-046-dev-prompt.md`.
3. ~~Planning-round decision on Option A / B executor wiring~~ — **ALREADY DECIDED 2026-05-22: Option A AUTHORIZED**; the wiring fix is locked into the contract + dev prompt.

### S-Eval-5 dev session (Claude Code; estimated 2-3 dev-days)

4. Dev consumes dev prompt; reads contract + milestone_objective + carry-overs.
5. Dev reads contract §2.6 + dev prompt §2.2 — Option A AUTHORIZED; executor wiring at `executor.py:252` IS in scope (no further authorization needed).
6. Dev implements: 3 L3 dim demotion + NEW `user_goal_achievement` dim + 2 rubric prompt updates + 4 R-item closures + monotone-relaxing tests + REQUIRED executor wiring fix (`executor.py:252` + 1-2 NEW tests).
7. Dev writes handoff `docs/sprints/sprint-046-handoff.md` (LEAVES §12 EMPTY).
8. Dev commits one bundle (~5-9 files).

### S-Eval-5 close cycle (deliver-agent + human; Codex deferred to M3-Eval close)

9. Deliver-agent + human review handoff: A / A-with-X / B-fix-iterate per §4.1. Codex NOT dispatched.
10. If A or A-with-X: deliver-agent close-out bundle (handoff §12 + sprint-046-objective archive + 10-handoff §1 refresh + action_bank Sprint 46 row + 4 R-item closure annotations + post-close context handoff). NO new dev prompt (next is M3-Eval close, not a new sub-sprint).
11. If B (fix-iterate): scope fix-iteration sub-sprint OR M3-Eval close fix; do NOT silently fix-and-continue.

### M3-Eval milestone close (after S-Eval-5 close)

12. **Bad-case suite manual review** as PRIMARY GATE per §5.6 (deliver-agent + human walk through 12 cases; judge PASS / FAIL / IMPROVING; consider OQ-S45.1 legacy-migrated cases real-LLM evidence; consider OQ-S45.3 dual-encoding intentional pattern).
13. **Milestone-shared Codex review** per §4.3 default. Deliver-agent drafts `compact/M3-Eval-review-prompt.md` covering cumulative S-Eval-1 → S-Eval-5 commit range + 9-item M3-Eval-shared review queue.
14. **Codex writes verdict** to `docs/codex-findings.md`.
15. **Deliver-agent + human classify** per §4.1 verdict set + §4.2 4-line header.
16. **M3-Eval close artefacts** (per `iteration_governance.md` §8.4): milestone objective archive to `docs/milestones/M3-Eval_objective.md` (with §12 closure verdict); Codex archive to `docs/milestones/M3-Eval_codex-review.md` (delete-and-add from codex-findings); §6.5 closed-milestone index row in action_bank; 10-handoff §1 lead refresh (demote M3-Eval to Preceding milestone; set next as Current); 4 R-item closure annotations + 1 unblocked-not-closed R-item documented; `R-bad-case-suite-uc-ghij-seed-from-real-sessions` deferred to M4+ planning.
17. **M4+ candidate selection** (per §8.4 + M3-Eval close planning round): pick from remaining slate (M3-B Single Handover Orchestrator if Salesforce calendar pressure / M3-Latency / M3-Skill-Tuning / M3-Tier-0 re-evaluation / M3-cleanup / NEW UC-G/H/I/J bad-case seeding milestone / M3-Corpus separate parallel-track).

## 7. 注意事项 (Caveats / red lines)

### S-Eval-5 specifics

- **LOAD-BEARING planning-round decision DECIDED 2026-05-22**: **Option A AUTHORIZED** — executor wiring fix at `executor.py:252` lands in S-Eval-5 bundle; S-Eval-5 rerun doubles as first real-LLM Tier-2 surface.
- **Monotone-relaxing check is the gating test**. Any `case_passed=true → false` transition triggers STOP per §10 #5 — do NOT silently force rubric to preserve PASS state.
- **Rubric updates use additive trust signals + tighter requirements, NOT keyword/regex/per-UC-matrix encoding**. R-l3-judge-form-context-trust ("`form_context.first_name` is trusted; don't penalise greeting by first name") + R-l1-source-citation-quality ("citation needs canonical_url OR article title; reject bare Salesforce IDs as sole citation"). Surface narrative wording in handoff §5 for deliver-agent + human + Codex review.
- **4 R-item closures**: R-l3-judge-form-context-trust-rubric + R-l1-source-citation-quality-rubric (closed via §2.5 rubric updates) + R-cs040-l3-review-intake-completion-semantics + R-cs038-l3-review-intake-efficiency (closed via S-Eval-1 schema simplification route + new tiered architecture). DO NOT delete R-item entries; append `succeeded-by` annotations only.
- **`R-bad-case-suite-uc-ghij-seed-from-real-sessions`** (NEW at S-Eval-4 close) — NOT S-Eval-5 scope. Stays open in §5 governance-track backlog; deferred to M4+ planning.

### Process discipline (carry-over from S-Eval-3 + S-Eval-4)

- **Numbers-cite per `feedback_deliver_agent_cited_numbers_must_be_reproducible.md`**: every count in S-Eval-5 handoff + Sprint 46 action_bank row reproducible. **Python baseline MUST use `uv run python -m pytest`** per OQ-S44.6 carry-over. **§3 numstat sub-tally arithmetic MUST validate by re-summing** per the fourth+ instance fold-back observation (S-Eval-4 §12.10).
- **Bundle policy per `feedback_commit_at_end_bundles_deliver_artefacts.md`**: dev does NOT stage deliver-agent files.
- **Handoff §12 closure verdict ownership per `feedback_handoff_verdict_section_delegation.md`**: dev leaves §12 EMPTY; deliver-agent + human (no Codex at S-Eval-5 since deferred) jointly fill at close.
- **OOSR-with-packaging-note pattern per `feedback_out_of_scope_review_packaging_rollforward.md`**: if findings at M3-Eval close are out-of-M3-Eval-scope, classify as OOSR + roll forward to M4+.
- **No pre-decisions on OQs surfaced to Codex** per `feedback_constitution_discipline_vs_planning_anticipation.md` — at M3-Eval close Codex prompt, surface the M3-Eval-shared queue WITHOUT pre-decisions.

### Hard fences inherited from prior sub-sprints (still ACTIVE for S-Eval-5)

- No new Tier-0 invariant without human-review escalation.
- No edits to runtime semantic surfaces.
- No edits to Skill abstraction (M2 + S-Eval-2 + S-Eval-3 Java; 6 Skill YAMLs; ContextProjectionBuilder.java).
- No edits to Tier-0 / Tier-1 / Tier-2 hard-gate scoring code (`hard_checks.py` / `outcome_checks.py` / `skill_procedure_check.py`).
- No edits to CaseSpec schema + loader (S-Eval-1 territory).
- No edits to case fixtures (smoke / anchor / anchor_outcome / case-families / 12 bad cases / shadow / `case_spec_overrides.yaml`).
- No edits to eval harness / loader / simulator (Option A `executor.py:252` is the ONLY authorised exception if Option A taken).
- No `iteration_governance.md` edit during S-Eval-5 (default; narrow §5 acceptance-bar refinement may be discussed separately).
- No widening of `escalation_reason` enum.

### Carry-over queues at S-Eval-4 close

- **M3-Eval-shared Codex review queue (9 items)** to address at M3-Eval close — see §3 above.
- **M2 close fold-back queue (6 items)** STILL DEFERRED — separate governance commit per `doc_governance.md` cadence.
- **Stale-test R-item** `R-stale-test-escalation-enum-sync-v0_2-to-v0_3-migration` stays `proposed; impl deferred`.
- **NEW R-item** `R-bad-case-suite-uc-ghij-seed-from-real-sessions` (S-Eval-4 close) stays `proposed; impl deferred to M4+ planning`.

### Term definitions (key for the new deliver-agent)

- **§5.3 standard table** (proposal §5.3): 4-row table for evaluating LLM-visible `desc` strings. Used at S-Eval-3 close; Codex 18× row-1 ✅.
- **§5.6 bad-case suite manual review**: PRIMARY ACCEPTANCE GATE at milestone close; human-judgment, NOT programmatic.
- **`bad_case_metadata` + `closure_criterion`**: per `iteration_governance.md` §5.6; each bad-case YAML carries these blocks.
- **`tier: core` vs `scope-relevant`**: per §5.6.1; core re-runs at every milestone close; scope-relevant re-runs only when milestone touches the relevant surface.
- **Monotone-relaxing check** (S-Eval-5 specific): no previously-PASS case flips to FAIL on the new rubric. Run smoke + anchor + anchor_outcome with old vs new scoring code; diff `case_passed` transitions.
- **§4.1 verdict set**: `pass` / `approve with downgrade-to-signal follow-up` / `reject as semantic hardcode` / `needs human architecture decision`.
- **A — Clean PASS** classification: dev shipped contract end-to-end; no §6 hard fence touched; no §10 stop signal fired; §4.1 Q1-Q8 pass + Q9 N/A; no contract drift beyond pre-existing baseline; OQs disposed non-blocking; Codex (if reviewed) returned `pass` first round.

### Key files / paths the new deliver-agent must know

#### Governance + planning (auto-loaded via AGENTS.md)

- `AGENTS.md` / `docs/current/doc_governance.md` / `docs/current/agent_context_guide.md` / `docs/current/iteration_governance.md`

#### Active sub-sprint state

- `docs/milestone_objective.md` (M3-Eval north star)
- `docs/sprint_objective.md` (live S-Eval-5 contract)
- `docs/10-handoff.md` §1 lead (refreshed at this close)
- `docs/codex-findings.md` (scaffold; M3-Eval milestone-shared at close writes here)
- `docs/action_bank.md` (R-items + §6 close-action index; Sprint 45 row appended + NEW R-item §5)
- `compact/sprint-046-dev-prompt.md` (NEW S-Eval-5 dev brief)
- `compact/context-handoff-sprint-045-post-close.md` (THIS FILE)
- `compact/context-handoff-sprint-044-post-close.md` (PRIOR; cumulative S-Eval-3 carry-overs)
- `compact/context-handoff-sprint-044-pre-codex.md` (HISTORICAL; pre-Codex S-Eval-3)

#### Archives (immutable)

- `docs/sprints/sprint-042-objective.md` + `sprint-042-handoff.md` (S-Eval-1)
- `docs/sprints/sprint-043-objective.md` + `sprint-043-handoff.md` (S-Eval-2)
- `docs/sprints/sprint-044-objective.md` + `sprint-044-handoff.md` + `sprint-044-codex-review.md` (S-Eval-3 with per-sub-sprint Codex archive)
- `docs/sprints/sprint-045-objective.md` + `sprint-045-handoff.md` (S-Eval-4; NO Codex archive — deferred to M3-Eval close)
- `docs/milestones/M1_objective.md` + `M2_objective.md` + `M2-Skill_objective.md` (superseded) + `M2_codex-review.md`

#### Bad-case suite (12 cases at S-Eval-4 close; PRIMARY GATE for M3-Eval close per §5.6)

- `eval_interactive/case_specs/bad_cases/alice_uc_a_uc_h_misclass.yaml` (regression guard)
- `eval_interactive/case_specs/bad_cases/cs012_uc_fp_late_phone_failure_path.yaml` + 10 more (S-Eval-4 new)
- `eval_interactive/case_specs/bad_cases/_manifest.md` (lifecycle ledger + S-Eval-4 calibration notes section)
- `eval_interactive/case_spec_overrides.yaml` (17 approved entries source pool; read-only)

#### Code surfaces touched by M3-Eval (S-Eval-1 + S-Eval-2 + S-Eval-3 + S-Eval-4)

- `eval_interactive/eval_interactive/case_spec/schema.py` (S-Eval-1)
- `eval_interactive/eval_interactive/case_spec/loader.py` (S-Eval-1)
- `eval_interactive/eval_interactive/scoring/hard_checks.py` (S-Eval-1)
- `eval_interactive/eval_interactive/scoring/outcome_checks.py` (S-Eval-1)
- `eval_interactive/eval_interactive/scoring/composite.py` (S-Eval-1 + S-Eval-2)
- `eval_interactive/eval_interactive/scoring/skill_procedure_check.py` (S-Eval-2)
- `eval_interactive/case_specs/anchor_outcome/` (S-Eval-1; 12 cases)
- `eval_interactive/case_specs/bad_cases/` (S-Eval-4; 12 cases incl. Alice)
- `eval_interactive/tests/test_s_eval_1_schema_and_scoring.py` (S-Eval-1; checkpoint updated S-Eval-4)
- `eval_interactive/tests/test_skill_procedure_extractor.py` (S-Eval-2; updated S-Eval-3)
- `eval_interactive/tests/test_skill_procedure_dsl_parser_rejects_hardcodes.py` (S-Eval-2)
- `server/src/main/java/com/gumtree/csagent/service/runtime/skill/Skill.java` + `CriticalStep.java` + `SkillLoader.java` (S-Eval-2)
- `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java` (S-Eval-2)
- `server/src/main/resources/skills/<6 YAMLs>` (S-Eval-3 content)

#### S-Eval-5 code surfaces (TBD by dev at planning round)

- `eval_interactive/eval_interactive/scoring/llm_judge.py` — current L3 judge surface; primary edit target
- `eval_interactive/eval_interactive/scoring/composite.py` — possible adjustment for dim-weighting per S-Eval-1 `severity` pattern
- `eval_interactive/eval_interactive/batch/executor.py:252` — Option A executor wiring fix IF authorized

#### Source-of-truth artefacts

- `docs/solutions/m3_eval_milestone_proposal.md` (research-agent proposal; LOAD-BEARING §6 S-Eval-5)
- `docs/runtime_freeze_and_risk_policy.md` §1 + §2 (Tier-0 invariants; UNCHANGED through S-Eval-4)
- `docs/current/customer_service_tool_spec_v0_3.md` (current tool spec)
- `docs/current/faq_grounding_contract.md`

### Deliver-agent memory (referenced but file system location differs)

Same note as previous handoff: the actual filesystem path `/Users/caoruixin/.claude/agent-memory/sprint-deliver-orchestrator/` does NOT exist; the rules referenced throughout governance docs + handoff files ARE the authoritative source. Most relevant rules at S-Eval-5 close + M3-Eval close:

- `feedback_commit_at_end_bundles_deliver_artefacts.md` — dev does NOT stage deliver-agent files.
- `feedback_handoff_verdict_section_delegation.md` — dev leaves handoff §12 EMPTY.
- `feedback_out_of_scope_review_packaging_rollforward.md` — OOSR-with-packaging-note pattern.
- `feedback_deliver_agent_cited_numbers_must_be_reproducible.md` — every numstat reproducible + sub-tally arithmetic re-summed.
- `feedback_constitution_discipline_vs_planning_anticipation.md` — surface OQs to Codex WITHOUT pre-decisions.
- `feedback_packaging_codex_findings_supersession.md` — delete-and-add pattern for codex-findings at archive (will fire at M3-Eval close).
- `feedback_mocked_llm_cannot_prove_prompt_causal_change.md` — real-LLM required for prompt-causal evidence (applies to S-Eval-5 Option A executor wiring + monotone-relaxing rerun).
