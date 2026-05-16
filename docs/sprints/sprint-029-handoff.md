---
title: Sprint 29 dev handoff — (R2) probe follow-on, Track A CaseSpec authoring + Track B (c) phase-derivation verification
doc_tier: sprint-archive
status: current
implementation_status: implemented
source_of_truth: this file
last_reviewed: 2026-05-15
review_cadence: per sprint
supersedes: []
superseded_by: null
notes: >
  Sprint 29 lands the (R2) follow-on probe sprint that Sprint 27
  recommended and Sprint 28 unblocked. Track A authored four
  hand-authored CaseSpecs exercising D485.2 / D564.7 / D616.2
  preconditions; smoke ran at `eval_interactive/results/20260514-225419/`.
  Track B verified (c) phase-derivation on Track A's authored corpus;
  Q2 failed (no CONFIRM→CLOSE turn observable). In-flight downgrade
  fired per `docs/sprint_objective.md` §7.2; proposed follow-on R-item
  `R-per-turn-phase-transition-dump-for-smoke-harness` named in §6.3
  for a future sprint. `R-prompt-phase-plan-directive-followship` at
  `docs/action_bank.md:450` updated with Sprint 29 evidence; R-item
  NOT closed.
---

# Sprint 29 dev handoff — (R2) probe follow-on for `R-prompt-phase-plan-directive-followship`

## 1. Context Pack

### 1.1 Relevant docs (read or sampled)

| path | tier | inferred status | relevance |
|------|------|-----------------|-----------|
| `docs/sprint_objective.md` | current-runtime | current | Sprint 29 authoritative scope. Defines Track A authoring + Track B verification + §7.2 downgrade clause + §15 stop conditions. |
| `docs/current/iteration_governance.md` | durable-connective | current | §1 Constitution, §3 Fix Layer Classification, §5 Eval Acceptance Rules, §7 sprint-objective stanza. |
| `docs/current/doc_governance.md` | durable-connective | current | Tier model + source-of-truth rules. |
| `docs/sprints/sprint-027-handoff.md` | sprint-archive | historical | §6 (R2) recommendation + §11 target directives. |
| `docs/sprints/sprint-028-handoff.md` | sprint-archive | historical | §3 Option B choice (writer-side enrichment); §5 14-case smoke worked example; §7 open question 1 (proposed follow-on R-item name + scope). |
| `docs/sprints/sprint-018-handoff.md` | sprint-archive | historical | §5.1 CaseSpec L3 override check; §8.7 open-observation rule; §8.8 conditional-broadening rule. |
| `docs/sprints/sprint-020-handoff.md` | sprint-archive | historical | §3 G2 case-family authoring pattern; §3.11 authoring-discipline checks. |
| `eval_interactive/case_specs/case_families/_manifest.yaml` | reference | current | Sprint 20 G2 visible manifest; consulted to choose Sprint 29's manifest-placement strategy. |
| `docs/foundational/phase2_domain_realization_spec.md` | foundational | partial | UC-H §347–383, UC-K §467–504 referenced to align Sprint 29 INTAKE case shapes. |
| `docs/FAQ-knowledge_include_help_url.csv` | reference | current | Used to pick viable-hit articles for D616.2 (UC-C "Sending & Receiving Messages") and D485.2 (UC-B "How to Delete an Ad" / UC-C "Can I Turn Off Email Messages?"). |
| `docs/action_bank.md` line 450 | durable-connective | current | `R-prompt-phase-plan-directive-followship` disposition; Sprint 29 updates (not closes). |

### 1.2 Relevant code paths (sampled at session start, 2026-05-15)

- `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java` lines 411 (DISCOVER), 446 (CONFIRM), 482 (CLOSE), 517 (ESCALATE), 558 (INTAKE), 614 (RESOLVE FAQ). D485.2 `systemInstruction` literal at `PhaseEvaluator.java:488–490`; D564.7 escalation policy literal at `PhaseEvaluator.java:578` (`request_handover` reason `intakeCompleteTrigger(activeUc)`); D616.2 grounding/escalation policy literals at `PhaseEvaluator.java:625–650`.
- `server/src/main/java/com/gumtree/csagent/service/runtime/IntakeFieldsRegistry.java` lines 53–67 — canonical required-intake-fields registry: UC-G `[registered_email, data_request_type]`, UC-H `[ad_id_or_listing_url, registered_email, stated_reason_or_context]`, UC-I `[transaction_reference, dispute_reason]`, UC-J `[report_target, report_type, description]`, UC-K `[platform, repro_steps_or_error_message]`. Sprint 29 D564.7 case authored against the canonical UC-H field list.
- `eval_interactive/eval_interactive/case_spec/loader.py` line 144 — `load_case_specs` globs `*.yaml` directly under the passed directory (no recursion). Sprint 29 layout choice (flat + `_manifest.md`) is shaped by this constraint; see §4.1.
- `eval_interactive/eval_interactive/batch/sets.py` lines 17, 84–91 — `_KNOWN_SETS = ("anchor","promotion","exploration","smoke")` (unchanged; Sprint 29 adds no 5th); `load_custom(path)` dispatches single-file vs directory.
- `eval_interactive/eval_interactive/batch/executor.py` lines 314–361 — Sprint 28 `_build_per_turn_trace` helper. Track B consumes its output without modification.
- `eval_interactive/eval_interactive/simulator/session_runner.py` line 224 — `result.stop_reason = "goal_achieved"`. Sprint 29 Track B finding shows this stop fires before the bot's CONFIRM phase plan can run on D485.2 cases; documented in §6.2.

### 1.3 Doc-status warnings (drift observed)

- None new. Sprint 28's open question 1 (named `R-per-turn-phase-transition-dump-for-smoke-harness`, scope: extend `per_turn_trace[i]` with `phase_before`, `phase_after`, `turn_index`) is the Sprint 28 §7 follow-on Track B's §6.3 proposes; the name carries forward.
- The Sprint 27 §11 question on D517.2 tautology recap is unchanged; Sprint 29 leaves it as the human's call per the sprint_objective §3 non-goal.

### 1.4 Source-of-truth decision

For Sprint 29's behaviour:

- **Track A authoring discipline** — `docs/current/iteration_governance.md` §1.7 forbidden list + Sprint 18 §5.2 Method note. Code (CaseSpec schema) is the syntactic source of truth; the authoring discipline is governance.
- **Track B verification** — the produced `eval_interactive/results/20260514-225419/results.json` is the authoritative artefact. All quantitative claims in §5–§7 cite a jq/python invocation against this file.
- **R-item disposition update** — `docs/action_bank.md` line 450 entry, edited to refine disposition only. R-item NOT closed.

### 1.5 Implementation status

| behaviour | classification | evidence |
|-----------|----------------|----------|
| N≥3 hand-authored CaseSpecs exercising D485.2 / D564.7 / D616.2 preconditions | implemented | 4 YAMLs under `eval_interactive/case_specs/case_families/sprint29_directive_probe/`; schema-loads via `load_case_specs`; see §4.1. |
| Smoke run via `--path` against real-LLM bot | implemented | `eval_interactive/results/20260514-225419/results.json` (run-id `20260514-225419`, elapsed 51 774 ms, 4 cases, 40 LLM calls total). |
| Track B Q1–Q4 answered with reproducible extraction | implemented | §6.1 every question carries a reproducible jq invocation. |
| Track B in-flight downgrade fired | implemented | §6.2 documents Q2=NO failure mode; §6.3 names proposed follow-on R-item; no retry, no scope expansion. |
| `R-prompt-phase-plan-directive-followship` disposition refined | implemented | §7 + `docs/action_bank.md:450` edit. |
| `R-prompt-phase-plan-directive-followship` NOT closed | implemented | line 450 entry retains "open" status. |

### 1.6 Risks before coding

(Carried into Track A authoring; cited here for audit.)

1. **Trace-text encoding** — risk of pulling phrases from cs_259 / manual-probe / cs_011 T2 (the three R-item-instances at line 450) into Sprint 29 CaseSpec `hidden_facts`. Mitigated: §4.3 §6.2 self-check.
2. **Rubric widening on bot failure** — risk of post-hoc relaxing `expected_tool_sequence` or `escalation_trigger` when bot fails. Mitigated: dev-prompt §13 stop condition + §15 stop conditions; the failure is the finding.
3. **Sprint 20 cascade fence** — risk of accidentally editing `cs001_*..cs259_*`. Mitigated: §9 file-list audit.
4. **Track B retry temptation** — risk of re-running smoke with modified scope when Q2 fails. Mitigated: §6.2 honours the §7.2 in-flight downgrade clause; no retry.
5. **cs_040 routing surface** (Sprint 22 fence) — risk of authoring a UC-K case that re-triggers the disengaged-jargon false-intake. Mitigated: Sprint 29's INTAKE case is UC-H, not UC-K; cs_040 surface untouched.

## 2. Sprint-objective recap

Per `docs/sprint_objective.md` (current): Sprint 29 is the **(R2) targeted probe sprint follow-on** Sprint 27 recommended and Sprint 28 unblocked. Two parallel tracks:

- **Track A — Author (R2) CaseSpecs.** N≥3 hand-authored CaseSpecs total across D485.2 / D564.7 / D616.2. Per-case observation: precondition fires (yes/no) AND directive followed (yes/no).
- **Track B — Verify (c) phase-derivation.** After Track A's smoke, extract per-turn `phase_plan.phase` deltas and answer four questions about whether the derivation rule (`phase_before[N] = phase_plan.phase[N-1]`, `phase_after[N] = phase_plan.phase[N]`) supports the consumer's load-bearing case (D485.2 CLOSE-entry detection).

Sprint 29 is **semantic-touching** (`eval_spec`-layer authoring) — §7 stanza required, multi-layer prospective per-track. The §11 stanza in `docs/sprint_objective.md` names Track A primary layer as `eval_spec` and Track B primary layer as `infra` (verify-only), with `human_review_required` as the Track B fallback if the in-flight downgrade clause fires.

The in-flight downgrade clause **did fire** on Track B (Q2=NO; details in §6.2). Per §7.2: Track A still lands; Track B records the downgrade + the proposed follow-on R-item name + scope; no mid-sprint retry.

## 3. Premise re-verification

§4 of `docs/sprint_objective.md` carries 8 verified premise items + an empirical premise check. The dev re-confirmed at session start (HEAD = `df8b8cd`). No drift observed; do NOT re-derive. Reproducible spot-checks (one per item):

1. **Case-family directory + manifest.** `ls eval_interactive/case_specs/case_families/` → 10 family directories + `_manifest.yaml` (12.3 KB). ✓
2. **Sprint 20 hidden_facts + expected_behaviors pattern.** Read `eval_interactive/case_specs/case_families/cs011_uc_d_description_ignored/neighbor/cs11n01_uc_d_2fa_loop_verbose.yaml` (70 lines); confirmed `case_id`, `source_session_id`, `source_dataset: case_family_authored`, `form_context`, `persona.hidden_facts` (list of `{fact, disclose_when}` dicts), `expected.*` structured, `scoring.*`. ✓
3. **`--path` flag** at `eval_interactive/eval_interactive/cli.py:142` (`--path` → `custom_path`). Re-read; intact. ✓
4. **`_KNOWN_SETS`** at `eval_interactive/eval_interactive/batch/sets.py:17` = `("anchor","promotion","exploration","smoke")`. Re-read; 4 entries, no 5th added by Sprint 29. ✓
5. **PhaseEvaluator branch lines** at `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java` lines 411 / 446 / 482 / 517 / 558 / 614. Spot-confirmed CONFIRM @ 446 + CLOSE @ 482 + RESOLVE FAQ @ 614. ✓
6. **`record_outcome` tool schema** at `RecordOutcomeTool.java:28–94`; accepts `outcome_class` ∈ {resolve, escalate, abandon} (or legacy aliases). CLOSE allowedTools `[record_outcome]` confirmed at `PhaseEvaluator.java:485`. ✓
7. **`intake_complete_for_uc_*` enum values** at `PhaseEvaluator.java:39–63`. Sprint 29's D564.7 case authors against `intake_complete_for_uc_h`, a member of the canonical 23-value set. ✓
8. **FAQ knowledge index** at `docs/FAQ-knowledge_include_help_url.csv` (3 964 lines, 219 parsed rows after CSV unwrapping). Articles used by Sprint 29 cases: `ka4P200000004JlIAI` (How to Delete an Ad), `ka4P200000003sLIAQ` (Can I Turn Off Email Messages?), `ka4P20000000421IAA` (Sending & Receiving Messages) — all confirmed present. ✓

   Reproducibility for the two numbers above:

   ```
   # 3 964 lines (raw line count):
   wc -l docs/FAQ-knowledge_include_help_url.csv
   # -> 3964 docs/FAQ-knowledge_include_help_url.csv

   # 219 parsed rows (after CSV multi-line-cell unwrapping):
   python3 -c "import csv; rows = list(csv.reader(open('docs/FAQ-knowledge_include_help_url.csv'))); print(len(rows) - 1)"
   # -> 219
   ```

**Empirical premise check (Sprint 28 smoke).** Re-confirmed via:

```
jq '[.case_results[].per_turn_trace[] | {phase: (.phase_plan.phase // "NULL")}] | group_by(.phase) | map({phase: .[0].phase, count: length})' \
  eval_interactive/results/20260514-181257/results.json
```

Output: `DISCOVER: 1`, `NULL: 1` (cs_029 third defensive shape), `RESOLVE: 21`. Zero phase transitions in Sprint 28's smoke. This is the corpus-undecidability finding that Sprint 29 set out to address by authoring directed cases. ✓

**Conclusion.** No premise drift. Track A authoring proceeds against the 8 verified premise items.

## 4. Track A — CaseSpec authoring

### 4.1 Files authored + layout choice

Layout choice: **option (i) — combined directory `sprint29_directive_probe/`**, flat (no subdirs per directive). Rationale:

- Option (ii) (three separate top-level case-families) would imply each directive is a Sprint 20 G2-style family with target / neighbor / negative / shadow. Sprint 29 is a (R2) probe — only `target` cases per `docs/sprint_objective.md` §11 — and the G2 schema does not fit.
- Initial draft used subdirs (`d485_2/`, `d564_7/`, `d616_2/`) for organisation. `load_case_specs(p)` in `eval_interactive/eval_interactive/case_spec/loader.py:144` globs `*.yaml` **directly under** the passed directory; subdirs were not discovered. Flattening to a single directory + directive-prefixed filenames keeps the smoke-run invocation single-shot (`--path case_specs/case_families/sprint29_directive_probe/`) without re-running per subdir.
- Manifest choice: **new manifest `_manifest.md` (markdown)** inside the Sprint-29 directory, not an append to the Sprint 20 G2 root `_manifest.yaml`. Reasons: (a) Sprint 29's probe shape (target only; no neighbor / negative / shadow) does not fit the G2 schema; (b) keeping the file as `.md` avoids the loader's `*.yaml` glob — the loader skips it cleanly. Documented inside `_manifest.md` itself.

Four CaseSpec YAMLs authored (one per directive minimum; D485.2 has 2 cases to give Track B's load-bearing CLOSE-entry question more than one chance to observe):

| path | directive | primary_uc | expected outcome_class | provenance |
|------|-----------|-----------|------------------------|------------|
| `eval_interactive/case_specs/case_families/sprint29_directive_probe/cs29d485_uc_b_delete_ad_resolution.yaml` | D485.2 | UC-B | resolve | `source_dataset: case_family_authored`, `source_session_id: synthetic-sprint29-d485-2-01` |
| `eval_interactive/case_specs/case_families/sprint29_directive_probe/cs29d485_uc_c_email_notifications_resolution.yaml` | D485.2 | UC-C | resolve | `source_dataset: case_family_authored`, `source_session_id: synthetic-sprint29-d485-2-02` |
| `eval_interactive/case_specs/case_families/sprint29_directive_probe/cs29d564_uc_h_ad_removal_appeal_complete_intake.yaml` | D564.7 | UC-H | escalate | `source_dataset: case_family_authored`, `source_session_id: synthetic-sprint29-d564-7-01` |
| `eval_interactive/case_specs/case_families/sprint29_directive_probe/cs29d616_uc_c_message_delivery_mild_frustration.yaml` | D616.2 | UC-C | resolve | `source_dataset: case_family_authored`, `source_session_id: synthetic-sprint29-d616-2-01` |

Plus `_manifest.md` documenting layout choice + per-case shape + authoring-discipline self-check.

N = 4 (≥ 3 floor; below 6 suggested upper bound). Schema-load verified via:

```
cd eval_interactive && uv run python -c "
from eval_interactive.case_spec.loader import load_case_specs
from pathlib import Path
specs = load_case_specs(Path('case_specs/case_families/sprint29_directive_probe'))
print(len(specs))
"
```

Output: `4`.

### 4.2 Per-directive design rationale (informational per sprint_objective §6.3)

- **D485.2 — CLOSE record_outcome-if-not-already-recorded.** Two cases authored (UC-B `cs29d485_uc_b_delete_ad_resolution`, UC-C `cs29d485_uc_c_email_notifications_resolution`). Both use a FAQ-resolvable resolve-path UC with a viable hit reachable in `docs/FAQ-knowledge_include_help_url.csv` (UC-B: How to Delete an Ad; UC-C: Can I Turn Off Email Messages?). User persona is terse + no frustration so the user simulator does not pre-empt the resolution path. `seed_messages[1]` is a clear satisfaction signal so the post-RESOLVE phase transitions through CONFIRM into CLOSE per `docs/sprint_objective.md` §4 premise item 5. Two cases give Track B Q2/Q3 two chances; one case would have been within the §3 floor.
- **D564.7 — INTAKE intake-complete handover.** One case authored (UC-H `cs29d564_uc_h_ad_removal_appeal_complete_intake`). UC-H required intake fields per `IntakeFieldsRegistry.java:59` are `[ad_id_or_listing_url, registered_email, stated_reason_or_context]`. The form context supplies `ad_id` (AD-29447) and `email`; the user's first message supplies the appeal `stated_reason_or_context`. All three fields are reachable inside one user turn, so the bot should call `request_handover(intake_complete_for_uc_h)` inside two bot turns. Expected `escalation_trigger: intake_complete_for_uc_h` is the canonical 23-value enum member at `PhaseEvaluator.java:48`.
- **D616.2 — RESOLVE FAQ premature-escalation prohibition.** One case authored (UC-C `cs29d616_uc_c_message_delivery_mild_frustration`). UC-C is a resolve-path FAQ-grounded UC (per phase 2 §UC-C-01); the user describes a messaging-delivery problem that maps to the "Sending & Receiving Messages" knowledge surface (article id `ka4P20000000421IAA`). The persona's `frustration_level: mild` is intentional — D616.2's prohibition is that mild frustration alone is NOT sufficient for premature `request_handover(faq_miss_threshold_exceeded)` before a valid resolve attempt. The user persona signals it will request human help **only** if the bot escalates without trying to answer (so the harness measures whether the bot honors the "valid resolve attempt first" gate).

### 4.3 Authoring-discipline self-check (per `docs/sprint_objective.md` §6.2)

Each rule verified across all 4 CaseSpecs by inspection:

1. **No trace-specific text in `hidden_facts`.** Only `cs29d564_uc_h` and `cs29d616_uc_c` have non-empty `hidden_facts`; both describe user-side observable situation (UC-H: no specific policy reason on removal email; UC-C: six months of prior success, user sees own sent messages). Neither copies bot transcript text. ✓
2. **No keyword / regex preconditions in `expected.*`.** Every `expected.*` field is structured: `outcome_class`, `primary_uc`, `secondary_ucs`, `should_escalate`, `escalation_trigger`, `expected_tool_sequence`, `forbidden_tools`, `grounding_mode`, `answer_must_not_contain`, `max_turns`. `bot_handling_pattern` is prose describing what the bot should do (Sprint 20 G2 precedent — cs011 / cs038 / cs092 all use this shape). No regex; no keyword precondition. ✓
3. **No CaseSpec id mirrored into runtime / prompt / judge code.** Sprint 29 ships zero `.java` / `.py` / `.properties` / prompt / judge-config edits — see §9. ✓
4. **No rubric widening to accept a known bot mistake.** Track A's smoke surfaces three directive-followship deviations (§5.2); none of the four CaseSpecs were edited post-smoke to make the deviation "pass". The deviation is the finding. ✓
5. **No L3 override authored** for any new case. `eval_interactive/case_spec_overrides.yaml` not touched. ✓
6. **No editing of existing case families.** `eval_interactive/case_specs/case_families/cs{001,011,015,038,040,095,176,192,259}_*` and `manual_probe_*` untouched. Sprint 20 cascade fence observed. ✓
7. **Provenance fields correct.** Every case has `source_dataset: case_family_authored` + `source_session_id: synthetic-sprint29-<directive>-<idx>`. ✓
8. **`escalation_trigger` membership.** Where `should_escalate: true` (cs29d564 only), trigger is `intake_complete_for_uc_h` ∈ the canonical 23-value `EscalationTrigger` set at `PhaseEvaluator.java:39–63`. Where `should_escalate: false`, trigger is `null`. ✓

## 5. Track A — Smoke run results

### 5.1 Run command + result file path

```
cd eval_interactive && \
  uv run eval-interactive run \
    --path case_specs/case_families/sprint29_directive_probe/ \
    --label sprint-29-r2-probe
```

Bot: local Spring Boot at `http://localhost:8080`. Pre-run health check: `curl -sS -o /dev/null -w "HTTP %{http_code}\n" http://localhost:8080/v1/demo/sessions` → `HTTP 200`.

Run complete `2026-05-15` in **51 774 ms** (CLI literal: `Batch run 'sprint-29-r2-probe' complete in 51774ms`). Run-id **`20260514-225419`**. Result: `eval_interactive/results/20260514-225419/results.json` (142.2 KB, 4 case-results, Sprint 28 `per_turn_trace[]` schema present per §5.2 Recipe E).

Reproducibility for the two numbers above:

```
# 51 774 ms (top-level elapsed_ms in the result file):
jq '.elapsed_ms' eval_interactive/results/20260514-225419/results.json
# -> 51774

# 142.2 KB (file size, KiB with one decimal):
wc -c eval_interactive/results/20260514-225419/results.json | awk '{printf "%.1f KB\n", $1/1024}'
# -> 142.2 KB   (raw bytes: 145568)
```

CLI summary line: `Total: 4 Passed: 0 Failed: 4 Success rate: 0.0%`. The 0 pass-rate is expected for an authoring probe — Sprint 29 is **not** measuring `composite_score`, it is measuring **per-directive followship**. The judges' `mean_judge: 0.8000` + `escalation_correct: 25.0%` + `mean_outcome: 0.8007` confirm the bot engaged meaningfully on every case; the `composite=0` rows come from L1/L2 hard-check failures that are themselves the directive-followship findings.

Reproducibility for the three judge / outcome numbers above (all read from the canonical `.summary` block in the result file):

```
jq '.summary | {mean_judge_score, escalation_correctness, mean_outcome_score}' \
  eval_interactive/results/20260514-225419/results.json
# -> {
#      "mean_judge_score": 0.8,
#      "escalation_correctness": 0.25,
#      "mean_outcome_score": 0.8007
#    }
```

Mapping: `mean_judge_score: 0.8` → `mean_judge: 0.8000`; `escalation_correctness: 0.25` → `escalation_correct: 25.0%`; `mean_outcome_score: 0.8007` → `mean_outcome: 0.8007`.

### 5.2 Per-case observation table

Per-case extraction via:

```
jq -r '.case_results[] | "case=\(.case_id) | turns=\(.total_turns) | ptt_len=\(.per_turn_trace | length) | stop_reason=\(.stop_reason) | escalation_reason=\(.escalation_reason) | containment=\(.containment_outcome)"' \
  eval_interactive/results/20260514-225419/results.json
```

Output:

```
case=cs29d485_uc_b_delete_ad_resolution            | turns=3 | ptt_len=1 | stop_reason=goal_achieved | escalation_reason=             | containment=
case=cs29d485_uc_c_email_notifications_resolution  | turns=2 | ptt_len=1 | stop_reason=bot_ended     | escalation_reason=service_degraded | containment=escalated
case=cs29d564_uc_h_ad_removal_appeal_complete_intake | turns=3 | ptt_len=2 | stop_reason=bot_ended     | escalation_reason=intake_complete_for_uc_h | containment=escalated
case=cs29d616_uc_c_message_delivery_mild_frustration | turns=2 | ptt_len=1 | stop_reason=bot_ended     | escalation_reason=user_distress | containment=escalated
```

Per-case phase + tool-sequence (Sprint 28 `per_turn_trace[]` schema):

```
jq -r '.case_results[] | "=== \(.case_id) ===", (.per_turn_trace[] | "  phase=\(.phase_plan.phase // "NULL") tools=\([.tool_calls[].tool_name] | join(","))")' \
  eval_interactive/results/20260514-225419/results.json
```

Output:

```
=== cs29d485_uc_b_delete_ad_resolution ===
  phase=RESOLVE tools=classify_use_case,search_knowledge,resolve_article,record_outcome
=== cs29d485_uc_c_email_notifications_resolution ===
  phase=RESOLVE tools=search_knowledge,resolve_article,record_outcome,request_handover
=== cs29d564_uc_h_ad_removal_appeal_complete_intake ===
  phase=RESOLVE tools=classify_use_case
  phase=RESOLVE tools=request_handover,request_handover,create_case_controlled
=== cs29d616_uc_c_message_delivery_mild_frustration ===
  phase=RESOLVE tools=search_knowledge,resolve_article,request_handover
```

Synthesised observation table (precondition fires / directive followed columns are the deliverable per `docs/sprint_objective.md` §6.1 + §12):

| case_id | directive | bot turns | precondition fires? | directive followed? | notes |
|---------|-----------|-----------|---------------------|---------------------|-------|
| `cs29d485_uc_b_delete_ad_resolution` | D485.2 | 1 (RESOLVE only) | **NO** | N/A | Bot delivered a grounded answer with article citation (`ka44J000000gL0FQAU` "Deleting Ads") in 1 RESOLVE turn including `record_outcome`. User simulator returned a satisfaction line (`"Thank you, that helps."`) on the next turn, and the harness stopped on `stop_reason=goal_achieved` (per `eval_interactive/eval_interactive/simulator/session_runner.py:224`) **before** the bot was given a turn to run the CONFIRM phase plan. The case therefore never reached CLOSE; D485.2's precondition (`phase==CLOSE`) did not fire. |
| `cs29d485_uc_c_email_notifications_resolution` | D485.2 | 1 (RESOLVE only) | **NO** | N/A | Bot called `search_knowledge` (3 hits) → `resolve_article` (resolved `ka4P200000003sLIAQ` "Can I Turn Off Email Messages?"; viable hit reached). The subsequent `record_outcome` call errored (`result_summary: error: [REDACTED_TOKEN]`); the bot then escalated with bot-requested `escalation_reason: service_degraded` (an `infra`-degraded path, not a directive-followship decision). Session terminated in handover without entering CONFIRM/CLOSE; D485.2 precondition did not fire. |
| `cs29d564_uc_h_ad_removal_appeal_complete_intake` | D564.7 | 2 | **YES** | **YES** | Turn 0: bot called `classify_use_case` → UC-H, confidence 0.90. Turn 1: bot collected the missing `registered_email` from the user (`"It's helena.smith@example.com."`) and called `request_handover` with `escalation_reason: intake_complete_for_uc_h`. Tool-call result: `handover intake_complete_for_uc_h -> transferred`. The canonical D564.7 success shape — escalation with the canonical `intake_complete_for_uc_h` reason once the three required fields per `IntakeFieldsRegistry.java:59` were collected. Minor observation: bot asked for `registered_email` even though it was present in `form_context.email`; the form-context value was not directly recognised by the bot as the registered email, requiring one extra clarification turn. |
| `cs29d616_uc_c_message_delivery_mild_frustration` | D616.2 | 1 (RESOLVE only) | **YES** | **NO** | Bot called `search_knowledge` (3 hits) → `resolve_article` (resolved `ka44J000000TetlQAC` "I Can't Send Replies"; viable hit reached). Bot did NOT then produce a grounded answer to the user; instead called `request_handover` with bot-requested `escalation_reason: user_distress` (runtime persisted `user_distress` at the case level). The session ended in handover; bot text was `"Let me connect you with a specialist."` only — no grounded answer surface delivered. D616.2's directive ("Only escalate via request_handover after a valid resolve attempt cannot complete") is shaped to prohibit exactly this — a viable hit reached, resolve_article succeeded, but the bot escalated without producing the grounded user-facing answer. **D616.2 directive NOT followed.** |

Tool-call result details (reproducibility):

```
# cs29d564_uc_h request_handover arguments
jq -r '.case_results[] | select(.case_id=="cs29d564_uc_h_ad_removal_appeal_complete_intake") | .per_turn_trace[1].tool_calls[] | select(.tool_name=="request_handover" and .success==true) | .arguments' \
  eval_interactive/results/20260514-225419/results.json
# -> {"escalation_reason":"intake_complete_for_uc_h"}

# cs29d616_uc_c request_handover arguments (bot-requested reason)
jq -r '.case_results[] | select(.case_id=="cs29d616_uc_c_message_delivery_mild_frustration") | .per_turn_trace[0].tool_calls[] | select(.tool_name=="request_handover") | .arguments' \
  eval_interactive/results/20260514-225419/results.json
# -> {"escalation_reason":"user_distress"}

# cs29d485_uc_c record_outcome error
jq -r '.case_results[] | select(.case_id=="cs29d485_uc_c_email_notifications_resolution") | .per_turn_trace[0].tool_calls[] | select(.tool_name=="record_outcome") | "summary=\(.result_summary) success=\(.success)"' \
  eval_interactive/results/20260514-225419/results.json
# -> summary=error: [REDACTED_TOKEN] success=false

# cs29d564_uc_h classify_use_case confidence on turn 0 (cited in the table row above as "confidence 0.90")
jq -r '.case_results[] | select(.case_id=="cs29d564_uc_h_ad_removal_appeal_complete_intake") | .per_turn_trace[0].tool_calls[] | select(.tool_name=="classify_use_case") | "args=\(.arguments) result=\(.result_summary)"' \
  eval_interactive/results/20260514-225419/results.json
# -> args={"reasoning":"...","confidence":0.9,"use_case_id":"UC-H"} result=uc=UC-H conf=0.90
```

### 5.3 Per-directive aggregate

| directive | cases authored | precondition fired (n) | directive followed (n) | confirmed direction |
|-----------|----------------|------------------------|------------------------|---------------------|
| D485.2 (CLOSE record_outcome-if-not-already-recorded) | 2 | **0 / 2** | N/A | **undecidable on Track A's corpus** — neither case reached CLOSE. Track B Q2 fails on the same data point (§6.2). |
| D564.7 (INTAKE intake-complete handover) | 1 | 1 / 1 | **1 / 1** | **directive followed.** Bot escalated with canonical `intake_complete_for_uc_h` once all three UC-H intake fields were collected. One observation; n=1 is below the §8.7 conditional-broadening threshold; recorded as a **single-case followship confirmation** rather than as a pattern. |
| D616.2 (RESOLVE FAQ premature-escalation prohibition) | 1 | 1 / 1 | **0 / 1 (violation observed)** | **directive NOT followed in 1 case.** Bot reached a viable hit (`resolve_article` returned `ka44J000000TetlQAC`) but escalated with `user_distress` instead of producing the grounded answer. One observation; n=1; per §8.7 open-observation rule **insufficient to broaden** to a systematic R-item, but **strong enough to refine the line-450 disposition** with an instance pointer (§7). |

**Net Track A finding** vs Sprint 27 §6 (R2) recommendation goal: D564.7 followship is confirmed once; D616.2 violation is observed once; D485.2 remains structurally undecidable on a smoke-harness corpus that stops on `goal_achieved` before the bot's CONFIRM phase can run. The D485.2 undecidability is a **new finding** — Sprint 28's premise check showed the existing 14-case smoke never reached CONFIRM/CLOSE; Sprint 29 shows that even when the case is designed to drive the bot to CONFIRM/CLOSE, the user-simulator's `goal_achieved` early-stop pre-empts the bot's CONFIRM phase plan. This finding shapes Track B §6.3's proposed follow-on R-item scope.

## 6. Track B — (c) phase-derivation verification

### 6.1 Questions 1–4 with reproducible extraction

**Q1. Is `phase_plan` populated on every bot turn of every Track A authored case?**

Extraction:

```
jq '[.case_results[].per_turn_trace[] | {phase: (.phase_plan.phase // "NULL")}] | group_by(.phase) | map({phase: .[0].phase, count: length})' \
  eval_interactive/results/20260514-225419/results.json
```

Output: `[{"phase":"RESOLVE","count":5}]`.

5 populated bot turns total across the 4 cases (1 + 1 + 2 + 1); every one has `phase_plan.phase != null`. **Q1 = YES.** Sprint 28's third defensive shape (populated trace, NULL `phase_plan`) did NOT appear in Track A's corpus; defensive-skip path was not exercised.

**Q2. Does the consumer's load-bearing case (D485.2 CLOSE entry: a `CONFIRM → CLOSE` turn) actually appear in Track A's authored corpus?**

Extraction (per Q1 above): every populated turn has `phase_plan.phase == "RESOLVE"`. Zero turns have `phase_plan.phase ∈ {DISCOVER, CONFIRM, CLOSE, INTAKE, ESCALATE}`.

**Q2 = NO.** No CONFIRM turn observable; no CLOSE turn observable. The two D485.2 cases (authored specifically to drive the bot to CONFIRM/CLOSE) both terminated in RESOLVE-end-state: `cs29d485_uc_b` via simulator `goal_achieved` early-stop before the bot's CONFIRM phase plan ran; `cs29d485_uc_c` via `service_degraded` handover after a `record_outcome` error path.

**Q3. If yes to Q2, do `phase_plan.phase[N-1] == "CONFIRM"` and `phase_plan.phase[N] == "CLOSE"` correctly identify the CLOSE entry turn?**

**Q3 = N/A.** Q2 = NO; no CONFIRM → CLOSE turn pair is present in Track A's authored corpus, so the derivation rule cannot be evaluated on the load-bearing question. The rule's *form* is sound (`phase[N-1] = "CONFIRM"` ∧ `phase[N] = "CLOSE"` ↔ "this is the CLOSE entry turn") given the §4 premise item 5 CONFIRM → CLOSE transition condition; but **the rule cannot be exercised against Track A's corpus because the corpus contains no CONFIRM → CLOSE turn pair**.

**Q4. Are there any other phase transitions in Track A's authored corpus (e.g. `DISCOVER → RESOLVE`, `RESOLVE → CONFIRM`, `RESOLVE → ESCALATE`) where derivation could fail in a way that matters for a future consumer?**

The corpus shows zero turn-to-turn phase deltas — every `phase_plan.phase[N-1] → phase_plan.phase[N]` pair is `RESOLVE → RESOLVE` (across the cs29d564 case's two-turn sequence; the other three cases have only one bot turn). Reproducible:

```
jq -r '.case_results[] | select(.case_id=="cs29d564_uc_h_ad_removal_appeal_complete_intake") | [.per_turn_trace[].phase_plan.phase] | "[\(join(","))]"' \
  eval_interactive/results/20260514-225419/results.json
# -> [RESOLVE,RESOLVE]
```

A **secondary observation** worth surfacing for Sprint 30 visibility (NOT a Sprint 29 R-item proposal): cs29d564_uc_h is on UC-H (an INTAKE UC per phase 2 §UC-H-01 and per `IntakeFieldsRegistry.java:110` `isIntakeUseCase("UC-H") == true`). The bot's behaviour on turn 1 (`request_handover` with `intake_complete_for_uc_h`) is consistent with the INTAKE phase plan at `PhaseEvaluator.java:558`. Yet `phase_plan.phase` in the recorded `per_turn_trace` shows `RESOLVE`, not `INTAKE`. Two interpretations:

- (a) `phase_plan` in `per_turn_trace[i]` snapshots the projection at the **end** of bot-turn execution rather than the start. Post-handover the runtime may transition out of INTAKE and the snapshot reads the post-state.
- (b) `phase_plan` in `per_turn_trace[i]` was set before the bot's `classify_use_case` ran (e.g. on the prior projection build at session start when UC was still unresolved). The bot may not have been "in" INTAKE phase at the moment the projection was assembled.

Either interpretation would explain the absence of CONFIRM / CLOSE / INTAKE / ESCALATE values in the corpus. Both interpretations point at the same root cause: **the `phase_plan` field in `per_turn_trace[i]` is a single-point snapshot of one phase plan, not a record of the phase the bot's loop actually executed on this turn.** This is the underlying reason Track B Q2/Q3 cannot be answered against the existing data shape, even in cases where the bot's behaviour does correspond to a non-RESOLVE phase plan.

**Q4 conclusion.** No observable transitions. No new derivation-failure mode beyond Q2/Q3's already-named failure. The secondary observation above sharpens the §6.3 follow-on R-item's scope.

### 6.2 Verdict — derivation does NOT work on Track A's corpus

**Verdict: derivation fails on the load-bearing question.** Track B's primary deliverable per `docs/sprint_objective.md` §7.1 and §12 is to confirm whether `phase_plan.phase[N-1] == "CONFIRM"` ∧ `phase_plan.phase[N] == "CLOSE"` identifies the CLOSE entry turn for a future D485.2 directive-followship consumer. The corpus is undecidable on this question because no CONFIRM → CLOSE turn pair appears.

Two distinct mechanisms suppressed the CONFIRM → CLOSE pair on the two D485.2 cases authored specifically to drive it:

1. **User simulator `goal_achieved` early-stop** (cs29d485_uc_b). After the bot delivered the grounded answer in 1 RESOLVE turn, the user simulator's third message was the satisfaction line `"Thank you, that helps."` The simulator's stop-condition at `eval_interactive/eval_interactive/simulator/session_runner.py:224` set `result.stop_reason = "goal_achieved"` and the session terminated before the bot was sent a turn-3 message to interpret. The bot therefore never ran the CONFIRM phase plan at `PhaseEvaluator.java:446`; consequently no `phase_plan.phase == "CONFIRM"` snapshot exists in the trace.
2. **`record_outcome` infra error path** (cs29d485_uc_c). Bot reached a viable resolve_article hit, but the `record_outcome` tool call returned an error (`result_summary: error: [REDACTED_TOKEN]`); the runtime degraded the path and the bot escalated with `escalation_reason: service_degraded`. The session terminated in handover; CONFIRM was never entered.

The third defensive shape Sprint 28 §5 documented (cs_029 turn 1 — populated trace, NULL `phase_plan`) did not appear in Track A's corpus.

**In-flight downgrade fires per `docs/sprint_objective.md` §7.2.** Track A still lands; Track B records the downgrade + the proposed follow-on R-item name + scope (§6.3). Per §15: **no mid-sprint retry** is performed — running smoke with a modified user simulator (e.g. forcing a follow-up turn past `goal_achieved`) would constitute a retry. The honest finding is "the existing smoke harness cannot produce a CONFIRM → CLOSE turn pair on a satisfied-user case; surface the follow-on R-item and try again in a future sprint."

### 6.3 Proposed follow-on R-item

**Proposed R-item name:** `R-per-turn-phase-transition-dump-for-smoke-harness` (the exact name Sprint 28 §7 open question 1 named).

**Proposed scope** (refined by Sprint 29's Track B finding):

1. **Primary scope (per Sprint 28 §7 + dev-prompt §6.1).** Extend `per_turn_trace[i]` in `eval_interactive/eval_interactive/batch/executor.py` `_build_per_turn_trace` with three additional fields:
   - `phase_before` (the phase the bot's loop **entered** on, per the projection built at the start of bot turn N).
   - `phase_after` (the phase the bot's loop **left** on, per the projection / runtime state at the end of bot turn N).
   - `turn_index` (the canonical bot-turn index; redundant with array index, but explicit for downstream joins per Sprint 28 §7 open question 2).
2. **Sister-scope observation (NOT in the primary R-item; deliberately named-but-not-opened here per Sprint 18 §8.7 precedent).** Even with `phase_before` / `phase_after` per bot turn, the D485.2 CLOSE-entry detection on a satisfied-user case requires the bot to actually be **given** a turn after the user's satisfaction signal so the CONFIRM phase plan can run. The user simulator's `goal_achieved` early-stop at `session_runner.py:224` pre-empts this. A future scope decision (Sprint 30 or beyond) should weigh whether the smoke harness's stop-condition policy needs adjusting to keep sessions running past user satisfaction long enough to observe CONFIRM / CLOSE. **Sprint 29 does NOT open this as an R-item** (n=1 evidence per §8.7), but records it as an open observation so the next planning turn has the context.

**Layer:** `infra` (per §3.2 Q1 — eval-harness scope; Option-B-style additive enrichment, mirroring Sprint 25 / Sprint 28's pattern). No `server/` code edit; no prompt edit; no Tier-0 candidate.

**Why proposed (not opened):** the (R2) follow-on probe sprint is evidence-gathering per Sprint 27 §6; opening an R-item this sprint would conflate evidence-gathering with planning. The named-but-not-opened pattern (Sprint 18 §8.7, Sprint 28 §7) is the established route; the next planning turn (human + deliver) decides whether to promote.

**Sister observation about `phase_plan` snapshot semantics** (§6.1 Q4). Whichever interpretation is correct ((a) end-of-turn snapshot or (b) start-of-turn projection that doesn't account for intra-loop phase transitions), the `R-per-turn-phase-transition-dump-for-smoke-harness` scope above answers it cleanly: a `phase_before` / `phase_after` pair removes the ambiguity.

## 7. R-item disposition update — `R-prompt-phase-plan-directive-followship` (line 450)

`docs/action_bank.md` line 450 entry: edited to append Sprint 29 evidence under the existing disposition. R-item **NOT closed**.

Added evidence (per the cascade rule for disposition refinement, not closure):

- Sprint 29 (2026-05-15) authored 4 hand-authored CaseSpecs at `eval_interactive/case_specs/case_families/sprint29_directive_probe/` and ran them through smoke at `eval_interactive/results/20260514-225419/results.json`.
- **D564.7 (INTAKE intake-complete handover):** 1/1 directive followed (cs29d564_uc_h_ad_removal_appeal_complete_intake). Bot escalated with canonical `intake_complete_for_uc_h` once all three UC-H required intake fields per `IntakeFieldsRegistry.java:59` were collected. n=1; not sufficient to close the R-item or to call the directive systematically followed.
- **D616.2 (RESOLVE FAQ premature-escalation prohibition):** 1/1 directive violated (cs29d616_uc_c_message_delivery_mild_frustration). Bot reached a viable resolve_article hit (`ka44J000000TetlQAC` "I Can't Send Replies") but escalated with bot-requested `user_distress` reason instead of producing the grounded user-facing answer. n=1; per §8.7 open-observation rule, **insufficient to broaden to a systematic D616.2 violation pattern** (which would otherwise be the line-450 R-item's structural confirmation surface).
- **D485.2 (CLOSE record_outcome-if-not-already-recorded):** undecidable on Track A's corpus. Both D485.2 cases (cs29d485_uc_b, cs29d485_uc_c) failed to reach CLOSE — cs29d485_uc_b stopped on `goal_achieved` before CONFIRM; cs29d485_uc_c degraded to `service_degraded` handover after a `record_outcome` error. The structural conclusion is the same as Sprint 28 §5 noted: the current smoke harness cannot observe CLOSE-entry on the load-bearing case.

**Disposition update text (added to line 450 after Sprint 28's close-paragraph):** "Sprint 29 (R2) probe ran 4 hand-authored cases through smoke. D564.7 followed (1/1); D616.2 violated (1/1) — neither n=1 observation broadens to systematic per the §8.7 rule. D485.2 undecidable: the user-simulator `goal_achieved` early-stop pre-empts the bot's CONFIRM phase plan before CLOSE-entry can be observed. Track B (c) phase-derivation verification fails on Q2 against Track A's authored corpus per `docs/sprints/sprint-029-handoff.md` §6.2; downgrade fires; proposed follow-on `R-per-turn-phase-transition-dump-for-smoke-harness` named but NOT opened per Sprint 28 §7 / Sprint 18 §8.7 convention. R-item stays open; structural-sprint trigger ((R1) ≥ 2 confirmed directive non-fulfillments) remains unmet."

## 8. Anti-hardcode self-walk (per `iteration_governance.md` §4.1; nine questions)

Sprint 29 is semantic-touching (`eval_spec`-layer authoring). The §4.1 review prompt applies; the dev runs the walk pre-emptively for traceability.

1. **Adds keyword / regex / if-else / enum / per-UC matrix for a semantic decision?** No. Sprint 29's only code-adjacent change is four CaseSpec YAMLs + one markdown manifest. No `.java` / `.py` / `.properties` / prompt edits. CaseSpec content is `expected.*` structured fields only; no regex, no keyword precondition.
2. **Justified as protecting a current Tier-0 invariant?** N/A. No Tier-0 invariant added by Sprint 29.
3. **Soft-signal alternative considered?** Yes; Sprint 29 explicitly defers structural remediation. The §6.3 proposed follow-on R-item is a soft-signal-via-extracted-data shape (extend `per_turn_trace[i]` with `phase_before` / `phase_after`), not a Java guard.
4. **Encodes visible-eval case text / trace-specific phrasing / CaseSpec id into runtime / prompt / judge?** No. No code, prompt, or judge edits. The CaseSpec `hidden_facts` describe user-side situation only (UC-H removal email lacked specific reason; UC-C six months prior success); no bot transcript text is encoded.
5. **Moves semantic ownership from LLM to Java?** No. Sprint 29 ships zero runtime code. The probe surfaces evidence about which directives the LLM follows; remediation, if pursued in a future sprint, is the §3.2 layer-routing question, not Sprint 29's.
6. **Adds if-else block to the prompt?** No. No prompt edit (Sprint 29 fence hard #3).
7. **Preserves tool schema, capability / permission boundary, PII / safety floor, grounding floor?** Yes. Sprint 29 touches none of these surfaces.
8. **Ships generalization eval coverage (target / neighbor / negative / shadow)?** N/A for a (R2) probe sprint per `docs/sprint_objective.md` §11 — target cases are the deliverable; neighbor / negative / shadow are explicitly NOT required for a probe sprint. The §6.2 design rationale per directive substitutes for the G2 generalization pattern.
9. **Temporary measure with sunset / rollback?** N/A. Track A's CaseSpecs are durable evidence (they live alongside Sprint 20 G2 case families). Track B's verification produced no code change; nothing to sunset.

Per §4.1 verdict set for **per-PR review** (distinct from the sprint-close header): the dev's expected verdict for Codex (if dispatched) is **`approve`** — Sprint 29 is `eval_spec`-layer authoring per the §11 stanza in `docs/sprint_objective.md`; no semantic hardcode is introduced; the bot-failure observations on D616.2 are recorded as findings, not converted into rubric changes.

## 9. Files changed

### 9.1 Track A — eval_interactive (5 new files, 0 edited)

| path | change type | description |
|------|-------------|-------------|
| `eval_interactive/case_specs/case_families/sprint29_directive_probe/_manifest.md` | NEW | Sprint-29-local manifest in markdown (skips loader's `*.yaml` glob); documents layout choice + per-case shape + §6.2 authoring-discipline self-check. |
| `eval_interactive/case_specs/case_families/sprint29_directive_probe/cs29d485_uc_b_delete_ad_resolution.yaml` | NEW | D485.2 case 1; UC-B "How to Delete an Ad" resolve path. |
| `eval_interactive/case_specs/case_families/sprint29_directive_probe/cs29d485_uc_c_email_notifications_resolution.yaml` | NEW | D485.2 case 2; UC-C "Can I Turn Off Email Messages?" resolve path. |
| `eval_interactive/case_specs/case_families/sprint29_directive_probe/cs29d564_uc_h_ad_removal_appeal_complete_intake.yaml` | NEW | D564.7 case; UC-H ad-removal appeal; canonical 3-field intake. |
| `eval_interactive/case_specs/case_families/sprint29_directive_probe/cs29d616_uc_c_message_delivery_mild_frustration.yaml` | NEW | D616.2 case; UC-C messaging-delivery with mild frustration; viable hit reachable. |
| `eval_interactive/results/20260514-225419/results.json` | NEW (smoke output) | 142.2 KB; 4 case-results; Sprint 28 `per_turn_trace[]` schema present; primary evidence file for §5–§7. |
| `eval_interactive/results/20260514-225419/report.html` | NEW (smoke output) | 29.6 KB; auto-generated by the harness; supporting artefact only. |

No edits to Sprint 20 case families. No edits to Sprint 23/24/25/26/27/28-landed Python / Java code. No prompt edit. No judge / override / persona / case-family-shadow edit. No `_KNOWN_SETS` change.

### 9.2 Sprint-close artefacts

| path | change type | description |
|------|-------------|-------------|
| `docs/sprints/sprint-029-handoff.md` | NEW (this file) | 12-section dev handoff per `docs/sprint_objective.md` §14. |
| `docs/action_bank.md` line 450 entry | EDIT (append-only) | Disposition refined with Sprint 29 evidence; R-item NOT closed. |

### 9.3 Pre-existing untouched working-tree mods (NOT staged by the dev; carried forward)

Per dev-prompt §11:

- ` M csagent_system_design_review.md` (pre-existing; Sprint 24+).
- ` M server/src/main/resources/prompts/system_prompt.txt` (pre-existing; source of inherited `SystemPromptUserRequestedTiebreakerTest` failure; Sprint 24+).
- `?? csagent-solution-_20260514.md` (pre-existing untracked).
- Deliver-agent-owned: `docs/sprint_objective.md` (Sprint 29 scope), `compact/sprint-029-dev-prompt.md`, `compact/sprint-029-review-prompt.md`, `compact/sprint-deliver-orchestrator.md`. **Not** staged by the dev.

## 10. Layer-classification self-walk (per `iteration_governance.md` §3; first-match-wins; per-track)

### 10.1 Track A — `eval_spec` (per `docs/sprint_objective.md` §11)

Walking §3.2 Q1–Q7:

- Q1 (infra crash / timeout)? No.
- Q2 (Tier-0 invariant broken)? No; Sprint 29 adds none.
- Q3 (LLM choice valid but projection impoverished)? No — Sprint 29 does not change the projection. Track A's CaseSpecs are eval-shape, not projection-shape.
- Q4 (multi-tool flow losing state)? No.
- Q5 (LLM semantically wrong with correct projection)? Partially — D616.2 case observed bot escalating with bot-requested `user_distress` instead of producing the grounded answer. But Sprint 29 does NOT remediate that observation; it records it. The first matching layer for the **observed bot mistake** is `semantic_planner` per §3.2 Q5. The first matching layer for **Sprint 29's diff** is Q6 below.
- **Q6 (eval CaseSpec / judge / rubric)?** **MATCH for the diff.** Sprint 29 authors CaseSpecs that exercise behaviour the runtime can and should perform. Sprint 20 G2 precedent (§3 of `docs/sprints/sprint-020-handoff.md`) explicitly classifies hand-authored case-family CaseSpecs as `eval_spec` per §3.2 Q6 even when no widening occurs. Sprint 29's authoring is the (R2) probe form of the same pattern.
- Q7 (product policy)? No.
- Tail rule (judge stability flip on rerun)? Not observed; Track A is one smoke run.

**Track A layer:** `eval_spec`.

### 10.2 Track B — `infra` (verification-only) → fallback `human_review_required`

Walking §3.2 Q1–Q7:

- **Q1 (infra crash / timeout / eval-harness behavior)?** **MATCH for Track B's diff** (zero diff; verification-only on the existing eval-harness output, Sprint 28 enrichment). Track B is post-hoc analysis on `results.json`, the canonical `infra` / eval-harness surface per §3.1 layer set.
- Q2–Q7: N/A; Track B has no code or rubric diff.
- Tail rule: not exercised.

**Track B layer (verification-only):** `infra`. **Track B fallback layer (per `docs/sprint_objective.md` §11):** `human_review_required` — fired (the §7.2 in-flight downgrade clause is precisely the §3.2 default-tail exit, surfacing the failure to the human for a future-sprint decision instead of inventing a remediation).

## 11. Open questions for the human

1. **D485.2 CLOSE-entry observability is structurally blocked by the user simulator's `goal_achieved` early-stop.** Track B Q2 fails on Track A's authored corpus not because the cases were poorly designed but because the smoke harness's stop policy at `eval_interactive/eval_interactive/simulator/session_runner.py:224` terminates the session before the bot is given a turn to interpret the user's satisfaction signal. The §6.3 proposed R-item names `phase_before` / `phase_after` per-turn dump as the primary scope; should a follow-on scope **also** consider keeping satisfied-user sessions open long enough for the bot's CONFIRM phase to run? (Sister-scope observation; named-but-not-opened per Sprint 18 §8.7 convention.)
2. **D616.2 single-case violation observation.** cs29d616_uc_c's behaviour (viable hit reached, then bot escalated with `user_distress` instead of producing the grounded answer) is exactly the D616.2 violation shape the line-450 R-item names. n=1 evidence is below the §8.7 broadening threshold, so it remains a single-case observation. Should the next sprint author 1–2 additional UC-C / UC-B / UC-D D616.2 cases at mild-frustration shape to confirm the pattern, or wait for production-trace evidence?
3. **`phase_plan` snapshot semantics in `per_turn_trace[i]`.** §6.1 Q4 names two interpretations of why UC-H intake-complete (cs29d564_uc_h turn 1) records `phase_plan.phase == "RESOLVE"` rather than `INTAKE`. The proposed `phase_before` / `phase_after` R-item resolves the ambiguity downstream; should it ALSO clarify which point-in-loop `phase_plan` represents in the existing field (`turn.projected_context["phase_plan"]` at `executor.py:323`)?
4. **D485.2 case design for a future probe.** Now that the simulator early-stop is identified as the structural blocker, a future D485.2 case could try to drive past `goal_achieved` (e.g. a 3-message user persona that delays satisfaction signal to turn 4 + asks a follow-up). This is a probe-sprint design question, not a Sprint 29 scope question.
5. **`record_outcome` `error: [REDACTED_TOKEN]` shape on cs29d485_uc_c.** The error is from the bot side and triggered the `service_degraded` escalation. The `[REDACTED_TOKEN]` is from trace redaction; the underlying error message is not visible in `results.json`. Whether this is an inherited infra bug (e.g. token-budget exhaustion at `record_outcome` time) or a Sprint 29-specific shape needs the human + a server-side trace inspection to decide; flagged for visibility, not opened as an R-item.

## 12. Closure verdict (filled at sprint close, 2026-05-16)

| field | value |
|-------|-------|
| status | closed |
| classification | pass |
| Codex sprint-close review | decision: pass; blocking_count: 0; summary: Codex substantive anti-hardcode verdict approved; two conditional out-of-scope blockers (`server/src/main/resources/prompts/system_prompt.txt`, `csagent_system_design_review.md` — both labelled pre-existing in §9.3) resolved by disciplined staging (excluded from the Sprint 29 commit); reproducibility blocker fixed by adding colocated source-path + extraction-command citations at §3 item 8 (3 964 lines / 219 parsed rows), §5.1 (51 774 ms / 142.2 KB), §5.1 follow-on (mean_judge 0.8000 / escalation_correct 25.0% / mean_outcome 0.8007), and §5.2 post-table reproducibility block (cs29d564_uc_h `classify_use_case` confidence 0.90). |
