---
title: Sprint 35 objective — Option β coverage probe + §7.2 worked-example re-anchor decision (M1 sub-sprint 3)
doc_tier: sprint-archive
status: archived
implementation_status: implemented
source_of_truth: this file
last_reviewed: 2026-05-17
review_cadence: ad hoc
supersedes: [docs/sprints/sprint-034-objective.md]
superseded_by: docs/sprint_objective.md
notes: >
  Sprint 35 is the third sub-sprint of Milestone M1 (DISCOVER + Intake)
  per `docs/milestone_objective.md`. Layer `eval_spec` per
  `docs/current/iteration_governance.md` §3.2 Q6 (the probe corpus IS
  an eval-spec instrument; the §7.2 worked-example re-anchor IS an
  eval-spec / governance fold-back). §7 stanza REQUIRED; Codex review
  deferred to M1 milestone-shared close per §4.3 default (no Tier-0
  candidate, no §1.7 red line, no hard-fence violation expected; no
  production code touch). Sprint 35 closes the Sprint 32-surfaced
  R-option-beta-coverage-gap-uc-a-uc-c-shape R-item by producing a
  per-(topic_subject, description-shape) AMBIGUOUS/ROUTED matrix and
  recommending one of two decisions: (a) re-anchor the §7.2 worked
  example to a UC pair that DOES fall within Option β coverage
  (Sprint 32 neighbor #1 evidence: UC-A↔UC-FP — both share "Ad
  Support" topic), OR (b) open a follow-on R-item
  R-option-gamma-alternate-uc-surveyor-design for post-routing drift
  shape coverage. Framing B (probe + decision-execution): if (a)
  confirmed at sub-sprint close, deliver-agent commits the §7.2
  fold-back as a follow-up commit at Sprint 35 close (NOT bundled
  with dev commit, per §1.7 read-only-against-runtime discipline +
  `feedback_commit_at_end_bundles_deliver_artefacts.md`).
---

# Sprint 35 — Option β coverage probe + §7.2 worked-example re-anchor decision

**Milestone:** M1 (DISCOVER + Intake) sub-sprint 3 of 3-4 per `docs/milestone_objective.md`.

## 1. Sub-sprint class

**Diagnostic / characterization sub-sprint, single track, read-only against production.** Layer: `eval_spec` per `docs/current/iteration_governance.md` §3.2 Q6 (the probe CaseSpec corpus is an eval-spec instrument; the conditional §7.2 worked-example re-anchor IS an eval-spec / governance fold-back). Sprint 35 is **not a behaviour-change sprint** — no production runtime, no prompt, no judge calibration is edited; the dev session runs the existing intake router against a deliberately-shaped CaseSpec corpus and writes down what it observes.

**§7 stanza REQUIRED** (see §9 below). The probe layer is eval_spec; the stanza fields are filled honestly — most read "N/A" or "deferred to conditional follow-up", which is the right shape for a measurement sprint.

**Codex review:** deferred to M1 milestone-shared close per `iteration_governance.md` §4.3 default. No per-sub-sprint Codex trigger expected (no Tier-0 candidate, no §1.7 red line, no hard-fence violation; no production code change to review).

## 2. Goal

Produce a per-(`topic_subject`, `description`-shape) AMBIGUOUS/ROUTED matrix that empirically characterizes where Option β (the Sprint 31 `alternate_candidate_use_cases` projection slot + Sprint 32 alternate-UC case family) DOES and DOES NOT provide coverage. Use the matrix to recommend one of two decisions at sub-sprint close:

- **Decision (a) — re-anchor `iteration_governance.md` §7.2 worked example.** The §7.2 worked example today describes a hypothetical Sprint 18 fix for UC-A↔UC-C drift. Sprint 32 §13 surfaced the architectural finding that UC-A↔UC-C drift is OUTSIDE Option β coverage because UC-A and UC-C belong to DIFFERENT `topic_subjects` ("Ad Support" vs "Replies or Messaging"), so the intake router never surfaces both as candidates. The Sprint 32 neighbor #1 evidence indicates UC-A↔UC-FP works (both share "Ad Support" topic). Decision (a) folds the §7.2 worked example back to a UC pair that DOES fall within Option β coverage, so the governance doc's worked example matches the implementation.
- **Decision (b) — open a follow-on R-item `R-option-gamma-alternate-uc-surveyor-design`.** If the matrix reveals a broader, more interesting class of post-routing drift coverage gaps (e.g., cross-topic drift that the existing Option β cannot rescue under any UC pair), the right next step is to design "Option γ" — a live `AlternateUseCaseSurveyor` component that scans for cross-topic drift signals at intake time. This R-item lands in `action_bank.md`; the design work happens in a later sub-sprint or milestone.

Sprint 35 dev produces the matrix + recommendation; deliver-agent + human confirm decision at sub-sprint close.

## 3. Non-goals (explicit)

- Sprint 35 does NOT touch any production code under `server/src/main/`. Read-only against the production runtime surface.
- Sprint 35 does NOT touch `UseCaseRouter.java`, `IntentClassification.java`, `RuntimeIntentClassifier.java`, `DriftDetector.java`, `ClassifyUseCaseTool.java`, `PhaseEvaluator.java`, `ContextProjectionBuilder.java`, `IntakeFieldExtractor.java`, `IntakeFieldsRegistry.java`, `UseCaseRegistryService.java`. These are READ surfaces for the probe; not edited.
- Sprint 35 does NOT touch `server/src/test/`. No Java tests added; the existing baseline (983 / 1-inherited / 0 / 2 post-Sprint-34) is preserved.
- Sprint 35 does NOT touch `eval_interactive/eval_interactive/` (harness, loader, simulator).
- Sprint 35 does NOT touch existing case families under `eval_interactive/case_specs/case_families/`, shadow case families under `eval_interactive/case_specs_shadow/case_families/`, smoke under `eval_interactive/case_specs/smoke/`, bad cases under `eval_interactive/case_specs/bad_cases/`, or `eval_interactive/case_spec_overrides.yaml`. The probe corpus lives in a NEW directory `eval_interactive/case_specs/probe/option_beta_coverage/` per M1 plan §3 Sprint 35 row.
- Sprint 35 does NOT touch `iteration_governance.md` in the dev session. The §7.2 fold-back (if decision (a) is confirmed) is a deliver-agent follow-up commit at Sprint 35 close — NOT bundled with the dev commit. Rationale: `iteration_governance.md` is a constitutional governance doc; edits to it require deliver-agent + human authority per `AGENTS.md` (the file is loaded transitively at every cold start). Per Framing B, the fold-back happens AT Sprint 35 close, not deferred to "normal cadence" — but it remains a deliver-agent commit, not a dev commit.
- Sprint 35 does NOT touch other foundational docs under `docs/foundational/` or `docs/current/` (other than the conditional §7.2 fold-back via deliver-agent at sprint close).
- Sprint 35 does NOT close any R-item by itself; the deliver-agent closes / opens R-items in `action_bank.md` at Sprint 35 close based on the human-confirmed decision.
- Sprint 35 does NOT add a Tier-0 invariant.

## 4. Premise check (deliver-agent verified 2026-05-16)

Verified at HEAD post-Sprint-34-close (cumulative commit range includes Sprint 33 + Sprint 34 commits; deliver-agent will verify Sprint 34 has been committed before dev session starts):

1. **`server/src/main/resources/config/use-case-registry.yaml`** at HEAD carries the multi-candidate weak-prior topic mapping:
   - "Ad Support" → [UC-A (FAQ visibility), UC-B (FAQ posting), UC-FP (FAQ deletion-explanation), UC-H (INTAKE appeal)] — 4 candidates
   - "Payments" → [UC-F (FAQ), UC-I (INTAKE)] — 2 candidates
   - "Technical Support" → [UC-E (FAQ), UC-K (INTAKE)] — 2 candidates
   - "Account Support" → [UC-D] — 1 candidate (single-UC weak-prior)
   And the strong-prior topics: "Replies or Messaging" → UC-C, "Delete My Account or Data" → UC-G, "Report a Safety Issue" → UC-J. Handover-only topics: "Delivery", "Pro Contract", "Ratings Reviews", "Account Manager Support".
2. **`UseCaseRouter.java`** at HEAD owns the `topic_subject` + `description`-shape → `RoutingResult{ AMBIGUOUS | ROUTED }` decision. The exact algorithm + threshold are READ surfaces; Sprint 35 dev MUST NOT touch.
3. **`alternate_candidate_use_cases` projection slot** at HEAD (Sprint 31 ship) is populated when the intake router returns AMBIGUOUS with > 1 candidate; the slot exposes the candidate list to the LLM. Confirmed via `ContextProjectionBuilder.java` grep (Sprint 33 handoff §3.2 verified the slot's presence post-Sprint-32-close).
4. **Sprint 32 surfaced finding `R-option-beta-coverage-gap-uc-a-uc-c-shape`** is the load-bearing R-item for Sprint 35. Per `docs/sprints/sprint-032-handoff.md` §13 (Codex re-run + deliver-agent investigation): UC-A↔UC-C drift falls OUTSIDE Option β coverage because the UCs belong to different `topic_subjects`. Sprint 32 neighbor #1 evidence: UC-A↔UC-FP works.
5. **`iteration_governance.md` §7.2 worked example** at HEAD names a hypothetical Sprint 18 fix for UC-A↔UC-C drift. This is the candidate re-anchor target for decision (a) at Sprint 35 close.
6. **`docs/action_bank.md`** at HEAD has `R-option-beta-coverage-gap-uc-a-uc-c-shape` open. Sprint 35 close will either close it (if decision (a)) or close it AND open `R-option-gamma-alternate-uc-surveyor-design` (if decision (b)). Either way, the R-item flow is deliver-agent-owned at sub-sprint close.
7. **`docs/diagnostics/`** directory exists per `docs/diagnostics/failure-briefs/` precedent (Sprint 33 references). Sprint 35's analysis doc lands at `docs/diagnostics/option_beta_coverage_matrix.md`.

Sprint 35 dev SHALL re-verify these at session start by reading the cited files (one read each; one short paragraph in handoff §3). If any premise has drifted between 2026-05-16 and dev session start, STOP and surface; do NOT code under a false premise.

## 5. Files in scope (Sprint 35 dev ships)

| path | change type |
|------|-------------|
| `eval_interactive/case_specs/probe/option_beta_coverage/` | NEW directory (FLAT layout per M1 plan §3 Sprint 35 row). Holds the probe CaseSpec corpus. |
| `eval_interactive/case_specs/probe/option_beta_coverage/_manifest.md` | NEW directory manifest (mirrors `eval_interactive/case_specs/bad_cases/_manifest.md` shape): purpose, lifecycle convention, per-case index, governance pointer. Names the directory as a `probe` (diagnostic instrument, not a regression suite). |
| `eval_interactive/case_specs/probe/option_beta_coverage/*.yaml` | NEW probe CaseSpecs. Target: ~20-30 CaseSpecs covering the (topic_subject × description-shape) dimensions per §5.1 below. Each CaseSpec follows the standard CaseSpec schema PLUS a `probe_metadata` block naming: `probe_dimension_topic_subject` (the topic axis value), `probe_dimension_description_shape` (the description-shape axis value), `probe_expected_routing` (deliver-agent + human prediction of AMBIGUOUS vs ROUTED for analysis), `probe_purpose` (one-line). The CaseSpecs themselves do NOT carry full `expected.expected_tool_sequence` / `judge_rubric` — they are observation instruments, not regression gates; the `closure_criterion` field is replaced with `probe_observation_recipe` naming what to extract from the trace at run time. |
| `docs/diagnostics/option_beta_coverage_matrix.md` | NEW analysis document. Front matter: `doc_tier: diagnostic`, `status: diagnostic`, `implementation_status: implemented`, `source_of_truth: this file`, `last_reviewed: 2026-05-16`, `review_cadence: ad hoc`. Body: §1 purpose; §2 method (probe corpus design + run protocol); §3 raw matrix (rows = topic_subjects, columns = description-shapes, cells = AMBIGUOUS{candidate_list} or ROUTED{uc}); §4 observations + analytical commentary; §5 recommended decision (a/b) with rationale; §6 if (b) recommended, draft R-item text for `R-option-gamma-alternate-uc-surveyor-design`. |

### 5.1 Probe corpus design (deliver-agent + dev co-design)

The corpus is a deliberate subset of the (topic_subject × description-shape) cartesian product, NOT every cell. Goal: enough coverage to draw the matrix; not so many CaseSpecs that the harness run takes hours.

**Recommended dimensions (deliver-agent baseline; dev may refine at session start and surface in handoff §4):**

- **Topic_subject axis (7 values):** the 4 weak-prior topics (`"Ad Support"`, `"Payments"`, `"Technical Support"`, `"Account Support"`) where Option β has potential coverage; the 3 strong-prior topics (`"Replies or Messaging"`, `"Delete My Account or Data"`, `"Report a Safety Issue"`) as controls; the 4 handover-only topics may be SKIPPED (no UC routing path exists, so the matrix cell is trivially handover).
- **Description_shape axis (5 values):** (1) single-issue (description names ONE UC's surface clearly, e.g., for "Ad Support": "Why was my ad removed"); (2) multi-issue same-topic (description names TWO UCs sharing the same topic_subject — the Option β-coverable case, e.g., "Ad Support": "Why was my ad removed AND how do I appeal"); (3) cross-topic drift (description names UCs from DIFFERENT topic_subjects — the Option β NON-coverable case per Sprint 32 finding, e.g., "Ad Support" form but description mentions "and my messages aren't coming through"); (4) ambiguous-soft (no UC named explicitly; soft visibility-vs-appeal split, the Alice shape); (5) empty / generic ("can you help me").

**Total CaseSpec count target:** ~25-30 (7 topics × 5 shapes is 35 cells; the dev prunes the handover-only-trivial + duplicate-info cells to land on ~25-30 meaningful probes). Final count is dev-judgment-call; document the pruning rationale in handoff §4 + the analysis doc §2.

**Each probe CaseSpec invokes the existing intake router via a normal `uv run eval-interactive run` invocation** (no new harness path). The dev extracts `active_use_case` + `alternate_candidate_use_cases` slot + `intent_routing.result` from the per-turn trace to populate the matrix cell.

### 5.2 Sub-sprint-close artefacts (deliver-agent owned, M1 milestone-scoped)

| path | change type |
|------|-------------|
| `docs/sprints/sprint-035-handoff.md` | NEW (12-section dev-authored archive per Sprint 31 / Sprint 32 / Sprint 33 / Sprint 34 shape) |
| `docs/sprint_objective.md` | EDIT after Sprint 35 close (deliver-agent replaces with M1 close planning OR Sprint 36 contract; archives this contract to `docs/sprints/sprint-035-objective.md`) |
| `docs/10-handoff.md` §1 lead | EDIT (deliver-agent demotes Sprint 35 to "current sub-sprint completed", sets either M1 close or Sprint 36 as current) |
| `docs/action_bank.md` | EDIT at Sprint 35 close (deliver-agent): close `R-option-beta-coverage-gap-uc-a-uc-c-shape` per the confirmed decision; if (b), open `R-option-gamma-alternate-uc-surveyor-design` with the draft text from `docs/diagnostics/option_beta_coverage_matrix.md` §6 |
| `docs/codex-findings.md` | UNCHANGED at Sprint 35 close (Codex deferred to M1 milestone-shared close per §4.3 default) |
| `compact/sprint-035-dev-prompt.md` | deliver-agent-owned (authored 2026-05-16; NOT staged by dev) |
| `compact/M1-review-prompt.md` | deliver-agent-owned, drafted at M1 close (NOT this sub-sprint) |
| `docs/current/iteration_governance.md` §7.2 fold-back | CONDITIONAL — if decision (a) confirmed at Sprint 35 close, deliver-agent commits the §7.2 worked-example re-anchor as a SEPARATE follow-up commit (NOT bundled with dev commit). Per `feedback_commit_at_end_bundles_deliver_artefacts.md`: deliver-agent + human bundle deliver-agent-owned files; dev does NOT stage governance docs. Per the M1 §3 Sprint 35 row: this fold-back happens AT Sprint 35 close (Framing B confirmed by deliver-agent + human 2026-05-16), not deferred to later cadence. |

## 6. Files NOT in scope (hard fences)

1. **No edits to ANY production code** under `server/src/main/`. Sprint 35 is read-only against the production runtime surface.
2. **No edits to ANY Java tests** under `server/src/test/`. Java baseline preserved.
3. **No edits to `eval_interactive/eval_interactive/`** — harness/loader/simulator stable.
4. **No edits to existing case families** under `eval_interactive/case_specs/case_families/` (cascade fence).
5. **No edits to shadow case families** under `eval_interactive/case_specs_shadow/case_families/`.
6. **No edits to `eval_interactive/case_specs/smoke/`**.
7. **No edits to `eval_interactive/case_specs/bad_cases/`**.
8. **No edits to `eval_interactive/case_spec_overrides.yaml`**.
9. **No edits to `iteration_governance.md` in the dev session.** The §7.2 fold-back (if decision (a)) is a deliver-agent follow-up commit at Sprint 35 close.
10. **No edits to OTHER foundational docs** under `docs/foundational/` or other `docs/current/` (other than the conditional §7.2 fold-back via deliver-agent).
11. **No edits to sprint archives** under `docs/sprints/sprint-001-*` through `docs/sprints/sprint-034-*`.
12. **No edits to `docs/milestone_objective.md`** — the M1 contract stays stable across sub-sprints (deliver-agent updates only at M1 close).
13. **No edits to `docs/action_bank.md`** in the dev session — R-item flow is deliver-agent-owned at sub-sprint close.
14. **No new Tier-0 invariant**. No edits to `docs/runtime_freeze_and_risk_policy.md`.
15. **No mocked-LLM for probe runs.** Probe corpus runs against the real backend at `http://localhost:8080` (`make backend` profile=local, real Moonshot/Kimi LLM endpoint per `application-local.yml`), same channel as Sprint 33's Alice run. Per `feedback_mocked_llm_cannot_prove_prompt_causal_change.md`: the intake router has a deterministic component (the `UseCaseRouter`'s topic + alternate logic is Java; the alternate-candidate surface to the LLM is the slot we want to observe). The LLM-behaviour layer (whether the LLM acts on the alternate candidates) is ALSO part of the matrix — recorded as observed, not enforced.

## 7. Bundle policy

- **Single dev commit** with all §5 dev-authored files: the new probe directory + its CaseSpecs + the `_manifest.md` + the `docs/diagnostics/option_beta_coverage_matrix.md` analysis + `docs/sprints/sprint-035-handoff.md`.
- Per `feedback_commit_at_end_bundles_deliver_artefacts.md`: dev does NOT stage deliver-agent-owned files (`docs/sprint_objective.md` / `docs/milestone_objective.md` / `docs/10-handoff.md` / `docs/action_bank.md` / `docs/codex-findings.md` / `compact/sprint-035-dev-prompt.md` / `docs/current/iteration_governance.md`). Deliver-agent + human bundle those at sub-sprint close.
- **Smoke rerun on 14-case set is NOT required** at Sprint 35 close. Smoke is observation only per §5.5; M1 milestone close rerun covers it.
- **Bad-case suite rerun is NOT required** at Sprint 35 close. Sprint 35 does not change runtime behaviour; the bad-case suite shape is unchanged. M1 milestone close rerun covers it across the cumulative Sprint 33 + 34 + 35 commit range.
- **Probe corpus run IS required** at Sprint 35 close: the matrix cannot be populated without running the probe CaseSpecs. Per §5 file table the dev runs `cd eval_interactive && uv run eval-interactive run --path case_specs/probe/option_beta_coverage/` against the local backend, extracts per-cell observations, and populates the matrix.
- **Java test suite SHALL run clean** even though Sprint 35 ships zero Java change: zero new regressions, the inherited `SystemPromptUserRequestedTiebreakerTest` failure remains the documented baseline.

## 8. Layer-classification + anti-hardcode stanza (per §7; REQUIRED)

**Target failure layer:** `eval_spec` per `docs/current/iteration_governance.md` §3.2 Q6. The Sprint 32-surfaced finding `R-option-beta-coverage-gap-uc-a-uc-c-shape` is a question about whether the §7.2 worked example AND the underlying alternate-UC mechanism are aligned — that's an eval-spec / governance question, not a runtime semantic question. The probe corpus IS an eval-spec instrument (a measurement designed to characterize a runtime surface). The §7.2 fold-back IS a governance / eval-spec adjustment (re-anchoring an instructive example to match implementation).

**Tier-0 invariant:** This sub-sprint adds no Tier-0 invariant. Sprint 35 ships zero production code change; no runtime invariant surface is touched. The probe is read-only against `UseCaseRouter` + projection builder.

**Semantic hardcode:** No semantic hardcode introduced. Sprint 35 ships zero runtime code change. The probe CaseSpecs are observation instruments. The analysis doc is descriptive prose. The conditional §7.2 fold-back (if decision (a)) updates a worked-example UC pair — replacing `UC-A↔UC-C` with `UC-A↔UC-FP` in the prose — which is documentation alignment, not a new behavioural rule. The §4.1 nine-question walk verdict (pre-walked by deliver-agent): `approve` — every question except Q4 + Q8 is N/A (no production code, no prompt, no Java guard); Q4 (does the change encode visible-eval phrasing / CaseSpec id / trace text into runtime, prompt, or judge config?) is NO (the probe CaseSpecs are NEW; they do not feed runtime / prompt / judge; the analysis doc cites the probe CaseSpec ids but they are new probe ids, not regression eval ids); Q8 (does the PR ship generalization eval coverage?) is N/A in the regression sense (the probe IS the eval; coverage is the matrix itself).

**Generalization coverage:**

- **Target:** the per-(topic_subject, description-shape) matrix. Coverage success = matrix is populated for the ~25-30 chosen cells with cited per-cell extraction recipes; analysis interprets the matrix; recommendation is grounded in cell evidence.
- **Neighbor:** N/A. Probe sprints do not have a regression neighbor concept — every probe CaseSpec IS a coverage cell.
- **Negative:** the handover-only topics + the strong-prior topics serve as deliberate control cells (single-UC routing should never produce AMBIGUOUS). If the probe accidentally produces AMBIGUOUS for a handover-only or strong-prior topic, that is an observation worth flagging in the analysis doc (NOT a Sprint 35 fix; possibly a Sprint 36 / M2 R-item).
- **Shadow:** N/A. Probe sprints do not have a held-out shadow concept — every probe is visible by design.

## 9. Success metrics (per `iteration_governance.md` §5)

**Hard gates (must pass for Sprint 35 close):**

- All §5 dev-authored files land in one dev commit.
- Probe corpus runs to completion against the local backend (no harness crash, no infrastructure issue).
- The per-cell matrix in `docs/diagnostics/option_beta_coverage_matrix.md` §3 is populated for every probe CaseSpec; every cell carries its extraction recipe (source result.json path + extraction commandlet) per `feedback_deliver_agent_cited_numbers_must_be_reproducible.md`.
- Analysis doc §5 recommends decision (a) or (b) with cited per-cell evidence.
- Java test suite: zero new regressions (baseline 983/1-inherited/0/2 preserved since Sprint 34).
- Anti-hardcode self-walk: handoff §8 walks the §4.1 nine questions; expected verdict `approve` (no semantic hardcode; probe is read-only).
- Reproducibility: every quantitative claim in handoff + analysis doc cites source path + extraction recipe.

**Primary gate (M1-scoped, not Sprint-35-blocking but tracked):**

- Sprint 35 contributes to the M1 milestone-close evidence pool. The matrix + decision are the load-bearing artefacts for closing the Sprint 32-surfaced R-item.

**Observations (not gates):**

- Smoke composite_score and friends per §5.5 (demoted to observation 2026-05-16). Not run in Sprint 35 dev session.
- Bad-case suite per §5.6. Not run in Sprint 35 dev session; covered at M1 close.
- Architecture-health metrics direction (§6): `new_semantic_hardcode_count` SHOULD be 0 (no runtime code); `soft_signal_conversion_count` = 0; `planner_ownership_ratio` unchanged.

**Codex review:** deferred to M1 milestone close per §4.3 default. Sprint 35 dev does NOT dispatch Codex; deliver-agent + human do at M1 close. Note: if the matrix surprises the deliver-agent + human (e.g., reveals an unexpected coverage gap on a control cell that was supposed to route cleanly), a per-sub-sprint Codex review MAY be triggered per §4.3 condition #1 / #3; deliver-agent + human decide at Sprint 35 close based on the matrix.

## 10. Stop conditions (dev-agent)

STOP and report (do NOT silently work around) when:

1. **Premise drift on §4 items** — any of the 7 premises has changed since 2026-05-16. STOP and surface in handoff §3.
2. **Tempted to edit ANY production code** under `server/src/main/` — STOP. Sprint 35 is read-only.
3. **Tempted to "fix" an observed coverage gap by editing `UseCaseRouter` or the projection slot logic** — STOP. The matrix is supposed to be a faithful characterization of the CURRENT routing surface; fixing observed gaps in this sprint would corrupt the measurement. If an observed gap is high-impact, surface as OQ for M1 close.
4. **Tempted to edit `iteration_governance.md` §7.2 in the dev commit** — STOP. The §7.2 fold-back is a deliver-agent commit at Sprint 35 close per Framing B + `feedback_commit_at_end_bundles_deliver_artefacts.md`.
5. **Tempted to author CaseSpecs in `case_families/` or `bad_cases/`** instead of the new `probe/option_beta_coverage/` directory — STOP. Sprint 35's probe lives in its own directory per M1 plan §3 Sprint 35 row.
6. **Tempted to invent runtime-attributable findings from the matrix that the matrix evidence does not support** — STOP. Each cell observation is the only evidence; do not extrapolate to "the runtime has bug X" without grounding the claim in the cited cell + trace.
7. **Probe corpus run fails to complete** (backend crash, timeout, OOM, harness error) — STOP. Diagnose root cause; do NOT silently retry with reduced corpus or skip failing cells. Surface in handoff §4 and discuss with deliver-agent.
8. **The matrix reveals a clear control-cell violation** (e.g., a strong-prior topic produces AMBIGUOUS) — DO NOT fix in this sprint, but DO flag prominently in handoff §7 OQ as a candidate for M2 or a Sprint 36 trigger.
9. **Java test regression appears for any reason** — STOP. Sprint 35 changes zero Java code; any regression is suspect (test flake, external state). Diagnose before commit.
10. **Tempted to recommend BOTH decision (a) AND decision (b)** — STOP. Pick one. If both seem warranted, the right answer is usually (a) (re-anchor §7.2 to a covered pair) PLUS the (b) R-item under a different framing (e.g., R-option-gamma-alternate-uc-surveyor-design as the FOLLOW-ON design work AFTER §7.2 is re-anchored). Surface this nuance in §5 of the analysis doc; let deliver-agent + human disambiguate at sub-sprint close.

## 11. Handoff document contract (12 sections per Sprint 31 / Sprint 32 / Sprint 33 / Sprint 34 shape)

Standard 12-section shape:

1. Context Pack.
2. Sub-sprint-objective recap.
3. Premise re-verification (§4 spot-check; 7 premises).
4. Implementation walkthrough — probe corpus design (final dimension counts, pruning rationale); analysis doc structure; run protocol; cited extraction recipe per cell.
5. Probe corpus run — result path + per-cell trace evidence + the populated matrix (or summary; the full matrix lives in `docs/diagnostics/option_beta_coverage_matrix.md`).
6. Generalization coverage table per §8 — adapted shape for a probe sprint (target = matrix coverage; control cells = handover-only / strong-prior).
7. Open questions for human — any control-cell surprise, any cell whose trace was ambiguous, any decision-edge between (a) and (b). Each may become an M1-close concern or follow-on R-item.
8. Anti-hardcode self-walk (§4.1 nine questions).
9. Files changed — table with paths (probe directory + manifest + CaseSpec count + analysis doc + handoff).
10. Layer-classification self-walk per §3 (lands on `eval_spec`).
11. §5 Eval Acceptance bars — adapted for a probe sprint; each gate line by line with cited evidence.
12. Closure verdict placeholder — leave for M1 milestone-shared decision per §4.3 + `feedback_handoff_verdict_section_delegation.md` (Sprint 35 itself does not close standalone; it contributes to the M1 close evidence AND to the §7.2 fold-back decision at Sprint 35 close).

## 12. M1 milestone context (cross-reference, not Sprint 35 contract)

Sprint 35 is the third of M1's 3-4 sub-sprints per `docs/milestone_objective.md`. After Sprint 35 commits:

- Deliver-agent + human review Sprint 35 handoff + matrix + recommendation at sub-sprint close.
- **If decision (a) confirmed:** deliver-agent commits `iteration_governance.md` §7.2 fold-back (small docs edit replacing the UC-A↔UC-C hypothetical worked-example with UC-A↔UC-FP) as a separate commit; deliver-agent closes `R-option-beta-coverage-gap-uc-a-uc-c-shape` in `action_bank.md`; M1 close planning begins.
- **If decision (b) confirmed:** deliver-agent opens `R-option-gamma-alternate-uc-surveyor-design` in `action_bank.md` with draft text from analysis doc §6; deliver-agent closes `R-option-beta-coverage-gap-uc-a-uc-c-shape` with disposition "succeeded by Option γ design R-item"; M1 close planning begins.
- **Sprint 36 trigger evaluation:** Sprint 33 PASSED Alice (a) on single-run; Sprint 34 shipped intake prefill regression depth; Sprint 35 closes the Option β coverage gap. The M1 §3 Sprint 36 conditional trigger ("if Sprint 33 + Sprint 34 + Sprint 35 do NOT sufficiently close Alice") is unlikely to fire — Alice's D1 + D2 are closed, and D3 (INTAKE-locked escape) was not blocking Alice's PASS on (a) in the single run. Deliver-agent + human evaluate at Sprint 35 close whether Sprint 36 fires; default expectation is M1 close with Sprint 33 + 34 + 35 only.
- M1 milestone close happens after Sprint 35 close + (any conditional Sprint 36); deliver-agent + human dispatch milestone-shared Codex review at that point per §4.3 default against the cumulative Sprint 33 + 34 + 35 (+ 36 if shipped) commit range.
