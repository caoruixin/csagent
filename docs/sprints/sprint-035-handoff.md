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
  `docs/current/iteration_governance.md` §3.2 Q6. Probe /
  characterization sub-sprint; no production code change. Codex
  sprint-close review deferred to M1 milestone-shared close per §4.3
  default (no Tier-0 candidate, no §1.7 red line, no hard-fence
  violation). Closure verdict (§12) is left for the M1 milestone-
  shared decision; this archive captures the dev-session probe
  evidence + recommendation that supports decision (a) re-anchor of
  the §7.2 worked example (with (b) Option γ R-item as follow-on).
---

# Sprint 35 handoff — Option β coverage probe + §7.2 worked-example re-anchor decision

Date: 2026-05-16
Branch: `refactor/remove-the-shackles`
HEAD at session start: `e532f0d` (Sprint 34 close — intake field prefill UC-G/H/I/J).
Sub-sprint class: diagnostic / characterization, single track, read-only against production. Layer: `eval_spec` per `iteration_governance.md` §3.2 Q6.

## 1. Context Pack

Per `docs/current/agent_context_guide.md` Context Pack Prompt; produced before any probe authoring.

### 1.1 Relevant docs (sampled & read)

- `AGENTS.md` — durable-connective; current. Constitution-chain entry; transitively loads `doc_governance.md`, `agent_context_guide.md`, `iteration_governance.md`.
- `docs/milestone_objective.md` (M1) — current-runtime; current. §3 Sprint 35 row scope (eval_spec layer, milestone-shared Codex, hard fences); §5 milestone acceptance bar (Alice closure criterion + Option β matrix as secondary observation); §6 hard fences (no `RuntimeIntentClassifier` / `UseCaseRouter` / `DriftDetector` / `ClassifyUseCaseTool` touch; no Tier-0 added; no existing case-family edits; no `iteration_governance.md` edit EXCEPT Sprint 35 §7.2 fold-back via deliver-agent); §7 R-items (consumed `R-option-beta-coverage-gap-uc-a-uc-c-shape`; expected to surface `R-option-gamma-alternate-uc-surveyor-design` if decision (b) applies).
- `docs/sprint_objective.md` (Sprint 35) — current-runtime; current. §2 goal + decisions (a)/(b); §5 file table + §5.1 probe corpus design; §6 hard fences (15 items); §7 bundle policy; §8 §7 stanza; §9 success metrics; §10 stop conditions; §11 12-section handoff contract; §12 M1 milestone context.
- `docs/sprints/sprint-031-handoff.md` — sprint-archive. Sprint 31 ship of `alternate_candidate_use_cases` projection slot (Option β implementation); §13 4-AMBIGUOUS-intake observed cases that informed Sprint 32 neighbor selection.
- `docs/sprints/sprint-032-handoff.md` — sprint-archive. §13 in-flight downgrade investigation: empirical evidence that the §7.2 UC-A↔UC-C worked-example shape is OUTSIDE Option β coverage; cs32t01 alternates `[UC-A,UC-FP,UC-H]` lacked UC-C; neighbor #2 ROUTED not AMBIGUOUS. Sprint 35 directly closes the R-item Sprint 32 §13 surfaced.
- `docs/sprints/sprint-033-handoff.md` — sprint-archive. Sprint 33 shipped `discover_disambiguation_signals` slot (separate from Option β); the two slots coexist at HEAD.
- `docs/sprints/sprint-034-handoff.md` — sprint-archive. Sprint 34 shipped intake field prefill UC-G/H/I/J extension of `IntakeFieldExtractor`. Sprint 35 reads against the post-Sprint-34 cumulative state.
- `docs/current/iteration_governance.md` — durable-connective; current. §1.4 (Runtime owns trace + eval contract — Sprint 35 IS this), §1.6 (Eval is evidence, not authority), §1.7 (forbidden-list — Sprint 35 introduces nothing on it), §3.2 Q6 (eval_spec layer rationale), §4.1 (anti-hardcode kernel that Codex will walk at M1 close), §4.3 (Codex deferral rules — Sprint 35 defaults to M1-shared), §5.5 / §5.6 (smoke demoted; bad-case suite as primary gate — Sprint 35 acceptance is the matrix + decision), §7 (sprint-objective stanza — REQUIRED for semantic-touching sub-sprints; Sprint 35 layer = eval_spec, so REQUIRED), §7.2 worked example (the candidate re-anchor target at lines 615-639).
- `docs/action_bank.md` — line 667: `R-option-beta-coverage-gap-uc-a-uc-c-shape` is OPEN, last edited at Sprint 32 close. Sprint 35 will close it at deliver-agent commit per (a) or (b) decision.
- `eval_interactive/case_specs/bad_cases/_manifest.md` — durable-connective. The manifest shape Sprint 35 mirrors for the new probe directory.

### 1.2 Relevant code paths (verified at session start, 2026-05-16, HEAD `e532f0d`)

- `server/src/main/resources/config/use-case-registry.yaml` — topic mapping (4 weak-prior + 3 strong-prior + 4 handover-only) verified per §3 premise check below.
- `server/src/main/java/com/gumtree/csagent/service/runtime/UseCaseRouter.java` — `RoutingResult{ROUTED|OUT_OF_SCOPE|AMBIGUOUS}` record at lines 750-770. The deterministic stages: handover-only (line 317) → strong-prior (335) → UC-K regression override (355) → B2 phrase bias (381) → weak-prior LLM/AMBIGUOUS (399-419). The `routeNonBlocking` path (line 293) is the SessionManager createSession entry; AMBIGUOUS only fires when `allowLlm=false`.
- `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java` — `alternate_candidate_use_cases` slot at lines 396-416 (Sprint 31 ship); `discover_disambiguation_signals` slot at lines 418-433 (Sprint 33 ship). Both confirmed present at HEAD.
- `eval_interactive/eval_interactive/case_spec/loader.py:144` — globs `*.yaml`; flat-layout convention; loader silently drops extra YAML keys not in the dataclass shape (verified by loading all 25 probes with `probe_metadata` + `probe_observation_recipe` blocks — zero errors).
- `eval_interactive/eval_interactive/case_spec/schema.py:170-204` — `Expected.__post_init__` validators enforce `allow_bot_resolution ∈ {true/false/partial}`, `bot_handling_pattern` non-empty, `escalation_trigger/should_escalate` coupling. Probe CaseSpecs satisfy all three with minimal valid values.

### 1.3 Doc-status warnings (drift observed)

None substantive. The `iteration_governance.md` working-tree mods (deliver-agent owned: §4.3 + §5.5 / §5.6 additions per the 2026-05-16 governance upgrade) are visible in `git diff` but do NOT affect §7.2; the §7.2 worked-example still names UC-A↔UC-C drift at HEAD lines 615-639.

### 1.4 Source-of-truth decision

For this sub-sprint, the authoritative artefact is the **probe corpus + matrix + analysis doc** itself. The matrix observations are sourced from a single eval-harness run (`results/20260516-140339/results.json`) plus six recovered traces via the backend `/v1/demo/sessions/<sid>/trace` endpoint for contract-violation cases — both cited per cell in `docs/diagnostics/option_beta_coverage_matrix.md` §3.

Per `iteration_governance.md` §1.6 ("Eval is evidence, not authority"), the matrix is treated as evidence for the decision; the decision itself is made by deliver-agent + human at sub-sprint close.

### 1.5 Implementation status

For the question Sprint 35 is trying to answer: **implemented**. The intake router and the Option β projection slot are at HEAD and produced the matrix data. The §7.2 worked example as written is currently `not_started` (it was always hypothetical) — Sprint 35 shows it cannot be implemented under Option β as shipped, hence the re-anchor recommendation.

### 1.6 Risks before coding (probe / matrix authoring)

1. **Mis-shaped descriptions that accidentally trigger bias/override** would corrupt the matrix. Mitigated by carefully checking each probe description against the `UseCaseRouter` regexes BEFORE running, and recording the prediction in `probe_metadata.probe_expected_routing` so the matrix surfaces any prediction-vs-observation gaps.
2. **LLM-side noise in the bot loop** could shift `active_use_case` mid-loop in a way that obscures the routing decision. Mitigated by extracting from `per_turn_trace[0]` only (the FIRST turn's projection captures the post-`routeNonBlocking` state).
3. **The Sprint 31 §13 12-element fallback list** (observed on `cs_interactive_040`) could appear in other cells and confuse the matrix. Mitigated by recording the literal `candidate_use_cases` per cell + flagging unexpected fallback observations in §4.4 of the analysis doc (NOT fixed in this sprint).
4. **The contract-violation `per_turn_trace: []` placeholder** would lose AMBIGUOUS observations. Mitigated by recovering the missing 6 traces directly from the backend trace endpoint (§5 of the analysis doc + §5 of this handoff cite the fallback path).
5. **Tempting "fix" of observed coverage gaps in this sprint** — STOP per §10 stop condition #3. Sprint 35 ships zero runtime change; observed gaps land as OQs + R-items, not patches.

## 2. Sub-sprint-objective recap

Per `docs/sprint_objective.md`:

> Produce a per-(`topic_subject`, `description`-shape) AMBIGUOUS/ROUTED matrix that empirically characterizes where Option β (the Sprint 31 `alternate_candidate_use_cases` projection slot + Sprint 32 alternate-UC case family) DOES and DOES NOT provide coverage. Use the matrix to recommend one of two decisions at sub-sprint close: (a) re-anchor `iteration_governance.md` §7.2 worked example, OR (b) open a follow-on R-item `R-option-gamma-alternate-uc-surveyor-design`.

Sprint 35 dev produces the matrix + recommendation; deliver-agent + human confirm decision at sub-sprint close. Per Framing B, if (a) is confirmed the §7.2 fold-back is committed by the deliver-agent at Sprint 35 close as a separate commit (NOT bundled with the dev commit; `iteration_governance.md` is deliver-agent-owned per `feedback_commit_at_end_bundles_deliver_artefacts.md` + §1.7 read-only-against-runtime discipline).

## 3. Premise re-verification (§4 spot-check)

All seven premises in `docs/sprint_objective.md` §4 verified at session start, HEAD `e532f0d`:

1. **`use-case-registry.yaml` topic mapping** — confirmed at `server/src/main/resources/config/use-case-registry.yaml`:
   - `Ad Support` → UC-A (FAQ visibility), UC-B (FAQ posting), UC-FP (FAQ deletion-explanation), UC-H (INTAKE appeal) — 4 candidates ✓
   - `Payments` → UC-F (FAQ), UC-I (INTAKE) — 2 candidates ✓
   - `Technical Support` → UC-E (FAQ), UC-K (INTAKE) — 2 candidates ✓
   - `Account Support` → UC-D — 1 candidate ✓
   - Strong-prior: `Replies or Messaging` → UC-C, `Delete My Account or Data` → UC-G, `Report a Safety Issue` → UC-J ✓
   - Handover-only: `Delivery`, `Pro Contract`, `Ratings Reviews`, `Account Manager Support` ✓
2. **`UseCaseRouter` at HEAD** — confirmed `RoutingResult` carries `RoutingOutcome{ROUTED|OUT_OF_SCOPE|AMBIGUOUS}` (line 757) + `ambiguousCandidates: List<String>` (line 755). Deterministic stage ordering verified per §1.2 above. Not touched in Sprint 35.
3. **`ContextProjectionBuilder.alternate_candidate_use_cases` slot** — confirmed at lines 396-416 (Sprint 31 ship). Populated from `session.getIntakeAmbiguousCandidates()` minus the active UC. Schema-stable empty-array shape when no AMBIGUOUS branch was taken at session creation.
4. **`R-option-beta-coverage-gap-uc-a-uc-c-shape`** — confirmed OPEN at `docs/action_bank.md:667`. Disposition: "proposed (Sprint 32 §13 in-flight downgrade finding + Codex Finding 2 closure carry); planned for consumption by Milestone M1 sub-sprint 2 or 3 per `docs/milestone_objective.md`." Sprint 35 is M1 sub-sprint 3.
5. **`iteration_governance.md` §7.2 worked example** — confirmed at HEAD lines 615-639 (the `cs_example_001` hypothetical Sprint 18 fix for UC-A↔UC-C drift). Not edited in Sprint 35 dev session per §10 stop condition #4.
6. **`docs/diagnostics/` directory** — confirmed exists; carries `failure-briefs/`, `fix_layer_taxonomy.md`, `prompt_context_projection_audit.md` at HEAD. Sprint 35's `option_beta_coverage_matrix.md` lands here.
7. **`eval_interactive/case_specs/probe/` directory** — confirmed does NOT exist pre-session; created in Sprint 35 dev with the `option_beta_coverage/` flat-layout sub-directory.

Zero premise drift; proceed.

## 4. Implementation walkthrough — probe corpus design + run protocol + extraction recipe

### 4.1 Probe corpus design

The full 7 topics × 5 description-shapes = 35-cell cartesian was pruned to **25 cells** (the corpus shipped). Pruning rationale, recorded in both `eval_interactive/case_specs/probe/option_beta_coverage/_manifest.md` and `docs/diagnostics/option_beta_coverage_matrix.md` §2.1:

1. Strong-prior topics × `multi-issue-same-topic` (3 cells: Replies / Delete / Safety) — collapses to single-issue, since strong-prior topics have only one UC.
2. Strong-prior Delete × `ambiguous-soft` and × `empty-generic` (2 cells) — strong-prior wins regardless; one strong-prior Delete cell + one cross-topic Delete cell is sufficient control.
3. Strong-prior Safety × `ambiguous-soft` and × `empty-generic` (2 cells) — same rationale as Delete.
4. Handover-only Delivery × `single-issue` / `ambiguous-soft` / `empty-generic` (3 cells) — one handover-only override cell is sufficient; the override path is heavily covered by Sprint 10 / 11 / 32 existing tests.

Total pruned = 10 cells; total shipped = 25 cells. Final count is within the §5.1 design target (~25-30).

The 25 probe CaseSpecs were authored programmatically via a one-shot generator (`/tmp/gen_probes.py`, NOT committed — a development tool, not a runtime artefact) for consistency. Each YAML follows the standard `case_specs` schema + a `probe_metadata` block + a `probe_observation_recipe` block. The `expected.*` and `scoring.*` blocks carry minimal valid values to satisfy the loader's `__post_init__` validators (`schema.py:170-204`); they are NOT used as regression rubric — probe CaseSpecs are observation instruments per Sprint 35 §5.1.

Each probe carries `max_turns: 2` (turn 0 routing + one bot response) to bound LLM cost. Persona text is deliberately neutral ("ProbeUser", "probe@example.com", terse / no drift) so the probe measures the deterministic routing surface, not persona-driven LLM behaviour. The `seed_messages` first element is identical to `form_context.description` so the simulator submits the form-context shape consistently.

Per-cell `probe_expected_routing` predictions are derived from reading the `UseCaseRouter` stage ordering at HEAD (§1.2 above). Predictions appear in both the YAML file and the manifest table; the matrix in `option_beta_coverage_matrix.md` §3 records the OBSERVED routing.

### 4.2 Run protocol

```bash
cd eval_interactive
uv run eval-interactive run --path case_specs/probe/option_beta_coverage/ \
    --label sprint-35-option-beta-probe
```

Backend: local at `http://localhost:8080`, `local` profile, real Moonshot/Kimi `moonshot-v1-32k` LLM channel per `application-local.yml`. Per `feedback_mocked_llm_cannot_prove_prompt_causal_change.md` and Sprint 35 §6 hard fence #15: NO mocked LLM for probe runs.

Run completed: 2026-05-16, total elapsed 108 718 ms, 25 of 25 cases reached terminal state (no harness crash, no timeout outside per-case budget, no infrastructure issue). Six cases hit `stop_reason: contract_violation` with `failure_tags: ['CONTRACT_VIOLATION:active_use_case']` — these are the AMBIGUOUS-path cells where the bot did NOT commit a UC within the `max_turns: 2` budget. Per §6 of the analysis doc, this is itself matrix evidence — those cells are unambiguous `AMBIGUOUS` cells. The harness emits empty `per_turn_trace: []` placeholder for contract-violation case_results (Sprint 28 `_build_case_result` shape); the full turn-0 trace remains recoverable via the backend `/v1/demo/sessions/<sid>/trace` endpoint.

### 4.3 Extraction recipe

Per `docs/diagnostics/option_beta_coverage_matrix.md` §2.3. Two paths:

1. **Default (19 cases)**: read `results.json`:
   ```bash
   jq '.case_results[] |
       select(.case_id == "<probe_id>") |
       {active: .per_turn_trace[0].projection.session.active_use_case,
        alternates: .per_turn_trace[0].projection.alternate_candidate_use_cases,
        candidates: .per_turn_trace[0].projection.candidate_use_cases}' \
     eval_interactive/results/20260516-140339/results.json
   ```
2. **Fallback (6 contract-violation cases)**: hit the trace endpoint:
   ```bash
   curl -s "http://localhost:8080/v1/demo/sessions/<session_id>/trace" \
     | jq '.[0] | {active: .activeUseCase,
                   pc: (.projectedContext | fromjson |
                        {alternates: .alternate_candidate_use_cases,
                         candidates: .candidate_use_cases})}'
   ```
   Caveat encountered: the trace endpoint returns a content-negotiated placeholder shape when the `Accept: application/json` header is supplied (`curl -H "Accept: application/json"` returned a schema doc, not the JSON list; `curl` without the header returns the JSON list correctly). The matrix extraction used plain `curl` without the header. This is a backend content-negotiation quirk worth flagging as an OQ (§7) but NOT part of Sprint 35 scope to fix.

The six recovered session IDs are cited per cell in `option_beta_coverage_matrix.md` §3. The fallback recipe is reproducible — the session traces remain in the bot's persistence (until the bot is restarted).

## 5. Probe corpus run — result path + per-cell trace evidence + populated matrix

**Run output**: `eval_interactive/results/20260516-140339/results.json` (label `sprint-35-option-beta-probe`, 25 cases, elapsed 108 718 ms, harness summary: passed=0 / failed=25 — the 0% pass rate is **expected and meaningless** because probe CaseSpecs carry empty `scoring.*` rubrics, so every case scores composite=0; the matrix is the load-bearing output, NOT the pass rate).

**Six recovered contract-violation traces**:
```
9d07f987-3a4f-4a1b-87e6-562b0dbd6cc5  probe_ad_support_cross_topic_uc_c_drift            (§7.2 worked-example cell ★)
792d2bde-de2f-4491-9dd8-c8b0508eef29  probe_ad_support_empty_generic
5cd217da-ada6-4dca-b4d0-6044ceda7554  probe_ad_support_multi_uc_a_uc_h_neutral
96742752-0a1d-474a-b8f8-71209360e71b  probe_tech_support_cross_topic_uc_f_drift
da913451-5ce7-470a-81f9-37157222b9bb  probe_tech_support_multi_uc_e_uc_k_neutral
3c26ed47-38d5-4d11-a3a8-b72cb5a1364d  probe_tech_support_single_uc_e_faq_neutral
```

**Per-cell observation summary** (full matrix in `docs/diagnostics/option_beta_coverage_matrix.md` §3):

| topic_subject | shape | observed |
|---|---|---|
| Ad Support | single-issue (visibility) | ROUTED{UC-A} via B2 ADS_VISIBILITY_BIAS; alts=[] |
| Ad Support | single-issue (posting) | ROUTED{UC-B} mid-loop; alts=[UC-A, UC-FP, UC-H] |
| Ad Support | multi-issue same-topic | AMBIGUOUS; alts=[UC-A, UC-B, UC-FP, UC-H] |
| Ad Support | cross-topic-drift (UC-C) ★ | AMBIGUOUS; alts=[UC-A, UC-B, UC-FP, UC-H] — UC-C **absent** ★ |
| Ad Support | ambiguous-soft (Alice) | AMBIGUOUS; alts=[UC-A, UC-B, UC-FP, UC-H] |
| Ad Support | empty-generic | AMBIGUOUS; alts=[UC-A, UC-B, UC-FP, UC-H] |
| Payments | single-issue (FAQ) | ROUTED{UC-F} mid-loop; alts=[UC-I] |
| Payments | multi-issue same-topic | ROUTED{UC-I} mid-loop; alts=[UC-F] |
| Payments | cross-topic-drift (UC-D) | AMBIGUOUS; alts=[UC-F, UC-I] — UC-D **absent** |
| Payments | ambiguous-soft | AMBIGUOUS; alts=[UC-F, UC-I] |
| Tech Support | single-issue (FAQ) | AMBIGUOUS; alts=[UC-E, UC-K] |
| Tech Support | single-issue (regression) | ROUTED{UC-K} via UC-K override; alts=[] |
| Tech Support | multi-issue same-topic | AMBIGUOUS; alts=[UC-E, UC-K] |
| Tech Support | cross-topic-drift (UC-F) | AMBIGUOUS; alts=[UC-E, UC-K] — UC-F **absent** |
| Account Support | single-issue (login) | ROUTED{UC-D} via B2 LOGIN_BIAS; alts=[11 UCs] (12-UC fallback) ⚠ |
| Account Support | cross-topic-drift (UC-C+login) | ROUTED{UC-D}; alts=[] cands=[12 UCs] (login bias fires before messaging bias) |
| Account Support | ambiguous-soft no-bias | AMBIGUOUS; alts=[12 UCs] ⚠ 12-UC fallback list — Sprint 31 §13 pattern |
| Replies/Messaging | single-issue | ROUTED{UC-C} via strong-prior; alts=[] |
| Replies/Messaging | cross-topic-drift (UC-A) | ROUTED{UC-C}; alts=[] — UC-A **absent** |
| Replies/Messaging | ambiguous-soft | ROUTED{UC-C}; alts=[] |
| Delete | single-issue | ROUTED{UC-G} via strong-prior; alts=[] |
| Delete | cross-topic-drift (safety) | ROUTED{UC-J} (mid-loop reclassification from strong-prior UC-G); alts=[] ⚠ |
| Safety | single-issue | ROUTED{UC-J} via strong-prior; alts=[] |
| Safety | cross-topic-drift (UC-I) | ROUTED{UC-J}; alts=[] — UC-I **absent** |
| Delivery | cross-topic-drift (handover-only) | ROUTED{UC-J} via TOPIC_OVERRIDES fraud pattern; alts=[] cands=[] |

★ The §7.2 worked-example cell — Sprint 32 finding confirmed empirically.
⚠ Unexpected observation flagged for OQ (§7).

## 6. Generalization coverage table (adapted for a probe sprint)

| coverage class | description | satisfied? | source |
|---|---|---|---|
| **Target** | per-(topic, description-shape) matrix populated with cited per-cell extraction recipe | YES | `docs/diagnostics/option_beta_coverage_matrix.md` §3 (25 cells; 19 from `results.json`, 6 from recovered backend traces); §2.3 extraction recipes |
| **Neighbor** | N/A for a probe sprint — every cell IS the coverage. | — | — |
| **Negative / control** | strong-prior topics + handover-only sample serve as controls; expected ROUTED{single-UC} with empty alternates | YES (with 1 control surprise — Delete cross-topic mid-loop reclassification, §4.4.2 of analysis doc) | matrix rows Replies / Delete / Safety / Delivery |
| **Shadow** | N/A — probes are observation instruments, no held-out shadow concept. | — | — |
| **Control surprises flagged** | Account Support 12-UC fallback (§4.4.1); Delete cross-topic mid-loop reclassification (§4.4.2) | flagged, not fixed | §7 OQ below + analysis doc §4.4 |

Per §8 of `docs/sprint_objective.md`: probe sprints do NOT have a regression neighbor / shadow concept. Coverage success = the matrix is populated for the ~25-30 chosen cells with cited per-cell extraction recipes (achieved: 25 cells), analysis interprets the matrix (achieved: §4 of analysis doc), recommendation is grounded in cell evidence (achieved: §5 of analysis doc cites §4.1 / §4.2 / §4.3).

## 7. Open questions for human

**OQ1 (load-bearing for Sprint 35 close):** confirm decision (a) — re-anchor `iteration_governance.md` §7.2 worked example to UC-A↔UC-FP (or another within-Ad-Support pair) — as primary, with (b) Option γ R-item as follow-on. The probe matrix supports (a) with strong evidence (§4.1 + §4.2 of the analysis doc); (b) is the architecturally honest follow-on for the cross-topic gap (§5.2 of the analysis doc). Per Sprint 35 §10 stop condition #10, picking BOTH is fine if (a) is named primary and (b) as follow-on.

**OQ2 (informational; deliver-agent decides whether to open):** the Account Support 12-UC fallback observation (§4.4.1 of the analysis doc) — `candidate_use_cases` and (where applicable) `alternate_candidate_use_cases` return the entire 12-UC registry instead of `[UC-D]` on the single-UC weak-prior Account Support topic. Same shape as the Sprint 31 §13 cs_interactive_040 observation. **Candidate R-item**: `R-account-support-12-uc-fallback-investigation` (diagnostic; layer `prompt_projection`).

**OQ3 (informational):** `probe_delete_cross_topic_safety_drift` produced `active_use_case = UC-J` despite strong-prior `Delete My Account or Data` → UC-G (§4.4.2 of the analysis doc). The bot's `classify_use_case` tool re-routed mid-loop across the strong-prior boundary. This is a different surface than Option β; not a Sprint 35 finding to act on, but worth noting for the M2 lifecycle / Salesforce / handover scope.

**OQ4 (informational; out of Sprint 35 scope):** the backend `/v1/demo/sessions/<sid>/trace` endpoint content-negotiation quirk — with `Accept: application/json` header it returns a shape-doc placeholder instead of JSON. Plain `curl` (no Accept header) returns JSON correctly. Likely Spring Boot content-negotiation interaction with the existing demo endpoint. Could affect tool consumers that always send Accept headers. Flagged for awareness; not a load-bearing issue.

**OQ5 (informational):** the `probe_ad_support_single_uc_b_posting_neutral` outcome `ROUTED{UC-B}` with alts=[UC-A, UC-FP, UC-H] is interesting — the routing went through AMBIGUOUS at session creation (alternates populated), the LLM picked UC-B mid-loop. Similar to `probe_payments_multi_uc_f_uc_i_neutral` (active=UC-I, alts=[UC-F]). These illustrate Option β working as designed: the LLM owns the in-loop pick from the AMBIGUOUS candidate set; the alternate slot remains as the projected soft signal.

## 8. Anti-hardcode self-walk (§4.1 nine-question kernel)

Sprint 35 ships zero production code change. Walking the §4.1 nine questions:

1. **Adds a keyword / regex / if-else / enum / per-UC matrix for a semantic decision?** NO. Sprint 35 adds probe CaseSpecs (`form_context.description` text + `probe_metadata` documentation) and a diagnostic analysis doc. Neither is a runtime decision surface. The probe descriptions are deliberately written to exercise existing `UseCaseRouter` patterns; they do NOT modify those patterns.
2. **If yes to Q1, is the change justified as protecting a Tier-0 invariant?** N/A.
3. **Could the same outcome be achieved by projecting a soft signal to the LLM?** N/A — Sprint 35 ships nothing the LLM consumes.
4. **Does the change encode visible-eval case text, trace-specific phrasing, or a CaseSpec id into runtime, prompt, or judge config?** NO. The probe CaseSpec ids appear in the analysis doc as evidence citations, but they do NOT feed runtime / prompt / judge. They are new probe ids, not regression-eval ids; the runtime does not read them.
5. **Does the change move semantic ownership from the LLM to Java?** NO. Sprint 35 ships zero runtime change. The §7.2 fold-back (if (a) chosen, deliver-agent commit) replaces one UC pair with another in a worked-example paragraph — that's documentation alignment, not a semantic-ownership shift.
6. **Does the change add an if-else block to the prompt instead of principle-level guidance?** NO. The prompt is untouched.
7. **Does the change preserve tool schema, capability / permission boundary, PII / safety floor, and grounding floor?** YES. Sprint 35 ships zero edits to any of these surfaces.
8. **Does the PR ship generalization eval coverage — target, neighbor, negative, and shadow cases?** Adapted shape per probe sprint convention (§6 of this handoff): target = matrix coverage (25 cells); control cells = strong-prior + handover-only (7 cells of the 25); no shadow (probe sprints have no held-out shadow concept per Sprint 35 §8 of objective). The matrix surfaces control surprises (Account Support 12-UC fallback, Delete cross-topic reclassification) but does NOT fix them — they become OQs per §7 above.
9. **If the change is temporary, does it carry an explicit rollback or sunset plan?** N/A — the probe corpus is a permanent baseline reference (no sunset planned). The §7.2 fold-back (deliver-agent commit) is permanent. The (b) R-item is opened with explicit acceptance bar and consuming-sprint criteria.

**Expected verdict at M1 milestone-shared Codex close: `approve`.** Sprint 35 ships zero semantic hardcode; the change is read-only against the production runtime; the matrix + analysis doc + draft R-item text are diagnostic artefacts grounded in cited per-cell evidence.

## 9. Files changed (dev-authored, single commit)

| path | type | description |
|---|---|---|
| `eval_interactive/case_specs/probe/option_beta_coverage/_manifest.md` | NEW | Probe corpus manifest: purpose, lifecycle, per-case index, pruning rationale, schema extension notes. Mirrors `bad_cases/_manifest.md` shape. |
| `eval_interactive/case_specs/probe/option_beta_coverage/probe_ad_support_single_uc_a_visibility_bias.yaml` | NEW | Probe CaseSpec |
| `eval_interactive/case_specs/probe/option_beta_coverage/probe_ad_support_single_uc_b_posting_neutral.yaml` | NEW | Probe CaseSpec |
| `eval_interactive/case_specs/probe/option_beta_coverage/probe_ad_support_multi_uc_a_uc_h_neutral.yaml` | NEW | Probe CaseSpec |
| `eval_interactive/case_specs/probe/option_beta_coverage/probe_ad_support_cross_topic_uc_c_drift.yaml` | NEW | Probe CaseSpec — §7.2 worked-example cell |
| `eval_interactive/case_specs/probe/option_beta_coverage/probe_ad_support_ambiguous_soft_alice_shape.yaml` | NEW | Probe CaseSpec |
| `eval_interactive/case_specs/probe/option_beta_coverage/probe_ad_support_empty_generic.yaml` | NEW | Probe CaseSpec |
| `eval_interactive/case_specs/probe/option_beta_coverage/probe_payments_single_uc_f_faq_neutral.yaml` | NEW | Probe CaseSpec |
| `eval_interactive/case_specs/probe/option_beta_coverage/probe_payments_multi_uc_f_uc_i_neutral.yaml` | NEW | Probe CaseSpec |
| `eval_interactive/case_specs/probe/option_beta_coverage/probe_payments_cross_topic_uc_d_drift.yaml` | NEW | Probe CaseSpec |
| `eval_interactive/case_specs/probe/option_beta_coverage/probe_payments_ambiguous_soft.yaml` | NEW | Probe CaseSpec |
| `eval_interactive/case_specs/probe/option_beta_coverage/probe_tech_support_single_uc_e_faq_neutral.yaml` | NEW | Probe CaseSpec |
| `eval_interactive/case_specs/probe/option_beta_coverage/probe_tech_support_regression_uc_k_override.yaml` | NEW | Probe CaseSpec |
| `eval_interactive/case_specs/probe/option_beta_coverage/probe_tech_support_multi_uc_e_uc_k_neutral.yaml` | NEW | Probe CaseSpec |
| `eval_interactive/case_specs/probe/option_beta_coverage/probe_tech_support_cross_topic_uc_f_drift.yaml` | NEW | Probe CaseSpec |
| `eval_interactive/case_specs/probe/option_beta_coverage/probe_account_support_single_uc_d_login_bias.yaml` | NEW | Probe CaseSpec |
| `eval_interactive/case_specs/probe/option_beta_coverage/probe_account_support_cross_topic_uc_c_drift_with_login_bias.yaml` | NEW | Probe CaseSpec |
| `eval_interactive/case_specs/probe/option_beta_coverage/probe_account_support_ambiguous_soft_no_bias.yaml` | NEW | Probe CaseSpec |
| `eval_interactive/case_specs/probe/option_beta_coverage/probe_replies_messaging_single_uc_c_strong_prior.yaml` | NEW | Probe CaseSpec |
| `eval_interactive/case_specs/probe/option_beta_coverage/probe_replies_messaging_cross_topic_uc_a_drift.yaml` | NEW | Probe CaseSpec |
| `eval_interactive/case_specs/probe/option_beta_coverage/probe_replies_messaging_ambiguous_soft.yaml` | NEW | Probe CaseSpec |
| `eval_interactive/case_specs/probe/option_beta_coverage/probe_delete_single_uc_g_strong_prior.yaml` | NEW | Probe CaseSpec |
| `eval_interactive/case_specs/probe/option_beta_coverage/probe_delete_cross_topic_safety_drift.yaml` | NEW | Probe CaseSpec |
| `eval_interactive/case_specs/probe/option_beta_coverage/probe_safety_single_uc_j_strong_prior.yaml` | NEW | Probe CaseSpec |
| `eval_interactive/case_specs/probe/option_beta_coverage/probe_safety_cross_topic_uc_i_payment_drift.yaml` | NEW | Probe CaseSpec |
| `eval_interactive/case_specs/probe/option_beta_coverage/probe_handover_delivery_safety_override.yaml` | NEW | Probe CaseSpec — handover-only override sample |
| `docs/diagnostics/option_beta_coverage_matrix.md` | NEW | Analysis doc: purpose / method / matrix / observations / decision recommendation / draft R-item text |
| `docs/sprints/sprint-035-handoff.md` | NEW | This file (Sprint 35 dev-authored handoff archive) |

**Total**: 1 manifest + 25 probe CaseSpecs + 1 analysis doc + 1 handoff = **28 files**, all NEW, zero edits to existing files.

**Per Sprint 35 §7 bundle policy**: deliver-agent-owned files (`docs/sprint_objective.md`, `docs/milestone_objective.md`, `docs/10-handoff.md`, `docs/action_bank.md`, `docs/codex-findings.md`, `compact/sprint-035-dev-prompt.md`, `docs/current/iteration_governance.md`) are NOT staged in this commit. The §7.2 fold-back (if decision (a) confirmed) is a separate deliver-agent commit at Sprint 35 close.

## 10. Layer-classification self-walk (per §3)

Walk the §3.2 decision questions in order:

1. **Q1**: Session crashing on infra / timeout / OOM? NO. Backend ran clean, harness ran clean.
2. **Q2**: Tier-0 invariant being broken? NO. Sprint 35 ships zero runtime change.
3. **Q3**: LLM chose validly but projection / context was wrong / impoverished? NO direct claim — Sprint 35 OBSERVES what the projection emits per cell; it does not CLAIM the projection is wrong. The §7.2 worked example IS wrong in the sense that it describes a hypothetical the projection cannot fulfill, but the fix is to the GOVERNANCE doc (eval_spec), not the projection.
4. **Q4**: Multi-tool / multi-turn flow losing state across turns? NO — state durability is not the concern.
5. **Q5**: LLM choosing semantically wrong action even with correct projection / state? NO direct claim — the matrix observes LLM behaviour incidentally (e.g., Delete cross-topic mid-loop reclassification) but Sprint 35 does NOT prescribe a fix for LLM behaviour. Those are §7 OQs.
6. **Q6**: Eval CaseSpec or judge asking the system to do something it cannot or should not do — a factual or policy impossibility, a frozen enum the CaseSpec asks to extend, a mis-rubric? **YES**. The §7.2 worked example IS an eval-spec / governance instrument; the probe corpus + matrix IS eval-spec measurement; the §7.2 fold-back IS an eval-spec / governance adjustment. Sprint 35 lands on **`eval_spec`**.

First match wins per §3.2: Sprint 35 = `eval_spec`. Matches the Sprint 35 §8 stanza prediction and the M1 plan §3 Sprint 35 row prediction.

## 11. §5 Eval Acceptance bars (adapted for a probe sprint)

| §5.1 bar | Sprint 35 disposition | source |
|---|---|---|
| **Target cases pass** | N/A — probe CaseSpecs are observation instruments, no pass/fail scoring (composite=0 by design; matrix populated for all 25 cells is the success criterion). | `docs/diagnostics/option_beta_coverage_matrix.md` §3 (25 cells with citations) |
| **Neighbor cases no regression** | N/A — probe sprints have no neighbor concept (every cell IS coverage). | §8 of `docs/sprint_objective.md` |
| **Negative-control cases unchanged** | YES — strong-prior topic controls (Replies / Delete / Safety) routed deterministically with empty alternates on every cell. Two control SURPRISES flagged (Account Support 12-UC fallback; Delete mid-loop reclassification) — neither is a regression of Sprint 33 / 34 / 31 behaviour, they are observations of pre-existing surface behaviour. | matrix §3 rows + §4.4 commentary |
| **Shadow cases no regression** | N/A — probe sprints have no shadow concept. | §8 of `docs/sprint_objective.md` |
| **Safety floor unchanged** | YES — Sprint 35 ships zero edit to PII / safety / identity / imminent-harm surfaces. The probe CaseSpecs do not touch the safety floor; the harness ran clean. | hard fence §6.1 of `docs/sprint_objective.md` (no `server/src/main/` edit) |
| **Grounding floor unchanged** | YES — Sprint 35 ships zero edit to grounding / FAQ surfaces. The probe runs against the existing grounding floor; one bot turn per probe (max_turns=2) means most probes did not exercise grounding heavily — no regression. | same hard fence |
| **Wrong-containment rate unchanged or down** | N/A — Sprint 35 does not change runtime behaviour; containment is unchanged by construction. | n/a |
| **Over-escalation rate unchanged or down** | N/A — same as above. | n/a |
| **Architecture-health metrics not regressed** | YES — `new_semantic_hardcode_count` = 0 (no runtime code); `soft_signal_conversion_count` = 0 (no soft-signal work this sprint); `planner_ownership_ratio` unchanged. | §8 anti-hardcode self-walk above (all Qs NO/N/A); collection_status remains `not_started` per §6 of `iteration_governance.md`. |
| **Java baseline** | YES — `mvn -q test` returned **Tests run: 983, Failures: 1, Errors: 0, Skipped: 2**. The 1 failure is the pre-existing inherited `SystemPromptUserRequestedTiebreakerTest:53 ACTIVE-UC TIEBREAKER must be tagged with the Sprint 6 anchor` from the unauthored `system_prompt.txt:60` working-tree mod that has been the documented baseline since Sprint 24-era. Zero new Sprint 35-attributable regressions (Sprint 35 ships zero Java code change). | run timestamp 2026-05-16 22:13:45 local |
| **§5.5 / §5.6 smoke + bad-case** | N/A — Sprint 35 §7 of objective explicitly skips smoke and bad-case rerun (covered at M1 milestone close across the cumulative Sprint 33 / 34 / 35 commit range). | §7 of `docs/sprint_objective.md` |

Hard close gates for Sprint 35 dev commit:

- ✓ All §5 dev-authored files in one commit (28 files, ready)
- ✓ Probe corpus ran to completion (25 / 25 cells; no harness crash; 6 contract-violation cells recovered via backend trace endpoint per §2.3 fallback)
- ✓ Per-cell matrix populated with extraction recipes (`option_beta_coverage_matrix.md` §3)
- ✓ Analysis doc §5 recommends decision (a) primary + (b) follow-on with cited per-cell evidence
- ✓ Java baseline preserved (983 / 1-inherited / 0 / 2)
- ✓ Anti-hardcode self-walk § 8 above arrives at `approve`
- ✓ Reproducibility: every quantitative claim (HEAD SHA, file paths, session IDs, result run id, test counts, harness elapsed) is cited

## 12. Closure verdict placeholder

Per §11 of `docs/sprint_objective.md` and `feedback_handoff_verdict_section_delegation.md`: Sprint 35's closure verdict is **deferred to the M1 milestone-shared decision** per `iteration_governance.md` §4.3 default (no Tier-0 candidate, no §1.7 red line, no hard-fence violation, no fix-iteration on prior sub-sprint).

This sub-sprint contributes to the M1 close evidence pool:

- The probe corpus + matrix + recommendation are the load-bearing artefacts for closing `R-option-beta-coverage-gap-uc-a-uc-c-shape` (`docs/action_bank.md:667`).
- The recommendation (decision (a) primary, decision (b) follow-on) is the input for the deliver-agent + human Sprint 35 close decision.
- If (a) confirmed: deliver-agent commits the `iteration_governance.md` §7.2 fold-back as a SEPARATE commit at Sprint 35 close (not bundled with this dev commit).
- If (b) confirmed (whether as primary or follow-on): deliver-agent opens `R-option-gamma-alternate-uc-surveyor-design` in `docs/action_bank.md` using the draft text in `docs/diagnostics/option_beta_coverage_matrix.md` §6.
- M1 milestone close (after Sprint 35 + any conditional Sprint 36) dispatches the milestone-shared Codex review against the cumulative Sprint 33 + 34 + 35 (+ 36) commit range per §4.3.

Sprint 35 dev does NOT issue a PASS / FAIL / fix_required verdict here; that is the deliver-agent + human decision at sub-sprint close.

End of dev-session handoff. Sprint 35 dev signs off; deliver-agent + human take over.
