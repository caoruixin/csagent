Paste the content below this line into a fresh Claude Code session on branch `refactor/remove-the-shackles`.

---

You are the dev agent for Sprint 32 — the case-family-authoring sprint that validates the Sprint 31 `alternate_candidate_use_cases` projection slot end-to-end. Sprint 31 OQ4 deferred this work; the deliver-agent + human picked it as the next current sprint at the 2026-05-16 Sprint 31 close planning round.

This sprint is `eval_spec`-only. You author CaseSpecs; you do NOT touch any production code, prompt, or Java test. The Sprint 20 / Sprint 29 cascade fence still applies: you do NOT edit existing case families.

## 1. Loader

1. `AGENTS.md` (transitively loads `iteration_governance.md` §1 / §3 / §5 / §7 + `doc_governance.md` + `agent_context_guide.md`).
2. `docs/sprint_objective.md` — your contract. §6 is the file table you ship; §7 is the hard-fence list; §9 is the §7 stanza you must fill; §10 is the success-metric checklist; §12 is the stop-condition list.
3. `docs/sprints/sprint-031-handoff.md` §5 (smoke-rerun extraction recipe and the 4 AMBIGUOUS-intake observed cases) + §14 (chained close summary) — context on what Sprint 31 actually shipped and how the new slot manifests in the smoke trace.
4. `docs/sprints/sprint-031-objective.md` §6 + §9 — file table that shipped + the §7 stanza Sprint 31 filled (`prompt_projection` layer). Sprint 32's §7 stanza is `eval_spec` layer instead.
5. `docs/current/iteration_governance.md` §2 (Failure Brief Template — your source brief uses this), §3 (Fix Layer Classification — you self-walk to `eval_spec` in handoff §10), §5 (Eval Acceptance Rules — you self-walk in handoff §11), §7.2 (the worked example — your target case promotes it).
6. `eval_interactive/case_specs/case_families/_manifest.yaml` lines 23–168 area — the Sprint 20 G2 family-entry schema. You append entry #10 at the end using this exact schema.
7. `eval_interactive/case_specs/case_families/sprint29_directive_probe/` (and especially `_manifest.md` inside it) — flat-layout precedent + local manifest convention.
8. `eval_interactive/case_specs/case_families/cs015_uc_fp_mis_route/` — an exemplar family directory (you do NOT edit anything inside, but read at least one neighbor + one negative CaseSpec to anchor schema and tone).
9. `eval_interactive/case_specs_shadow/_ACCESS_BOUNDARY.md` — the dev-agent shadow access rule. You WRITE the shadow files but you do NOT execute them.
10. `eval_interactive/case_specs_shadow/_manifest.yaml` — the shadow manifest you append the real shadow ids to (the visible root manifest gets the `[REDACTED ...]` placeholder).
11. `eval_interactive/results/20260516-024934/results.json` — Sprint 31 reference smoke. You extract the four AMBIGUOUS-intake cases' `per_turn_trace[].projection.alternate_candidate_use_cases` arrays to ground your neighbor-UC-pair selection (see §3.2 below).
12. One existing failure-brief at `docs/diagnostics/failure-briefs/cs015-uc-fp-mis-route-and-premature-escalate.md` (or any other brief in that dir) for tone / six-field-template precedent.

## 2. Premise re-verification

Spot-check at session start (`HEAD` should be `8d3e73b` or later if other agents have committed since 2026-05-16):

1. `eval_interactive/case_specs/case_families/sprint29_directive_probe/` exists with flat layout (4 `*.yaml` + 1 `_manifest.md`). Confirm flat layout by `ls`.
2. `eval_interactive/case_specs/case_families/_manifest.yaml` carries 9 family entries (Sprint 20 G2). Confirm by `grep -c "^  - family_id:" eval_interactive/case_specs/case_families/_manifest.yaml`. Expected: 9.
3. `eval_interactive/case_specs_shadow/_ACCESS_BOUNDARY.md` exists and asserts the dev-agent-may-not-read-shadow rule. Confirm by reading it.
4. `eval_interactive/eval_interactive/case_spec/loader.py:144` globs `*.yaml`. Confirm by reading that line.
5. Sprint 31 reference smoke artifact `eval_interactive/results/20260516-024934/results.json` exists; extract the four AMBIGUOUS-intake cases' alternates lists:

   ```bash
   jq -r '.case_results[] | select(.case_id | IN("cs_interactive_015","cs_interactive_040","cs_interactive_176","cs_interactive_192")) | "\(.case_id): \(.per_turn_trace[0].projection.alternate_candidate_use_cases // "absent")"' \
     eval_interactive/results/20260516-024934/results.json
   ```

   Expected output (per Sprint 31 handoff §5.4):
   - `cs_interactive_015`: `["UC-A","UC-FP","UC-H"]`
   - `cs_interactive_040`: 12-element fallback list
   - `cs_interactive_176`: `["UC-E","UC-K"]`
   - `cs_interactive_192`: `["UC-A","UC-B","UC-FP"]`

   If any premise drifts, STOP and surface in handoff §3.

## 3. The work — author the case family

Single track. ~9 new files plus 2 manifest appends. NO production code, NO prompt edits, NO Java test edits.

### 3.1 Source brief (REQUIRED)

Path: `docs/diagnostics/failure-briefs/sprint32-uc-a-uc-c-alternate-uc-readiness.md`.

Promote the `docs/current/iteration_governance.md` §7.2 hypothetical brief into a real six-field brief per `iteration_governance.md` §2 Failure Brief Template:

- **What happened?** — UC-A user (listing visibility / Top Ad question) drifts to UC-C (Replies / Messaging) on turn 2 or 3; without the Sprint 31 slot the bot stamps `active_use_case=UC-A` through the drift and answers the messaging complaint as a UC-A follow-up. **With** the Sprint 31 slot, the LLM should observe `alternate_candidate_use_cases` containing `UC-C` and either reroute or ask a focused clarifying question.
- **What should a good CS agent have done?** — recognize the topic shift, route to the alternate UC (or ask one focused clarifying question on ambiguous reroute), continue the conversation as a UC-C issue with the projection now carrying both the original and the new UC's task state.
- **Why does this matter?** — drift / topic-shift is LLM-owned per Constitution §1.3; failure to reroute corrupts intake, fires off-topic answers, and is the canonical eval-surface failure the Sprint 31 projection slot was designed to address.
- **Is this a one-off or a pattern?** — pattern. UC-A↔UC-C soft-shift is documented at `iteration_governance.md` §7.2; the broader shape covers any UC pair sharing an intake topic-subject family (e.g., UC-A↔UC-D, UC-A↔UC-FP).
- **Which layer is likely responsible?** — `eval_spec` per §3.2 Q6 (runtime + projection are right post-Sprint-31; the eval surface lacks the case-family corpus to observe whether the LLM acts on the new soft signal).
- **What should NOT be done?** — adding a regex on "replies" / "messages" / "buyer" to `DriftDetector` to force reroute. The Sprint 31 fix-iteration #2 strengthened T8 across 6 parameterised variants × 5 invariance bars to lock down runtime non-enforcement; the LLM owns the read decision.

### 3.2 Neighbor UC-pair selection

Ground the neighbor selection in the observed §2 (5) data. The four Sprint 31 AMBIGUOUS-intake cases' alternates contain these UC pairs:

- `cs_interactive_015` alternates → UC-A, UC-FP, UC-H
- `cs_interactive_192` alternates → UC-A, UC-B, UC-FP
- `cs_interactive_176` alternates → UC-E, UC-K

Pick TWO neighbor UC pairs that:

- Share the AMBIGUOUS-intake + topic-shift shape with the UC-A↔UC-C target.
- Have observably-emitted alternates lists in the Sprint 31 smoke (do NOT invent UC pairs the intake router would not actually emit).
- Differ from the target on at least one axis (different active UC OR different alternate UC).

Suggested defaults (you may pick differently; document rationale in handoff §4):

- **Neighbor 1:** UC-A ↔ UC-D (listing visibility vs account access). Plausible AMBIGUOUS shape on the "Account & Ad" topic family.
- **Neighbor 2:** UC-A ↔ UC-FP (listing visibility vs ad-support deletion appeal). Observed shape (`cs_interactive_015` carries UC-A + UC-FP in alternates).

Document neighbor rationale + the observed-trace evidence each choice rests on in handoff §4.

### 3.3 Negative case design

Two negative cases. Each MUST satisfy: the `alternate_candidate_use_cases` slot is populated (so the LLM observes the signal), AND the LLM should NOT reroute.

Suggested shapes (you may pick differently; document rationale):

- **Negative 1:** UC-A user with the alternates slot populated (e.g., `["UC-A","UC-C"]` per the §7.2 worked-example AMBIGUOUS-shape intake) who follows up with a deeper UC-A question on turn 2 ("ok, but what about the Top Ad price — can I downgrade?"). Bot should stay in UC-A; the slot is observable evidence but not enforcement.
- **Negative 2:** UC-A user who explicitly confirms staying on the visibility question on turn 2 ("yes I just want to know about the visibility issue, that's all"). Bot should continue UC-A handling.

The negative cases are the load-bearing CaseSpecs for the §1.7 non-enforcement posture — they prove the LLM can read the soft signal and still ignore it when the user clearly does not drift. If a future Java branch were added that gated on the slot's contents, the negative cases would fail (the runtime would force-reroute despite the user's clear UC-A continuity). The Sprint 31 fix-iteration #2 strengthened T8 covers the runtime-side proof; the negative CaseSpecs cover the LLM-behaviour proof.

### 3.4 Shadow case design

Two shadow cases. WRITE the files at `eval_interactive/case_specs_shadow/sprint32_alternate_uc/`. DO NOT execute them through your smoke run.

Recommendation: pick two UC pairs the dev does NOT use for the target / neighbor / negative split (e.g., UC-A↔UC-B, UC-C↔UC-D). Or pick the same pair as the target with a different surface (different topic_subject, different seed messages, different drift mechanism — e.g., user surfaces the drift via a hidden_fact reveal instead of an explicit on-message shift). The point of shadow CaseSpecs is to be held out from the dev so a future regression sprint that fixes against the visible cases can be independently checked against the shadows by the human + review agent.

### 3.5 CaseSpec authoring rules

For each new CaseSpec:

- `case_id` matches the filename (no `.yaml` suffix). Naming: `cs32t01_uc_a_uc_c_drift` (target), `cs32n01_<neighbor1>`, `cs32n02_<neighbor2>`, `cs32g01_<negative1>`, `cs32g02_<negative2>` (visible) + `cs32s01_<shadow1>`, `cs32s02_<shadow2>` (shadow).
- `source_session_id`: `synthetic-sprint32-<class>-<idx>` (mirror Sprint 29 pattern `synthetic-sprint29-<directive>-<idx>` and Sprint 20 pattern `synthetic-sprint20-<family>-<class>-<idx>`).
- `source_dataset: case_family_authored`.
- `form_context`: realistic — `first_name`, `email: customer@example.com`, `topic_subject` (matches the intake AMBIGUOUS family — e.g., "Replies & Messaging" for UC-A↔UC-C drift on the §7.2 shape), `ad_id` (populated for UC-A target / negative; empty or blank where appropriate), `description` (one or two sentences from the user's perspective).
- `persona.user_goal_summary`: one paragraph from the user's perspective; names the underlying goal (e.g., "I want to fix the visibility issue and also figure out why my replies aren't going through"); does NOT encode bot behavior or trace text.
- `persona.frustration_level`: `mild` / `normal` / `high` — pick per case shape.
- `persona.verbosity`: `normal` / `verbose` — pick per case shape.
- `persona.drift_behavior`: `none` (negatives) / `seeded` (target / neighbors) / similar. Document choice.
- `persona.seed_messages`: 1–3 seed messages from the user. The drift target / neighbor cases SHALL include the drift message verbatim as one of the seeds (typically turn 2 or 3); the negative cases SHALL include a follow-up that explicitly stays in the active UC.
- `persona.hidden_facts`: optional. Names user-side observable state with `disclose_when` cues. Do NOT encode bot trace text.
- `persona.will_request_human_if`: one-line trigger; do NOT use trace-specific phrasing.
- `expected.outcome_class`: `resolve` / `escalate` / `intake` per the per-case shape.
- `expected.primary_uc`: the UC the case ENDS in (after drift, if any).
- `expected.secondary_ucs`: list of UCs the case touches; for target / neighbor drift cases this list SHALL include the original UC AND the post-drift UC.
- `expected.should_escalate`: per case shape.
- `expected.allow_bot_resolution`: `'true'` / `'false'` per case shape.
- `expected.bot_handling_pattern`: one-paragraph description of expected behaviour from the bot perspective. Names which UC the bot is in at the end, what tools it ran, whether it acknowledged the drift, whether it asked a clarifying question.
- `expected.escalation_trigger`: null for resolve cases; one of the 23 enum values per `PhaseEvaluator.java:39–63` for escalate cases.
- `expected.risk_level`: `low` / `medium` / `high` per case shape.
- `expected.expected_tool_sequence`: list of tool calls in expected order. For UC-A target the sequence likely begins with `classify_use_case` then post-drift transitions to UC-C handling.
- `expected.forbidden_tools`: list of tools the bot SHALL NOT call on this case shape.
- `expected.grounding_mode`: `faq_source_backed` / `intake_state_backed` / `not_applicable` per case shape.
- `expected.answer_must_not_contain`: list of trace-specific phrases the bot SHALL NOT produce. Keep generic ("I've fixed", "I have sent you an email") — NOT case-specific.
- `expected.max_turns`: per case shape; typical 8–12.
- `scoring.hard_checks`: the standard 8-check list (mirror Sprint 29 case).
- `scoring.outcome_checks`: the standard 7-check list (mirror Sprint 29 case).
- `scoring.llm_judge_dimensions`: the standard 3-dimension list (mirror Sprint 29 case).

Read at least one Sprint 29 CaseSpec verbatim before authoring to anchor schema; do not invent fields.

### 3.6 Local manifest

Path: `eval_interactive/case_specs/case_families/sprint32_alternate_uc/_manifest.md`.

Mirror `eval_interactive/case_specs/case_families/sprint29_directive_probe/_manifest.md` shape:

- Title.
- Two-paragraph context (why this family is local + flat layout per loader contract).
- Cases table (case_id / class / primary_uc / expected_outcome_class / shape).
- Authoring discipline self-check (the §6.2-shape section).
- Load + run command.

### 3.7 Root manifest append

Path: `eval_interactive/case_specs/case_families/_manifest.yaml`.

Append entry #10 at end-of-file using the schema at lines 23–38 (`cs015_uc_fp_mis_route` exemplar):

```yaml

  - family_id: sprint32_alternate_uc
    source_brief: docs/diagnostics/failure-briefs/sprint32-uc-a-uc-c-alternate-uc-readiness.md
    primary_layer: eval_spec
    secondary_layer: prompt_projection
    l3_override_status: NONE (Sprint 32 hand-authored family; no L3 triage)
    target_case_ids: [cs32t01_uc_a_uc_c_drift]
    target_case_path: eval_interactive/case_specs/case_families/sprint32_alternate_uc/cs32t01_uc_a_uc_c_drift.yaml
    neighbor_case_ids: [cs32n01_<neighbor1>, cs32n02_<neighbor2>]
    negative_case_ids: [cs32g01_<negative1>, cs32g02_<negative2>]
    shadow_case_ids: "[REDACTED -- see case_specs_shadow/_manifest.yaml]"
    notes: >-
      Validates the Sprint 31 alternate_candidate_use_cases projection
      slot end-to-end on the §7.2 worked-example shape. Target is the
      UC-A↔UC-C topic-shift case; neighbors are other UC pairs sharing
      the AMBIGUOUS-intake + topic-shift shape. Negatives prove the
      LLM can observe the populated slot and still stay in the active
      UC when the user does not drift (load-bearing for §1.7
      non-enforcement at the eval surface; Sprint 31 fix-iteration #2
      strengthened T8 covers the runtime-side proof). Authored
      2026-05-16.
```

Replace the `<neighbor1>` / `<neighbor2>` / `<negative1>` / `<negative2>` placeholders with the actual ids you picked.

### 3.8 Shadow manifest append

Path: `eval_interactive/case_specs_shadow/_manifest.yaml`.

Append the real shadow ids using the same entry schema as the visible root manifest, but with the actual ids in place of `[REDACTED ...]`. Do NOT execute the shadow CaseSpecs through your dev smoke run.

## 4. Verification

### 4.1 Java tests

Sprint 32 makes NO Java change. Re-run the full server suite to confirm byte-identical baseline:

```bash
mvn -q -pl server test
```

Expected: `Tests run: 917, Failures: 1, Errors: 0, Skipped: 2` — identical to the Sprint 31 fix-iteration #2 close baseline (the same inherited `SystemPromptUserRequestedTiebreakerTest` failure carried since Sprint 24). If any delta, STOP and surface in handoff §4.

### 4.2 14-case smoke regression check

```bash
cd eval_interactive && uv run eval-interactive run --path case_specs/smoke
```

Compare against `eval_interactive/results/20260516-024934/results.json` (Sprint 31 reference). Recipe:

```bash
jq -r '"passed=\(.summary.passed_cases) mean_comp=\(.summary.mean_composite_score) mean_out=\(.summary.mean_outcome_score) mean_judge=\(.summary.mean_judge_score)"' \
  eval_interactive/results/<your-run-id>/results.json
```

No-regression bar: `composite_score` / `task_success_rate` / safety floor / grounding floor / wrong-containment / over-escalation per `iteration_governance.md` §5.1. Sprint 32 makes no production change so the regression risk is LOW; this is a defensive check. Note the Sprint 31 §13 external-drift caveat — if you see another +84% mean elapsed_ms widening, surface as observation, do NOT block on it (carried by `R-llm-provider-latency-drift-2026-05-16`).

### 4.3 Sprint 32 family rerun (primary signal)

```bash
cd eval_interactive && uv run eval-interactive run \
  --path case_specs/case_families/sprint32_alternate_uc/
```

This runs target + neighbor + negative (the loader globs `*.yaml`; the `_manifest.md` is skipped). Do NOT include `case_specs_shadow/sprint32_alternate_uc/` in any path argument; that violates the access boundary.

Per-case extraction recipe (handoff §5 inputs):

```bash
jq -r '.case_results[] | "\(.case_id): outcome=\(.terminal_outcome) score=\(.composite_score) alts=\(.per_turn_trace[0].projection.alternate_candidate_use_cases // "absent")"' \
  eval_interactive/results/<your-run-id>/results.json
```

Document per-case outcomes in handoff §5. Do NOT make LLM-behaviour claims beyond observed traces. If the target case fires the expected reroute and the negative cases stay in the active UC, that is informational evidence for the §7.2 worked example; you SHALL NOT generalize this to "the slot works" without further investigation.

### 4.4 §10 success-metric self-walk

Walk each of the seven bullets in `docs/sprint_objective.md` §10 against your actual delivery. Surface gaps in handoff §11 (§5 Eval Acceptance bars).

## 5. Stop conditions

See `docs/sprint_objective.md` §12 — ten conditions. Specifically watch for:

- Premise drift on the §4 / §2 items.
- Java test regression (none expected; large delta is a stop).
- 14-case smoke regression beyond the §1.7 noise band (composite drop > 10% triggers a stop and a handoff §7 open question).
- Loader / executor / simulator surface concerns — surface as handoff §7, do NOT fix in this sprint.
- Temptation to consume shadow CaseSpecs during dev — STOP.
- Single-issue simulator early-stop on `goal_achieved` pre-empting the seeded drift — surface as handoff §7 (the Sprint 29 §11 pattern); do NOT re-engineer the CaseSpec to bypass it.

## 6. Files you stage and commit

Stage:

- `docs/diagnostics/failure-briefs/sprint32-uc-a-uc-c-alternate-uc-readiness.md` (new)
- `eval_interactive/case_specs/case_families/sprint32_alternate_uc/_manifest.md` (new)
- `eval_interactive/case_specs/case_families/sprint32_alternate_uc/cs32t01_*.yaml` (new target)
- `eval_interactive/case_specs/case_families/sprint32_alternate_uc/cs32n01_*.yaml` (new neighbor #1)
- `eval_interactive/case_specs/case_families/sprint32_alternate_uc/cs32n02_*.yaml` (new neighbor #2)
- `eval_interactive/case_specs/case_families/sprint32_alternate_uc/cs32g01_*.yaml` (new negative #1)
- `eval_interactive/case_specs/case_families/sprint32_alternate_uc/cs32g02_*.yaml` (new negative #2)
- `eval_interactive/case_specs_shadow/sprint32_alternate_uc/cs32s01_*.yaml` (new shadow #1)
- `eval_interactive/case_specs_shadow/sprint32_alternate_uc/cs32s02_*.yaml` (new shadow #2)
- `eval_interactive/case_specs/case_families/_manifest.yaml` (root manifest append, single 14-line block at end-of-file)
- `eval_interactive/case_specs_shadow/_manifest.yaml` (shadow manifest append)
- `docs/sprints/sprint-032-handoff.md` (new; 12-section archive per §11)

Do NOT stage:

- `docs/sprint_objective.md` (deliver-agent-owned; deliver-agent archives at sprint close)
- `docs/10-handoff.md` (deliver-agent-owned)
- `docs/action_bank.md` (deliver-agent-owned)
- `docs/codex-findings.md` (Codex-owned)
- `compact/sprint-032-*-prompt.md` (deliver-agent-owned)
- Any working-tree mock-data files that pre-existed at session start (per deliver-agent context — they are unrelated to Sprint 32)

Commit message shape (mirror Sprint 31 / Sprint 29):

```
sprint 32: alternate_candidate_use_cases case family (Sprint 31 OQ4 deferral)

Authors a 7+ CaseSpec family (target / neighbor / negative / shadow split
per Sprint 20 G2 pattern) validating the Sprint 31 projection slot on the
§7.2 UC-A↔UC-C worked-example shape. Source brief promotes the §7.2
hypothetical into a real failure-brief; family entry appended to root +
shadow manifests; flat layout per Sprint 29 loader-compat precedent.

ZERO server/src/main/** change, ZERO prompt change, ZERO Java test change.
Sprint 31 cascade fence honored (existing case families untouched).

[summary of test run / smoke run / family run with cited numbers]

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
```

## 7. Handoff document

Write `docs/sprints/sprint-032-handoff.md` per the 12-section contract in `docs/sprint_objective.md` §11. Key sections:

- **§3 Premise re-verification:** the five §4 premises spot-checked.
- **§4 Implementation walkthrough:** each CaseSpec by file, the neighbor / negative selection rationale grounded in observed-trace evidence, the shadow choice rationale.
- **§5 Smoke rerun:** 14-case regression check result + the Sprint 32 family run result + per-case `alternate_candidate_use_cases` extraction.
- **§6 Generalization coverage table:** target=1, neighbor=N, negative=N, shadow=N counts.
- **§7 Open questions for human:** any item you surface but don't act on.
- **§8 Anti-hardcode self-walk:** the §4.1 nine-question kernel.
- **§9 Files changed:** table per §6 above.
- **§10 Layer-classification self-walk:** lands on `eval_spec`.
- **§11 §5 Eval Acceptance bars:** all 9 bars with cited evidence.
- **§12 Closure verdict placeholder:** leave for deliver-agent + human.

## 8. Final check before commit

- [ ] Source brief at `docs/diagnostics/failure-briefs/sprint32-uc-a-uc-c-alternate-uc-readiness.md` exists with all six §2 fields filled.
- [ ] Sprint 32 family directory has 5 visible `*.yaml` files (1 target + 2 neighbor + 2 negative) + 1 `_manifest.md`.
- [ ] Shadow directory has 2 `*.yaml` files at `eval_interactive/case_specs_shadow/sprint32_alternate_uc/`.
- [ ] Root manifest carries 10 family entries (was 9; +1 for Sprint 32). Verify by `grep -c "^  - family_id:" eval_interactive/case_specs/case_families/_manifest.yaml`.
- [ ] Shadow manifest has the Sprint 32 entry with real shadow ids (no `[REDACTED ...]`).
- [ ] Java test run produces 917/1-inherited/0/2 (byte-identical to Sprint 31 fix-iteration #2 baseline).
- [ ] 14-case smoke rerun produces no regression beyond noise (or surfaces external-drift continuation per §4.2 caveat).
- [ ] Sprint 32 family rerun's per-case `alternate_candidate_use_cases` extraction is in handoff §5.
- [ ] Handoff §8 nine-question anti-hardcode kernel walked.
- [ ] No `server/src/main/**` files in the staged commit.
- [ ] No production code, no prompt change, no Java test change.
- [ ] `compact/sprint-032-*-prompt.md` are NOT staged.
- [ ] Working-tree mock-data files from before session start are NOT staged.

Commit and surface the result for the deliver-agent to launch Codex review.
