Paste the content below this line into a fresh Claude Code session on branch `refactor/remove-the-shackles`.

---

You are the dev agent for Sprint 35 — the third sub-sprint of Milestone M1 (DISCOVER + Intake) per the 2026-05-16 governance upgrade to the milestone framework (`docs/current/iteration_governance.md` §8).

Sprint 35 is a **diagnostic / characterization sub-sprint**: you produce a probe CaseSpec corpus + an analysis matrix + a decision recommendation. You do NOT change production code, prompts, Java tests, or any existing eval surface. You ARE measuring the existing intake router's `(topic_subject, description-shape) → AMBIGUOUS|ROUTED` behaviour to close the Sprint 32-surfaced `R-option-beta-coverage-gap-uc-a-uc-c-shape` R-item.

**Framing B (probe + decision-execution).** At dev session close you commit the probe corpus + matrix + recommendation. The deliver-agent + human review and confirm decision (a) or (b) at sub-sprint close. If (a), the deliver-agent commits the `iteration_governance.md` §7.2 fold-back as a separate follow-up commit. You do NOT touch `iteration_governance.md`.

**Codex review:** deferred to M1 milestone-shared close per `iteration_governance.md` §4.3 default. You do NOT dispatch Codex at Sprint 35 close.

## 1. Loader

1. `AGENTS.md` (transitively loads `iteration_governance.md` §1 / §3 / §4 / §5 / §7 / §8 + `doc_governance.md` + `agent_context_guide.md`).
2. `docs/milestone_objective.md` — the M1 milestone north star. Read §3 Sprint 35 row (scope, hard fences, conditional §7.2 fold-back); §6 milestone hard fences; §7 R-items consumed/surfaced (`R-option-beta-coverage-gap-uc-a-uc-c-shape`).
3. `docs/sprint_objective.md` — your Sprint 35 contract. §2 goal + decisions (a)/(b); §5 file table + §5.1 probe corpus design; §6 hard fences; §8 §7 stanza; §9 success metrics; §10 stop conditions.
4. `docs/sprints/sprint-031-handoff.md` — Sprint 31 ship of `alternate_candidate_use_cases` projection slot (Option β implementation context).
5. `docs/sprints/sprint-032-handoff.md` §13 — the architectural finding that surfaced `R-option-beta-coverage-gap-uc-a-uc-c-shape`. UC-A↔UC-C drift falls outside Option β coverage (cross-topic); UC-A↔UC-FP neighbor #1 evidence works (same "Ad Support" topic).
6. `docs/sprints/sprint-033-handoff.md` + `docs/sprints/sprint-034-handoff.md` — Sprint 33 + 34 dev evidence. Sprint 33 shipped `discover_disambiguation_signals` slot at DISCOVER; Sprint 34 shipped UC-G/H/I/J intake prefill. Sprint 35 reads against the cumulative state.
7. `docs/current/iteration_governance.md` §1.4 (Runtime owns trace + eval contract — Sprint 35 IS this), §3.2 Q6 (eval_spec layer rationale), §4.1 (anti-hardcode kernel that Codex will walk at M1 close), §4.3 (Codex deferral rules), §5.5 / §5.6 (smoke demoted; bad-case suite as primary gate), §7 (sprint-objective stanza), §7.2 worked example (the candidate re-anchor target).
8. `docs/action_bank.md` — confirm `R-option-beta-coverage-gap-uc-a-uc-c-shape` is currently open; note its rationale + any deferral context.
9. `server/src/main/resources/config/use-case-registry.yaml` — the topic_subject → UC mapping. Confirm the weak-prior topics (Ad Support, Payments, Technical Support, Account Support) + strong-prior topics (Replies or Messaging, Delete My Account or Data, Report a Safety Issue) + handover-only topics.
10. `server/src/main/java/com/gumtree/csagent/service/runtime/UseCaseRouter.java` — read-only. Understand the routing decision shape (`RoutingResult{AMBIGUOUS|ROUTED}` + candidate list) so you can structure your matrix extraction recipe.
11. `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java` — read-only. Locate the `alternate_candidate_use_cases` slot population (Sprint 31). Note: Sprint 33 added `discover_disambiguation_signals` as a separate slot; do not confuse them. For the matrix you primarily care about `alternate_candidate_use_cases` (the Option β surface) + the per-turn `active_use_case`.
12. `eval_interactive/case_specs/bad_cases/_manifest.md` — the manifest convention you'll mirror for the new probe directory.
13. `eval_interactive/case_specs/case_families/sprint32_alternate_uc/` (any one CaseSpec) — the Sprint 32 alternate-UC CaseSpec shape for reference. Your probe CaseSpecs follow the standard schema PLUS the `probe_metadata` block specified in §5 of `docs/sprint_objective.md`.
14. `eval_interactive/case_spec/loader.py` or similar — confirm the loader will pick up your new probe directory at `eval_interactive/case_specs/probe/option_beta_coverage/` per `--path` invocation (no harness change needed).

## 2. Premise re-verification

Spot-check at session start (HEAD post-Sprint-34-close):

1. **`use-case-registry.yaml` topic mapping** — confirm the 4 weak-prior topics + 3 strong-prior topics + 4 handover-only topics match what §4 of `docs/sprint_objective.md` cites.
2. **`UseCaseRouter` at HEAD** — confirm `RoutingResult` carries `AMBIGUOUS|ROUTED` distinction + candidate list. Do NOT touch.
3. **`ContextProjectionBuilder.alternate_candidate_use_cases` slot** — confirm presence at HEAD (Sprint 31 ship + Sprint 32 case family).
4. **`R-option-beta-coverage-gap-uc-a-uc-c-shape` in `docs/action_bank.md`** — confirm OPEN.
5. **`iteration_governance.md` §7.2 worked example** — confirm at HEAD it names UC-A↔UC-C as the hypothetical Sprint 18 fix. This is the candidate re-anchor target for decision (a). You do NOT edit this file in dev session.
6. **`docs/diagnostics/` directory exists** — confirm before writing `docs/diagnostics/option_beta_coverage_matrix.md`.
7. **`eval_interactive/case_specs/probe/` directory does NOT yet exist** (you're creating it).

If any premise drifts, STOP and surface in handoff §3.

## 3. The work

### 3.1 Design the probe corpus

Per §5.1 of `docs/sprint_objective.md`:
- **Topic_subject axis (≤7):** 4 weak-prior + 3 strong-prior. Handover-only topics may be skipped.
- **Description_shape axis (5):** (1) single-issue, (2) multi-issue same-topic, (3) cross-topic drift, (4) ambiguous-soft, (5) empty/generic.

Sketch the dimension cube (~35 cells). Prune trivial / duplicate cells. **Document pruning rationale** in handoff §4 and analysis doc §2. Target final count: **~25-30 probe CaseSpecs.**

Recommended priority cells (do not skip these — they answer the Sprint 32 question):
- "Ad Support" × multi-issue same-topic (UC-A + UC-H + UC-FP mentioned) — directly tests Option β coverage on the Alice / Sprint 33 disambiguation surface
- "Ad Support" × cross-topic drift (UC-A mentioned in form + UC-C messaging mentioned in description) — directly tests the Sprint 32 finding; expected: ROUTED to one UC, no AMBIGUOUS surface, alternate slot empty for UC-C
- "Ad Support" × ambiguous-soft (the Alice shape: REMOVED listing + visibility question) — confirms Sprint 33 disambiguation slot fires on this surface
- "Payments" × multi-issue same-topic (UC-F + UC-I mentioned) — second multi-candidate topic; symmetric to "Ad Support"
- "Technical Support" × multi-issue same-topic (UC-E + UC-K mentioned) — third multi-candidate topic
- "Replies or Messaging" × ambiguous-soft (strong-prior control) — expected: ROUTED to UC-C, no alternates
- "Delete My Account or Data" × any shape (strong-prior control) — expected: ROUTED to UC-G

### 3.2 Author the probe CaseSpecs

Path: `eval_interactive/case_specs/probe/option_beta_coverage/<probe_id>.yaml`.

Each CaseSpec follows the standard CaseSpec schema. Add a `probe_metadata` block (analogous to `bad_case_metadata` in bad cases):

```yaml
case_id: probe_optbeta_ad_support_multi_issue_uc_a_uc_h
probe_metadata:
  probe_dimension_topic_subject: "Ad Support"
  probe_dimension_description_shape: "multi-issue-same-topic"
  probe_expected_routing: "AMBIGUOUS{UC-A, UC-H}"
  probe_purpose: "Confirm Option β surfaces UC-A + UC-H alternates when the form description mentions both visibility and appeal under Ad Support topic"
probe_observation_recipe: |
  Extract from per_turn_trace[0]:
    - active_use_case → matrix cell value (ROUTED to which UC)
    - alternate_candidate_use_cases slot contents → matrix cell value (AMBIGUOUS candidate list)
    - intent_routing.result → confirm AMBIGUOUS vs ROUTED
  Source: results/<timestamp>/results.json → case_results[case_id=...]
form_context:
  first_name: ProbeUser
  email: probe@example.com
  topic_subject: "Ad Support"
  description: "Why was my ad removed and how do I appeal the removal"
# ... rest of standard CaseSpec schema (user_persona, expected, etc.)
# DO NOT carry a regression rubric — the case_passed pass/fail signal is
# meaningless for a probe. The standard schema fields are present only to
# satisfy the loader.
```

For description-shape (3) cross-topic drift, the description deliberately mentions a UC outside the form_context.topic_subject — e.g., form topic "Ad Support" but description includes "and my messages aren't being delivered". This is the Sprint 32 finding-test cell.

Author ~25-30 probe CaseSpecs. Keep persona text deliberately generic (ProbeUser / probe@example.com / "AD-9001" etc. — NOT Alice/Bob/Carol or any prior-eval persona). Keep description text deliberately neutral — your goal is to probe the routing behaviour, not to test the LLM's grounding or persona handling.

### 3.3 Author the directory manifest

Path: `eval_interactive/case_specs/probe/option_beta_coverage/_manifest.md`.

Mirror the shape of `eval_interactive/case_specs/bad_cases/_manifest.md`. Sections:
- **Purpose** — "This directory holds the Sprint 35 probe CaseSpec corpus for the Option β coverage matrix. Probe CaseSpecs are diagnostic instruments, NOT regression gates."
- **Lifecycle** — opened 2026-05-16 for Sprint 35; expected to stay as a baseline reference after M1 close; future re-runs allowed; closure when superseded by Option γ probe (if applicable).
- **Governance pointer** — `docs/diagnostics/option_beta_coverage_matrix.md` is the analysis surface; `docs/sprint_objective.md` (Sprint 35) is the design surface; `docs/milestone_objective.md` (M1) is the milestone surface.
- **Per-case index** — table of probe_id → topic_subject × description_shape → expected_routing (one row per probe CaseSpec).

### 3.4 Run the probe corpus

Once all probe CaseSpecs are authored:

```bash
cd eval_interactive
uv run eval-interactive run --path case_specs/probe/option_beta_coverage/
```

Backend must be running at `http://localhost:8080` via `make backend` profile=local (real Moonshot/Kimi LLM endpoint per `application-local.yml`), same channel Sprint 33 used. **Per `feedback_mocked_llm_cannot_prove_prompt_causal_change.md`: do NOT mock the LLM for this run.** The routing decision in `UseCaseRouter` is deterministic Java (so the matrix's primary AMBIGUOUS/ROUTED cells are stable), but the LLM-behaviour layer (what the LLM does AFTER routing — does it use the alternate slot? does it ask clarifying?) is also part of the matrix and requires real LLM.

If any single probe case crashes, fails to load, or produces no trace: STOP. Diagnose. Do NOT silently skip and continue — a partial matrix is misleading.

### 3.5 Extract the matrix

For each probe CaseSpec, extract from `results/<timestamp>/results.json`:
- `case_results[case_id=<probe_id>].per_turn_trace[0].active_use_case`
- `case_results[case_id=<probe_id>].per_turn_trace[0].projection.alternate_candidate_use_cases` (or equivalent path)
- `case_results[case_id=<probe_id>].per_turn_trace[0].intent_routing` (or equivalent path)

(Verify the exact JSON paths against the actual `results.json` shape — Sprint 33 handoff §5 has an extraction recipe template you can adapt.)

Tabulate into the matrix in `docs/diagnostics/option_beta_coverage_matrix.md` §3.

### 3.6 Author the analysis document

Path: `docs/diagnostics/option_beta_coverage_matrix.md`. Front matter per §5 of `docs/sprint_objective.md`:

```yaml
---
title: Option β coverage matrix — (topic_subject, description-shape) → AMBIGUOUS/ROUTED
doc_tier: diagnostic
status: diagnostic
implementation_status: implemented
source_of_truth: this file
last_reviewed: 2026-05-16
review_cadence: ad hoc
supersedes: []
superseded_by: null
notes: >
  Sprint 35 diagnostic output. Characterizes the Option β
  (alternate_candidate_use_cases) coverage surface across the
  topic_subject × description-shape dimensions. Used to close
  R-option-beta-coverage-gap-uc-a-uc-c-shape with a re-anchor
  decision on iteration_governance.md §7.2 worked example OR an
  Option γ design R-item.
---
```

Body sections per §5 of `docs/sprint_objective.md`:
1. **Purpose** — what question the matrix answers, who reads it.
2. **Method** — probe corpus design rationale (pruning decisions), run protocol (backend + LLM config + harness invocation), extraction recipe.
3. **Raw matrix** — table (rows = topic_subjects, columns = description_shapes, cells = AMBIGUOUS{candidate_list} or ROUTED{uc} per the probe CaseSpec). Cite per-cell source (`results/<timestamp>/results.json:case_results[probe_id]`).
4. **Observations + analytical commentary** — per-row + per-column patterns; surprises; control-cell behaviour; whether the Sprint 32 finding is confirmed empirically.
5. **Recommended decision** — (a) re-anchor §7.2 worked example OR (b) open Option γ R-item. Cite per-cell evidence supporting the recommendation. If both seem warranted (per §10 stop condition #10 of `docs/sprint_objective.md`), pick (a) as primary and note (b) as follow-on under §6.
6. **If (b) recommended OR (a) PLUS (b) as follow-on: draft R-item text** for `R-option-gamma-alternate-uc-surveyor-design`. Include the standard R-item fields (id, surfaced_by, surfaced_date, what_it_addresses, possible_design_directions, deferred_to, prerequisites).

### 3.7 Author the handoff archive

Path: `docs/sprints/sprint-035-handoff.md`. Front matter per Sprint 34 archive shape (adjusted for Sprint 35):

```yaml
---
title: Sprint 35 handoff — Option β coverage probe + §7.2 worked-example re-anchor decision (M1 sub-sprint 3)
doc_tier: sprint-archive
status: archived
implementation_status: implemented
source_of_truth: this file
last_reviewed: 2026-05-16
review_cadence: ad hoc
supersedes: []
superseded_by: null
notes: >
  Sprint 35 is the third sub-sprint of Milestone M1 (DISCOVER + Intake)
  per `docs/milestone_objective.md`. Layer `eval_spec` per
  `docs/current/iteration_governance.md` §3.2 Q6. Probe / characterization
  sub-sprint; no production code change. Codex sprint-close review
  deferred to M1 milestone-shared close per §4.3 default. Closure
  verdict (§12) is left for the M1 milestone-shared decision; this
  archive captures the dev-session probe evidence + recommendation.
---
```

Section ordering and contents per §11 of `docs/sprint_objective.md` (12 sections; §12 is closure-verdict-placeholder, NOT a verdict).

## 4. Self-walk checklist before commit

1. **Probe corpus authored.** ~25-30 CaseSpecs; per-cell `probe_metadata` block; persona text generic; description text deliberate.
2. **Manifest written.** `_manifest.md` carries purpose / lifecycle / governance pointer / per-case index.
3. **Probe run completed end-to-end.** Every probe CaseSpec produced a trace in `results/<timestamp>/results.json`. No crashes. No silent skips.
4. **Matrix populated.** Every cell of `docs/diagnostics/option_beta_coverage_matrix.md` §3 carries a value + source citation.
5. **Decision recommended.** §5 of the analysis doc names (a) or (b) with cited per-cell evidence.
6. **Anti-hardcode self-walk.** Handoff §8 walks the §4.1 nine questions; expected verdict `approve`. Sprint 35 ships zero runtime / prompt / Java guard, so most Qs are N/A; document each.
7. **Java baseline unchanged.** Run full server test suite. 983/1-inherited/0/2 preserved. NO Sprint 35-attributable regression.
8. **Files-changed table.** Handoff §9 lists every staged file with paths + counts.
9. **§5 Eval Acceptance bars table.** Handoff §11 walks each bar adapted for a probe sprint.
10. **Closure verdict placeholder.** Handoff §12 explicitly defers verdict to M1 milestone-shared close per §4.3 default. DO NOT write a PASS/FAIL verdict yourself.

## 5. Bundle policy

Single commit with all dev-authored files:
- `eval_interactive/case_specs/probe/option_beta_coverage/_manifest.md`
- `eval_interactive/case_specs/probe/option_beta_coverage/*.yaml` (the ~25-30 probe CaseSpecs)
- `docs/diagnostics/option_beta_coverage_matrix.md`
- `docs/sprints/sprint-035-handoff.md`

Per `feedback_commit_at_end_bundles_deliver_artefacts.md`: do NOT stage deliver-agent-owned files (`docs/sprint_objective.md` / `docs/milestone_objective.md` / `docs/10-handoff.md` / `docs/action_bank.md` / `docs/codex-findings.md` / `compact/sprint-035-dev-prompt.md` / `docs/current/iteration_governance.md`). Human bundles those at deliver-agent commit.

## 6. Stop conditions (mirroring §10 of sprint_objective)

STOP and report when:

1. Premise drift on §4 items.
2. Tempted to edit any production code under `server/src/main/`.
3. Tempted to "fix" an observed coverage gap by editing `UseCaseRouter` or projection logic.
4. Tempted to edit `iteration_governance.md` §7.2 in the dev commit.
5. Tempted to author CaseSpecs in `case_families/` or `bad_cases/` instead of the new `probe/option_beta_coverage/` directory.
6. Tempted to invent runtime-attributable findings from the matrix that the matrix evidence does not support.
7. Probe corpus run fails to complete (crash / timeout / OOM / harness error).
8. The matrix reveals a clear control-cell violation (strong-prior topic produces AMBIGUOUS, etc.) — flag in handoff §7, do NOT fix.
9. Java test regression appears (Sprint 35 changes zero Java, so any regression is suspect — diagnose).
10. Tempted to recommend BOTH decision (a) AND decision (b) without disambiguation — pick one as primary, note the other as follow-on.

When you STOP, write your STOP rationale in handoff §3 or §7 and surface to the human.

## 7. Handoff document

Per §11 of `docs/sprint_objective.md`: 12-section shape; §12 is closure-verdict-placeholder. Adapted for a probe sprint:

- §5 reports probe corpus run results (not bad-case Alice trace).
- §6 generalization coverage table is adapted: target = matrix coverage; control cells; no shadow.
- §11 §5 Eval Acceptance bars are adapted: most read "N/A — probe sprint, no behaviour change" with cited rationale.

---

End of Sprint 35 dev prompt.
