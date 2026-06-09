Paste the content below this line into a fresh Claude Code session at the repo root. This is the dev-agent brief for **Sprint 46 — L3 judge repositioning + R-item closure** (S-Eval-5; FIFTH and LAST sub-sprint of Milestone M3-Eval Coarse-to-Fine Evaluation Architecture).

---

You are Claude Code doing the **S-Eval-5 implementation** for Sprint 46 of NEW Milestone M3-Eval. This is the **LAST M3-Eval sub-sprint**; after S-Eval-5 close, M3-Eval closes via milestone-shared Codex review per `iteration_governance.md` §4.3 default + curated bad-case suite manual review as PRIMARY GATE per §5.6.

You ship: (a) demote 3 current L3 dims (`relevance`, `tone_appropriateness`, `groundedness`) from composite to Tier-3 advisory; (b) add NEW `user_goal_achievement` L3 dim as Tier-1 supplementary advisory; (c) update rubric prompts to close R-l3-judge-form-context-trust + R-l1-source-citation-quality; (d) close 4 M3-Eval R-items in action_bank; (e) monotone-relaxing check (no PASS → FAIL transitions). **Codex review deferred to M3-Eval milestone-shared close** per §4.3 default. Handoff §12 stays EMPTY at dev close; deliver-agent + human fill.

## 1. Read order (cold-start; load in this order)

1. `AGENTS.md` (auto-loads `doc_governance.md` + `agent_context_guide.md` + `iteration_governance.md` §1 / §3 / §4 / §5 / §7).
2. `docs/milestone_objective.md` — M3-Eval north star; ESPECIALLY §3 S-Eval-5 row + §5 milestone acceptance bar + §6 hard fences + §7 R-items consumed (4 R-items S-Eval-5 closes) + §8 Codex review plan.
3. `docs/sprint_objective.md` — S-Eval-5 contract (this is the live contract; implement against §5 Files in scope + §6 Files NOT in scope + §8 §7-stanza + §10 STOP signals).
4. `docs/solutions/m3_eval_milestone_proposal.md` §6 S-Eval-5 (scope detail) + §2 four-tier pyramid (L3 dim repositioning context).
5. `docs/sprints/sprint-045-handoff.md` §12 closure verdict — LOAD-BEARING carry-overs: OQ-S45.5 executor wiring decision (LOAD-BEARING; planning-round decision); OQ-S45.6 UC-FP moderation calibration via real-LLM; **OQ-S44.6 Python baseline runner discipline (you MUST use `uv run python -m pytest`, NOT `uv run pytest`)**.
6. `eval_interactive/eval_interactive/scoring/llm_judge.py` — current L3 judge surface (dimensions, rubric prompts location TBD).
7. `eval_interactive/eval_interactive/scoring/composite.py` — composite scorer (may need dim-weighting adjustment per S-Eval-1 `severity` pattern).
8. `eval_interactive/eval_interactive/scoring/hard_checks.py` + `outcome_checks.py` — reference for the S-Eval-1 `severity` field convention (don't modify; reuse pattern).
9. `docs/action_bank.md` §6 close-action index — find the 4 M3-Eval R-items to close (R-l3-judge-form-context-trust-rubric / R-l1-source-citation-quality-rubric / R-cs040-l3-review-intake-completion-semantics / R-cs038-l3-review-intake-efficiency).

## 2. Implementation actions

### 2.1 Locate the L3 rubric surfaces

Read `eval_interactive/eval_interactive/scoring/llm_judge.py` end-to-end at planning round. Identify:

- Where the 3 L3 dimensions (`relevance`, `tone_appropriateness`, `groundedness`) are declared (dim list, score range, rubric prompts).
- Where the rubric prompts live (embedded in `llm_judge.py` constants OR a sibling rubric module).
- How dim scores flow into `case_passed` / `composite_score` (the demotion path — likely a `severity="advisory"` field per S-Eval-1 `HardCheckResult` / `OutcomeCheckResult` convention).
- Where the `persona.user_goal_summary` field is accessible from the judge call site (informs the NEW `user_goal_achievement` dim wiring).

If the rubric prompt structure cannot accommodate the new trust signal OR new dim without a structural rewrite, STOP per §10 #2 and surface (structural rewrite is out of scope per §3 #3).

### 2.2 Executor wiring fix (Option A AUTHORIZED at S-Eval-5 planning round 2026-05-22 per `docs/sprint_objective.md` §2.6)

**Required change** (do NOT skip):

- **Edit** `eval_interactive/eval_interactive/batch/executor.py:252` — pass `tier2_result=` to `compute_composite(...)` (one-line edit).
- **Add 1-2 NEW Python tests** verifying:
  - Tier-2 mandatory result with `failed=true` flips production `case_passed` to false (was the inert path before this fix).
  - Tier-2 advisory result with `failed=true` does NOT flip `case_passed` (advisory non-gating preserved).
  - Empty `critical_steps` parity preserved (Skills without populated `critical_steps` produce no Tier-2 effect; backward-compat per S-Eval-2 ship).

**Why required**: the S-Eval-5 monotone-relaxing rerun (per §2.7) doubles as the first real-LLM Tier-2 surface. With this wiring landed, the rerun exercises the populated `critical_steps` content from S-Eval-3 end-to-end and surfaces real-LLM evidence for OQ-S44.5 (real-LLM Tier-2) + OQ-S44.3+S44.4 (UC-FP moderation calibration) + OQ-S45.5+S45.6 simultaneously. Without it, M3-Eval close inherits the wiring work and milestone-shared Codex review would have zero real-LLM Tier-2 evidence to evaluate.

**Calibration follow-on**: if the monotone-relaxing rerun surfaces a legitimate UC-FP no-context path that escalates or defers instead of answering (contradicting S-Eval-3 mandatory step `consult-moderation-context-on-removal-explanation`), STOP per §3 #2 and surface — deliver-agent + human decide S-Eval-3 fix-iteration (loosen `trace_check` OR downgrade to advisory).

### 2.3 L3 dim demotion (3 dims)

In `eval_interactive/eval_interactive/scoring/llm_judge.py`, demote `relevance`, `tone_appropriateness`, `groundedness` from composite contributors to Tier-3 advisory:

- Reuse the S-Eval-1 `severity="advisory"` field convention (see `HardCheckResult` + `OutcomeCheckResult` for pattern reference).
- Demoted dim numeric scores STILL RECORDED in `case_results[].judge_scores`.
- Demoted dims do NOT factor into `case_passed` or `composite_score`.
- `composite.py` may need a small adjustment to skip advisory L3 dims from gate computation (per S-Eval-1 D-2.5 pattern); if needed, edit per §5.

### 2.4 NEW `user_goal_achievement` L3 dim

Add a NEW L3 dimension `user_goal_achievement`:

- Score range: coarse 1-5 (1 = clear failure / wrong UC / unresolved; 3 = adequate / problem named + appropriate next step; 5 = clear success / user goal observably achieved OR appropriate escalation taken).
- Anchor: `persona.user_goal_summary` from CaseSpec.
- Wired as **Tier-1 supplementary advisory signal** (NOT a hard gate; bad-case + anchor_outcome manual review remain primary Tier-1 gates per §5.6 + milestone §5).
- Feeds into per-case `judge_scores.user_goal_achievement` + a per-run trend metric.
- Does NOT flip `case_passed`.

Rubric prompt for the new dim must follow the §1.7 anti-hardcode standard (additive narrative; NOT keyword/regex/per-UC-matrix). Surface the rubric narrative wording in handoff §5 for deliver-agent + human + Codex review.

### 2.5 Rubric prompt updates (2 R-item closures)

- **R-l3-judge-form-context-trust-rubric**: update L3 rubric to explicitly state `form_context.first_name` (from case persona / pre-populated form context) is a TRUSTED signal; the bot greeting "Hi {first_name}" should NOT be penalised under `tone_appropriateness` for "unauthorized familiarity". Rubric wording: additive trust statement, NOT a per-UC branch.
- **R-l1-source-citation-quality-rubric**: tighten the L1 source-citation evaluation to require `canonical_url` OR article title; reject bare Salesforce IDs (e.g., `ka44J000000gKxqQAE`) as sole citation evidence. Rubric wording: tightened requirement + explicit failure-pattern callout ("citation by Salesforce ID alone is not user-actionable; require canonical_url from the FAQ corpus OR a human-readable article title"). NOT keyword enumeration.

### 2.6 R-item closures in action_bank

In `docs/action_bank.md` §6 close-action index, for each of these 4 R-items, append a `succeeded-by: <S-Eval-5 close commit>` annotation:

- `R-l3-judge-form-context-trust-rubric` — closed via §2.5.
- `R-l1-source-citation-quality-rubric` — closed via §2.5.
- `R-cs040-l3-review-intake-completion-semantics` — closed via S-Eval-1 schema simplification route + tiered architecture (no per-case hard-gate L3 dim under new tiering).
- `R-cs038-l3-review-intake-efficiency` — closed via the same route.

Do NOT delete the R-item entries from action_bank (preserve historical record per §6 convention).

### 2.7 Monotone-relaxing check (LOAD-BEARING)

Verify L3 repositioning is monotone-relaxing. Run smoke (14 cases) + anchor (159 cases) + anchor_outcome (12 cases) with BOTH old and new scoring code:

```bash
# Pre-S-Eval-5 baseline run (capture case_passed per case)
cd eval_interactive && git stash  # if needed to revert local changes temporarily
uv run eval-interactive run --path case_specs/smoke/ --output /tmp/smoke_pre.json
uv run eval-interactive run --path case_specs/anchor/ --output /tmp/anchor_pre.json
uv run eval-interactive run --path case_specs/anchor_outcome/ --output /tmp/anchor_outcome_pre.json

# Post-S-Eval-5 baseline run (with new scoring code)
git stash pop  # restore S-Eval-5 changes
uv run eval-interactive run --path case_specs/smoke/ --output /tmp/smoke_post.json
uv run eval-interactive run --path case_specs/anchor/ --output /tmp/anchor_pre.json
uv run eval-interactive run --path case_specs/anchor_outcome/ --output /tmp/anchor_outcome_post.json

# Diff case_passed transitions
python -c "
import json
for name in ['smoke', 'anchor', 'anchor_outcome']:
    pre = {c['case_id']: c['case_passed'] for c in json.load(open(f'/tmp/{name}_pre.json'))['case_results']}
    post = {c['case_id']: c['case_passed'] for c in json.load(open(f'/tmp/{name}_post.json'))['case_results']}
    transitions = [(cid, pre[cid], post[cid]) for cid in pre if pre[cid] != post.get(cid)]
    print(f'{name}: {len(transitions)} transitions')
    for cid, before, after in transitions:
        print(f'  {cid}: {before} → {after}')
"
```

Expected: **zero `True → False` transitions** across all suites.

If any `True → False` surfaces, STOP per §10 #5 and surface — do NOT silently force the rubric to preserve PASS state.

### 2.8 Validation runs

```bash
mvn test 2>&1 | grep "Tests run" | tail -1
```

Expected: `Tests run: 1163, Failures: 1, Errors: 0, Skipped: 2` UNCHANGED (S-Eval-5 ships no Java).

```bash
cd eval_interactive && uv run python -m pytest 2>&1 | tail -1
```

**MUST use `uv run python -m pytest`** per OQ-S44.6 carry-over. Baseline pre-S-Eval-5: `5 failed / 396 passed`. Post-S-Eval-5: expected `5 failed / 401-411 passed` (S-Eval-5 adds 5-15 NEW tests for demoted dims + new dim + monotone-relaxing + rubric updates + optional executor wiring). No new failures.

### 2.9 Handoff document

Author `docs/sprints/sprint-046-handoff.md` per the 12-section template (per Sprint 35 / 41 / 42 / 43 / 44 / 45 precedent shape; `docs/sprint_objective.md` §11 specifies the contract). **§3 numstat sub-tally arithmetic MUST validate by re-summing per-file counts** per OQ-S44.6 fold-back observation. **§5 surface the actual rubric narrative wording** for R-l3-judge-form-context-trust + R-l1-source-citation-quality + new `user_goal_achievement` dim (for deliver-agent + human + Codex review at M3-Eval close). LEAVE §12 EMPTY per `feedback_handoff_verdict_section_delegation.md`.

## 3. STOP discipline

STOP and surface to deliver-agent + human (instead of pressing on) per `docs/sprint_objective.md` §10 if any of:

1. ~~Option A executor wiring fix authorisation needed~~ (**STRUCK**: Option A AUTHORIZED at planning round 2026-05-22 per §2.2; `executor.py:252` IS in scope per §2 + contract §5; no STOP needed).
2. Rubric prompt structural rewrite needed (out of scope; §3 #3).
3. L3 model temperature / provider / model change needed.
4. Populated `critical_steps` content edit needed in 6 Skill YAMLs.
5. Monotone-relaxing check FAILS (any `case_passed=true → false` transition surfaces).
6. Any case fixture edit needed to make tests pass.
7. Java or Python baseline regresses.
8. Any file under "Files NOT in scope" §6 needs touching (Option A executor wiring is the ONLY exception with explicit deliver-agent + human approval).
9. R-item closure path unclear for any of the 4 M3-Eval R-items.

## 4. Hard fences (per `docs/sprint_objective.md` §6)

ZERO touch to:
- Tier-0 / Tier-1 hard-gate code (`hard_checks.py` / `outcome_checks.py`).
- Tier-2 code (`skill_procedure_check.py`).
- CaseSpec schema + loader (`case_spec/schema.py` / `case_spec/loader.py`).
- Skill abstraction (M2 + S-Eval-2 + S-Eval-3 Java + 6 Skill YAMLs; `ContextProjectionBuilder.java`; `IntakeFieldsRegistry.java`).
- Runtime semantic surfaces (`RuntimeIntentClassifier.java` / `IntentClassification.java` / `DriftResult.java` / `DriftDetector.java` / `UseCaseRouter.java` / `ClassifyUseCaseTool.java` / `PhaseEvaluator.java` / `AgentRunLoopImpl.java` / `ControlKernel.java` / `system_prompt.txt` / `tool-policy.yaml`).
- Eval case fixtures (smoke / anchor / anchor_outcome / case-families / 12 bad cases / shadow / `case_spec_overrides.yaml`).
- Eval harness / loader / simulator / `batch/executor.py` (NO touch EXCEPT `executor.py:252` REQUIRED per Option A AUTHORIZED §2.2; no other touch).
- Governance + foundational + sprint / milestone archives.
- `docs/codex-findings.md` (stays scaffold during S-Eval-5).
- `docs/milestone_objective.md` (deliver-agent territory).

## 5. §4.1 nine-question anti-hardcode self-walk (REQUIRED — fill in handoff §6)

Walk Q1-Q9 against the cumulative diff. Cite file:line for each verdict. Expected:

- **Q1** Semantic hardcode? Expected: pass — dim weight changes + new dim wiring + rubric narrative updates; no keyword/regex/per-UC-matrix introduced.
- **Q2** Tier-0 invariant protection? Expected: N/A — no new Tier-0.
- **Q3** Soft signal replace hard branch? Expected: pass — the dim demotions + new dim are weight adjustments (soft signal direction); the rubric updates are additive trust signals + tighter requirements (NOT hard branches).
- **Q4** Eval phrase / trace-specific phrasing / CaseSpec-id encoded? Expected: pass — rubric narrative describes patterns in semantic terms, NOT case-specific text.
- **Q5** LLM ownership shrunk? Expected: pass — judge config + rubric ARE the eval side; LLM ownership of §1.3 surfaces unchanged.
- **Q6** Prompt if-else added? Expected: pass — no `system_prompt.txt` / Skill YAML touch; rubric updates are narrative.
- **Q7** Tool schema / capability / PII / grounding floor preserved? Expected: pass — no tool / runtime change; grounding floor strengthened by R-l1-source-citation-quality update.
- **Q8** Generalization coverage? Verify per §8 stanza.
- **Q9** Rollback / sunset plan? Expected: N/A — L3 repositioning intended permanent (M3-Eval north star).

## 6. Handoff document contract (12 sections per Sprint 35 / 41 / 42 / 43 / 44 / 45 shape)

Per `docs/sprint_objective.md` §11. Key sections:

§3 Files shipped + numstat (re-sum per-file counts; validate vs `git show --stat <commit>` per OQ-S44.6 fold-back).
§4 Per-change content map (3 dim demotions + 1 new dim + 2 rubric updates + 4 R-item closures + optional executor wiring).
§5 Rubric prompt update content (actual narrative wording for R-l3-judge-form-context-trust + R-l1-source-citation-quality + new `user_goal_achievement` dim).
§6 §4.1 anti-hardcode self-walk Q1-Q9.
§7 Open questions (OQ-S46.N format).
§8 Generalization coverage filled.
§9 Validation runs (Java baseline + Python baseline VIA `uv run python -m pytest` + monotone-relaxing check diff table + per-dim score distribution + executor wiring outcome if Option A taken).
§10 Contract drift.
§11 Bundle policy honored.
§12 Closure verdict (LEFT EMPTY).

## 7. Bundle policy

Ship in ONE bundle commit containing:
- `llm_judge.py` EDIT + optional rubric file edit + optional `composite.py` EDIT.
- NEW Python regression tests for demoted dims + new dim + monotone-relaxing + rubric updates.
- `docs/action_bank.md` EDIT (4 R-item closures + optional Sprint 46 row).
- **`executor.py:252` EDIT + 1-2 NEW tests** (REQUIRED per Option A AUTHORIZED §2.2).
- Dev handoff `docs/sprints/sprint-046-handoff.md`.

Expected: 6-10 files in bundle.

Do NOT stage deliver-agent close-out files. Human bundles separately per `feedback_commit_at_end_bundles_deliver_artefacts.md`.

Suggested commit message:

```
sprint 46 / S-Eval-5: L3 judge repositioning + R-item closure (NEW M3-Eval sub-sprint 5; LAST M3-Eval sub-sprint)
```

## 8. Self-check before commit

- [ ] 3 L3 dims demoted to Tier-3 advisory; numeric scores still recorded; do NOT flip `case_passed` / `composite_score`.
- [ ] NEW `user_goal_achievement` L3 dim landed; 1-5 score; persona.user_goal_summary anchor; Tier-1 supplementary advisory wiring.
- [ ] Rubric updates: R-l3-judge-form-context-trust trust signal + R-l1-source-citation-quality canonical_url-or-title requirement landed (narrative wording surfaced in handoff §5).
- [ ] 4 M3-Eval R-items closed in `docs/action_bank.md` §6 close-action index (`succeeded-by` annotations).
- [ ] **Monotone-relaxing check PASS**: 0 `case_passed=true → false` transitions across smoke + anchor + anchor_outcome.
- [ ] Java baseline `Tests run: 1163, Failures: 1, Errors: 0, Skipped: 2` UNCHANGED.
- [ ] Python baseline cited via `uv run python -m pytest`, NOT `uv run pytest` (per OQ-S44.6); 5 pre-existing failures unchanged; 7-17 NEW tests added (no new failures).
- [ ] **Executor wiring fix landed (Option A §2.2)**: `executor.py:252` passes `tier2_result=` to `compute_composite(...)`; 1-2 NEW tests verify Tier-2 mandatory results flow through to production `case_passed`; UC-FP moderation calibration outcome surfaced in handoff §9.
- [ ] §4.1 self-walk Q1-Q9 filled in handoff §6.
- [ ] §6 hard fences honored — no touch to scoring code beyond `llm_judge.py` + optional `composite.py` adjustment + optional `executor.py:252` per §2.2 + tests; no touch to runtime / Skill / case fixtures / governance / archives / codex-findings / milestone_objective.
- [ ] Handoff §3 numstat sub-tally arithmetic validated by re-summing (per OQ-S44.6 fold-back).
- [ ] Handoff §12 LEFT EMPTY.
- [ ] Single bundle commit (no deliver-agent files staged).
