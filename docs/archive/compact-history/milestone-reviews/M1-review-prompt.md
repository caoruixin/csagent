Paste the content below this line into a fresh Codex session after Sprint 33 + 34 + 35 dev commits land. No PR will be opened; review the cumulative commit range directly.

---

You are the Anti-Hardcode + Milestone-Close Review Agent for **Milestone M1 (DISCOVER + Intake)** — the first milestone under the §8 milestone framework introduced 2026-05-16 in `docs/current/iteration_governance.md`. M1 bundles three semantic-touching sub-sprints whose cumulative commit range you review as one unit per the §4.3 "milestone-shared Codex review at close" default.

**Cumulative commit range:** `c9edb37..eb65e2b` (3 dev commits).

| sub-sprint | commit | layer | one-line scope |
|---|---|---|---|
| Sprint 33 | `8a22aa6` | `prompt_projection` | DISCOVER soft signal `discover_disambiguation_signals` + DISCOVER systemInstruction extension + `system_prompt.txt` teaching paragraph |
| Sprint 34 | `e532f0d` | `skill_state` | `IntakeFieldExtractor` extension UC-K → UC-G/H/I/J (UC-I deliberate no-op for symmetry); zero edit to `AgentRunLoopImpl` (zero-edit prediction held) |
| Sprint 35 | `eb65e2b` | `eval_spec` | Probe corpus + matrix at `docs/diagnostics/option_beta_coverage_matrix.md`; closes `R-option-beta-coverage-gap-uc-a-uc-c-shape`; surfaces `R-loosen-topic-uc-binding-llm-owned-drift` (constraint-removal / architecture-alignment) |

**Per-sub-sprint Codex review was deferred** to this milestone-shared round per §4.3 default (no Tier-0 candidate, no §1.7 red line, no hard-fence violation, no fix-iteration). You are the FIRST Codex pass on the M1 cumulative range.

## 1. Loader

1. `AGENTS.md` (transitively loads `iteration_governance.md` §1 / §3 / §4 / §5 / §7 / §8 + `doc_governance.md` + `agent_context_guide.md`).
2. `docs/milestone_objective.md` — the M1 north star. §2 goal; §3 sub-sprint sequence; §4 non-goals; §5 milestone acceptance bar; §6 milestone hard fences; §7 R-items; §8 Codex review plan (milestone-shared default — THIS review).
3. `docs/sprints/sprint-033-objective.md` + `docs/sprints/sprint-033-handoff.md` — Sprint 33 contract + dev evidence.
4. `docs/sprints/sprint-034-objective.md` + `docs/sprints/sprint-034-handoff.md` — Sprint 34 contract + dev evidence.
5. `docs/sprints/sprint-035-objective.md` + `docs/sprints/sprint-035-handoff.md` — Sprint 35 contract + dev evidence.
6. `docs/current/iteration_governance.md` §1.3 / §1.4 / §1.5 / §1.6 / §1.7 (Constitution + forbidden list) + §3.2 (layer classification) + §4.1 (your nine-question kernel) + §4.3 (milestone-shared review rules — your scope) + §5.1 / §5.5 / §5.6 (acceptance bars + smoke-demotion + bad-case-suite-as-primary-gate) + §7 (sprint-objective stanza requirement) + §8 (milestone framework).
7. `docs/action_bank.md` §5.2 Sprint 32 + Sprint 35 surfaced R-items subsections (lines ~657–710): verify `R-option-beta-coverage-gap-uc-a-uc-c-shape` is CLOSED by Sprint 35 with the (a) considered-and-dropped framing + (b) succeeded-by note; verify `R-loosen-topic-uc-binding-llm-owned-drift` is opened as **constraint-removal / architecture-alignment** with explicit anti-framings (NO new Java `AlternateUseCaseSurveyor`; NO per-UC regex; the cage is the issue, not the field).
8. `eval_interactive/case_specs/bad_cases/alice_uc_a_uc_h_misclass.yaml` — the M1 acceptance-bar case.
9. `eval_interactive/case_specs/bad_cases/_manifest.md` — the bad-case suite convention.
10. `docs/diagnostics/option_beta_coverage_matrix.md` — Sprint 35 analysis doc. Verify the matrix is populated for every probe cell with cited extraction recipes; verify §5 decision recommendation matches the action_bank R-item flips.
11. `eval_interactive/case_specs/probe/option_beta_coverage/_manifest.md` + `*.yaml` — Sprint 35 probe corpus (25 CaseSpecs).
12. `compact/sprint-033-dev-prompt.md` + `compact/sprint-034-dev-prompt.md` + `compact/sprint-035-dev-prompt.md` — what each dev was authorized to do vs what landed.
13. `eval_interactive/results/20260516-110928/results.json` (Sprint 33 single-run Alice trace) + `eval_interactive/results/20260516-140339/results.json` (Sprint 35 probe run) + `eval_interactive/results/20260516-232938/results.json` (**M1-close Alice rerun — see §1.5 below**) — re-extract per-cell / per-trace extraction recipes from Sprint 35 handoff §5 and Sprint 33 handoff §5 to verify cited numbers reproduce.

## 1.5 Pre-Codex Evidence Pack — M1-close Alice rerun (deliver-agent extracted, 2026-05-17)

The M1 primary acceptance gate per `iteration_governance.md` §5.6 is manual review of the bad-case suite traces against the cumulative commit range. Sprint 33 ran Alice once at `8a22aa6` with closure-criterion (a) PASS (`eval_interactive/results/20260516-110928/results.json`). The deliver-agent re-ran the bad-case suite at HEAD `eb65e2b` (M1 cumulative; backend = `make backend` profile=local, real Moonshot/Kimi LLM channel) and extracted the trace below as the second data point for **multi-trace stability** evidence. Codex may re-derive this independently per §7.2; the evidence pack is provided so Codex has the same baseline the deliver-agent + human used.

**Result path:** `eval_interactive/results/20260516-232938/results.json` (label `m1-close-alice-rerun`).

**Top-level outcome fields** (extracted via `jq '.case_results[] | select(.case_id == "alice_uc_a_uc_h_misclass") | {active: .active_use_case, total_turns: .total_turns, stop_reason: .stop_reason, judge_score: .judge_score, case_passed: .case_passed, containment_outcome: .containment_outcome, outcome_class: .outcome_class}'`):

```json
{
  "active": "UC-A",
  "total_turns": 3,
  "stop_reason": "bot_ended",
  "judge_score": 0.8667,
  "case_passed": false,
  "containment_outcome": "escalated",
  "outcome_class": null
}
```

**Sprint 33 `discover_disambiguation_signals` slot at turn 0** (extracted via `jq '.case_results[] | select(.case_id == "alice_uc_a_uc_h_misclass") | .per_turn_trace[0].projection.discover_disambiguation_signals'`):

```json
{
  "ad_status_observed": "REMOVED",
  "candidate_ucs_for_topic": ["UC-A", "UC-B", "UC-FP", "UC-H"],
  "topic_subject_carries_multiple_candidate_ucs": true
}
```

Sprint 33 slot fired correctly: REMOVED listing + Ad Support topic → multi-candidate disambiguation surface populated. (Matches Sprint 33 handoff §5 single-run.)

**Sprint 31 `alternate_candidate_use_cases` slot at turn 0**: `[]` (empty). Expected — Alice routed via strong-prior / deterministic path, not via intake AMBIGUOUS branch (the only branch that populates Sprint 31's slot per `SessionManager.createSession.java:185-189`). Same as Sprint 33 single-run.

**Tool sequence** (extracted via `jq '.case_results[] | select(.case_id == "alice_uc_a_uc_h_misclass") | [.per_turn_trace[].tool_calls[]? | .tool_name]'`):

```
search_knowledge → resolve_article → search_knowledge → search_knowledge → search_knowledge → resolve_article → request_handover
```

Final `request_handover` arguments: `{"escalation_reason": "faq_miss_threshold_exceeded"}`. (Sprint 33 single-run escalated via `user_requested` shape; M1-close rerun via `faq_miss_threshold_exceeded` after the bot hit the `faq_miss` threshold trying to find the specific removal reason. Both are honest escalation shapes — bot did NOT lie about `intake_complete_for_uc_h`.)

**Bot turn 1 verbatim response** (the grounded answer):

> Hi Alice, I've checked your ad (AD-2001) and it shows as 'Removed', which means it was taken down due to a policy violation. You should have received an email explaining which policy was broken — please check your inbox and spam folder. You can review our posting policies and repost your ad anytime, as long as you don't continue to break the rules. For more details, see our article: https://help.gumtree.com/s/technical-issues?cat=Ad_Issues&article=My-Ad-was-Removed

Grounded with canonical_url citation to `My Ad was Removed` (`ka44J000000gKv5QAE`). No fabrication.

**Closure-criterion verdict per CaseSpec**:

> (a) Bot routes the conversation to UC-A handling (FAQ-path for ad visibility questions), runs search_knowledge / resolve_article to retrieve the removal-reason FAQ surface, and answers the user's actual question (why was the ad removed, where to see it, how to appeal if applicable).

✅ **PASS on (a).** `active_use_case=UC-A` throughout; FAQ-path tools fired; grounded with source citation; user's question answered.

**FAIL conditions per CaseSpec**:

> bot enters > 6 turns AND ends with non-canonical request_handover or runs out of budget without progress; bot fabricates an answer about why the ad was removed; bot calls request_handover(intake_complete_for_uc_h) when fields_collected is empty.

✅ **All FAIL conditions absent.** 3 turns (well within 6); `escalation_reason=faq_miss_threshold_exceeded` is canonical; no fabrication (bot honestly admits inability to find the specific removal reason); no `intake_complete_for_uc_h` lie.

**`case_passed: false` orthogonality note**: same as Sprint 33 §5 / OQ4 carry-forward — the bad-case CaseSpec's eval rubric `L1:trace_minimum` / `L2:correct_outcome` expects `record_outcome(resolve)` and a different tool-sequence shape. The bot did NOT call `record_outcome` because it escalated; `outcome_class` is null. This is the Sprint 11/Sprint 23 progressive-resolve / `containment_outcome` surface concern, **NOT a D1 mis-classification failure** and NOT a Sprint 33/34/35 attributable regression. M2 candidate per Sprint 33 handoff §7 OQ4. **Does NOT block M1 close.**

**Multi-trace stability assessment**: Sprint 33 single-run PASS on (a) + M1-close rerun PASS on (a) = **two-trace consistency** on Alice's D1 dimension. LLM variance between the two runs is observable (different stop_reason path, different escalation shape) but both land on the same closure-criterion sub-bullet. The DISCOVER classification surface is stable across the M1 cumulative commit range (Sprint 34 zero-DISCOVER-touch + Sprint 35 zero-runtime-code confirm this is preserved by construction at the Java layer; LLM-variance is the only risk and the two-trace evidence shows the same path is taken).

**Codex SHALL still re-run §7.2 per protocol** (a third trace = three-trace stability evidence); the evidence pack here is the deliver-agent + human baseline, not a substitute for Codex independent verification.

## 2. Scope-discipline gate (BLOCKING, per sub-sprint)

Each commit's diff must respect that sub-sprint's hard fences. Walk per sub-sprint:

### 2.1 Sprint 33 (`8a22aa6 ^..8a22aa6`)
Allowed surfaces:
- `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java` (new `discover_disambiguation_signals` slot + helpers)
- `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java` (DISCOVER `systemInstruction` extension at lines 411-431 area only)
- `server/src/main/resources/prompts/system_prompt.txt` (new teaching paragraph adjacent to `already_called` + `alternate_candidate_use_cases`)
- `server/src/test/java/com/gumtree/csagent/service/runtime/DiscoverDisambiguationSignalsProjectionTest.java` (new)
- `server/src/test/java/com/gumtree/csagent/integration/AgentRunLoopDiscoverDisambiguationNonEnforcementIntegrationTest.java` (new)
- `docs/sprints/sprint-033-handoff.md` (new dev archive)

Forbidden surfaces (any in the diff → BLOCKING):
- `RuntimeIntentClassifier.java` / `IntentClassification.java` / `DriftResult.java` / `DriftDetector.java` / `UseCaseRouter.java` / `ClassifyUseCaseTool.java`
- `IntakeFieldExtractor.java` / `INTAKE_UCS` at `PhaseEvaluator.java:30`
- `escalation_reason` enum at `PhaseEvaluator.java:39-63`
- `eval_interactive/eval_interactive/` (harness/loader/simulator)
- existing case families under `eval_interactive/case_specs/case_families/`
- `eval_interactive/case_specs_shadow/` (any)
- `eval_interactive/case_specs/smoke/` / `eval_interactive/case_specs/bad_cases/` (any)
- `eval_interactive/case_spec_overrides.yaml`
- `docs/foundational/` / `docs/current/iteration_governance.md` / `docs/current/doc_governance.md` / `docs/current/agent_context_guide.md`
- `docs/runtime_freeze_and_risk_policy.md`
- `docs/sprints/sprint-001-*` through `docs/sprints/sprint-032-*` (existing archives)
- `docs/milestone_objective.md` / `docs/sprint_objective.md` / `docs/10-handoff.md` / `docs/action_bank.md` / `docs/codex-findings.md` / `compact/*` (deliver-agent-owned)

### 2.2 Sprint 34 (`8a22aa6..e532f0d`)
Allowed surfaces:
- `server/src/main/java/com/gumtree/csagent/service/runtime/IntakeFieldExtractor.java` (extend `extractFromTurn` dispatch + new per-UC helpers UC-G/H/I/J + new `extractFormContextField` helper + `handlesUc` extension; class Javadoc update)
- `server/src/test/java/com/gumtree/csagent/service/runtime/Sprint34IntakePrefillExtractorTest.java` (new, ~33 tests)
- `server/src/test/java/com/gumtree/csagent/service/runtime/Sprint34IntakePrefillProjectionAndGuardTest.java` (new, ~12 tests)
- `server/src/test/java/com/gumtree/csagent/integration/AgentRunLoopUcGHIJIntakePrefillIntegrationTest.java` (new, 6 parameterised variants)
- `docs/sprints/sprint-034-handoff.md` (new dev archive)

Forbidden surfaces (any in the diff → BLOCKING):
- `IntakeFieldsRegistry.java` (schema stable; Sprint 34 consumes existing aliases only)
- `AgentRunLoopImpl.java` (zero-edit prediction held; verify diff is empty for this file)
- existing UC-K helpers in `IntakeFieldExtractor.java` (`extractUcKFields`, `capturePlatform`, `captureRegressionText`, `PLATFORM_TOKEN_PATTERN`, `REGRESSION_MARKER_PATTERN`) — byte-identical
- `Sprint71PartialIntakePersistenceTest.java` — UC-K regression guard, must remain byte-identical
- `ContextProjectionBuilder.java` (Sprint 33 shipped its only Sprint-33-attributable touch)
- `FormContextIngestionService.java` (form_context JSON schema stable)
- `system_prompt.txt` (no prompt edit in Sprint 34)
- All other surfaces from §2.1 Sprint 33 forbidden list (plus Sprint 33's own commit scope is not Sprint 34 scope)

### 2.3 Sprint 35 (`e532f0d..eb65e2b`)
Allowed surfaces:
- `eval_interactive/case_specs/probe/option_beta_coverage/_manifest.md` (new directory manifest)
- `eval_interactive/case_specs/probe/option_beta_coverage/*.yaml` (new probe CaseSpecs, ~25)
- `docs/diagnostics/option_beta_coverage_matrix.md` (new analysis doc)
- `docs/sprints/sprint-035-handoff.md` (new dev archive)

Forbidden surfaces (any in the diff → BLOCKING):
- ANY file under `server/src/main/` (Sprint 35 ships zero production code)
- ANY file under `server/src/test/` (zero Java test edit)
- ANY file under `eval_interactive/eval_interactive/` (harness untouched)
- existing case families / shadow / smoke / bad cases / overrides
- `docs/current/iteration_governance.md` — **critical**: per the constitution-discipline review at Sprint 35 close (2026-05-17), the §7.2 fold-back was *considered and dropped*; the §7.2 worked example must remain at UC-A↔UC-C illustration. Verify `iteration_governance.md` is absent from the Sprint 35 diff range.
- All deliver-agent-owned files

### 2.4 Milestone-level hard fences (per `milestone_objective.md` §6)

Verify across the cumulative range:
1. NO edits to `RuntimeIntentClassifier.java` / `IntentClassification.java` / `DriftResult.java` / `DriftDetector.java` / `UseCaseRouter.java` / `ClassifyUseCaseTool.java`.
2. NO Tier-0 invariant added (no edits to `docs/runtime_freeze_and_risk_policy.md`).
3. NO edits to existing case families.
4. NO edits to `eval_interactive/eval_interactive/`.
5. NO edits to `iteration_governance.md` (the §7.2 fold-back authorized in M1 §3 was dropped on constitution-discipline review at Sprint 35 close; this is captured in `docs/action_bank.md` §5.2 closed R-item disposition and in `.claude/agent-memory/sprint-deliver-orchestrator/feedback_constitution_discipline_vs_planning_anticipation.md`).
6. NO widening of `eval_interactive/case_spec_overrides.yaml`.
7. NO edits to sprint archives under `docs/sprints/sprint-001-*` through `docs/sprints/sprint-032-*`.
8. NO edits to `docs/foundational/`.
9. NO widening of `escalation_reason` enum at `PhaseEvaluator.java:39-63`.

Surface any milestone-level fence violation as Finding #1 with cited diff snippet.

## 3. §4.1 Anti-Hardcode kernel walk — three passes (one per sub-sprint)

Walk the nine-question kernel three times — once per sub-sprint. Each sub-sprint touches a different semantic layer; the expected verdict is `approve` for each.

### 3.1 Sprint 33 — `prompt_projection` layer
Expected: approve. Key checks: Q1 (no regex on user content; the new slot is built from runtime-observable state — `listing_context.status` + `form_topic_subject` + `useCaseRegistry.getCandidateUcsForTopic`); Q4 (no eval-case text encoded — only UC IDs by way of example in DISCOVER prompt); Q5 (LLM still owns classification — proved by `AgentRunLoopDiscoverDisambiguationNonEnforcementIntegrationTest` 6-variant invariance); Q6 (DISCOVER teaching is principle-level, not if-else dump); Q8 (target + neighbor + negative shipped; shadow deferred to milestone-close per §5.6).

### 3.2 Sprint 34 — `skill_state` layer
Expected: approve. Key checks: Q1 (per-UC extractor branches are FORM-CONTEXT field mapping plumbing, NOT semantic decision — the LLM's classify_use_case decision happens before `mergePartialIntakeFromContext` fires); Q3 (could be a soft signal? No — this is plumbing for already-existing soft-signal slot `intake_state`); Q5 (LLM still owns classification + inline `intake_fields` capture + escalation choice — verified by Sprint34 projection tests + the `shouldRejectIncompleteIntakeHandover` guard staying untouched); Q8 (target + neighbor UC-K regression + negative UC-A FAQ-path shipped at Java layer; shadow deferred).

### 3.3 Sprint 35 — `eval_spec` layer
Expected: approve. Key checks: Q1 (probe CaseSpecs are observation instruments; no decision-logic encoded); Q4 (probe CaseSpec ids do NOT feed runtime / prompt / judge — they are new probe ids); Q5 (Sprint 35 ships zero runtime change); Q8 (matrix coverage is the target; control cells + matrix surprises documented in handoff §7 OQs).

## 4. §1.7 + §1.6 checks (BLOCKING)

### 4.1 §1.7 forbidden-list check
- Sprint 33 did NOT add a Java decision-path branch on `listing_context.status` for routing (the soft signal + prompt teaching is the design — verified by integration-test 6-variant invariance).
- Sprint 34 did NOT add a regex on user-typed content for UC-G/H/I/J extraction (form-context plumbing only; the existing UC-K user-content regex stays UC-K-scoped per hard fence).
- Sprint 35 ships zero runtime change; nothing to encode.
- M1 milestone-shared: the deliver-agent + human closed `R-option-beta-coverage-gap-uc-a-uc-c-shape` by opening a constraint-removal R-item (NOT by adding a new Java component — explicitly rejected the "Option γ `AlternateUseCaseSurveyor`" framing per §1.7 anti-hardcode discipline). Verify this is captured in `docs/action_bank.md` §5.2 new R-item entry.

### 4.2 §1.6 evaluation-rule check
- No widened CaseSpec accepts a genuine bot mistake. The probe CaseSpecs in Sprint 35 explicitly carry minimal valid `expected.*` / `scoring.*` blocks per the loader's validators; they are NOT regression rubrics.
- No judge rubric was relaxed.
- Smoke composite_score is observation per §5.5; M1 close does NOT use smoke as a gate.

## 5. Hard-fence verification (per sub-sprint + milestone-level)

Re-run the diff scans per §2.1 / §2.2 / §2.3 / §2.4 against the cumulative range. Document each fence as pass/fail per sub-sprint commit.

Cumulative range diff:
```bash
git diff --stat c9edb37..eb65e2b -- \
    server/src/main/java/com/gumtree/csagent/service/runtime/RuntimeIntentClassifier.java \
    server/src/main/java/com/gumtree/csagent/service/runtime/IntentClassification.java \
    server/src/main/java/com/gumtree/csagent/service/runtime/DriftResult.java \
    server/src/main/java/com/gumtree/csagent/service/runtime/DriftDetector.java \
    server/src/main/java/com/gumtree/csagent/service/runtime/UseCaseRouter.java \
    server/src/main/java/com/gumtree/csagent/service/tools/ClassifyUseCaseTool.java \
    docs/runtime_freeze_and_risk_policy.md \
    docs/current/iteration_governance.md \
    eval_interactive/case_specs/case_families/ \
    eval_interactive/case_specs_shadow/case_families/ \
    eval_interactive/case_specs/smoke/ \
    eval_interactive/case_specs/bad_cases/ \
    eval_interactive/eval_interactive/ \
    eval_interactive/case_spec_overrides.yaml
```

Expected output: empty (all listed paths untouched). Any non-empty entry → BLOCKING fence violation.

## 6. Schema and reproducibility checks

### 6.1 Per sub-sprint reproducibility

For each cited number in each handoff (Sprint 33 §5 trace evidence; Sprint 34 §11 baseline 983/1-inherited/0/2 + §5 integration variants; Sprint 35 §5 matrix observation table + §11 baseline), re-run the cited extraction recipe (or path read) and confirm bit-identical output.

Sprint 33 Alice trace:
```bash
jq '.case_results[] | select(.case_id == "alice_uc_a_uc_h_misclass") | {active: .per_turn_trace[-1].projection.session.active_use_case, turns: .total_turns, judge: .judge_score, stop: .stop_reason}' \
  eval_interactive/results/<sprint-33-alice-run-id>/results.json
```
(Re-extract from the run id cited in Sprint 33 handoff §5; expect `active=UC-A`, `turns=4`, `stop=goal_achieved`.)

Sprint 35 matrix per-cell:
```bash
jq '.case_results[] | select(.case_id == "<probe_id>") | {active: .per_turn_trace[0].projection.session.active_use_case, alternates: .per_turn_trace[0].projection.alternate_candidate_use_cases}' \
  eval_interactive/results/20260516-140339/results.json
```
(Run for each of the 25 probe ids; compare to matrix §3 of the analysis doc. For the 6 contract-violation cells, use the backend trace endpoint fallback per Sprint 35 handoff §4.3.)

### 6.2 Schema validation

Sprint 35 probe CaseSpecs:
```bash
cd eval_interactive && uv run python -c "from eval_interactive.case_spec.loader import load_case_specs; specs = load_case_specs('case_specs/probe/option_beta_coverage'); print(len(specs))"
```
Expected: 25.

Sprint 33 + 34 new Java tests load + pass:
```bash
mvn -q -pl server test -Dtest='DiscoverDisambiguationSignalsProjectionTest,AgentRunLoopDiscoverDisambiguationNonEnforcementIntegrationTest,Sprint34IntakePrefillExtractorTest,Sprint34IntakePrefillProjectionAndGuardTest,AgentRunLoopUcGHIJIntakePrefillIntegrationTest,Sprint71PartialIntakePersistenceTest'
```
Expected: all PASS; `Sprint71PartialIntakePersistenceTest` 14/14 unchanged (UC-K regression guard).

## 7. Validation runs (you re-execute)

From a clean checkout of HEAD (`eb65e2b`):

### 7.1 Java baseline preservation
```bash
mvn -q -pl server test
```
Expected: **`Tests run: 983, Failures: 1, Errors: 0, Skipped: 2`** — Sprint 34 baseline preserved through Sprint 35 (zero Java change). The 1 failure is the pre-existing inherited `SystemPromptUserRequestedTiebreakerTest` from the Sprint 24-era `system_prompt.txt` working-tree mod (documented baseline). Any other delta is a BLOCKING finding.

### 7.2 M1 primary-gate bad-case suite rerun (Alice closure-criterion stability)
```bash
cd eval_interactive && uv run eval-interactive run --path case_specs/bad_cases/
```
Backend at `http://localhost:8080` via `make backend` profile=local, real Moonshot/Kimi LLM channel.

Per `iteration_governance.md` §5.6: this IS the primary acceptance gate for M1 close. Manual review the per-turn trace; confirm Alice closure-criterion sub-bullet (a) PASS — bot routes to UC-A FAQ path, grounds with source citation, gracefully escalates if user presses for specifics. The Sprint 33 single-run PASS plus this M1-close rerun = multi-run stability evidence.

If Alice on M1-close rerun flips to UC-H (LLM variance):
- Verify Sprint 34's intake prefill plumbing fires (alts populated; form_context `ad_id=AD-2001` + `email=alice.removed@example.com` appear as `intake_state.fields_collected`)
- Verify bot does NOT call `request_handover(intake_complete_for_uc_h)` with empty `stated_reason_or_context` (Sprint 34's negative-control guard)
- This is also M1-acceptable per the CaseSpec's closure-criterion (c) graceful escalation

If Alice closure-criterion FAILS (all three sub-bullets fail; bot enters dead loop OR fabricates OR lies about intake_complete): SURFACE as BLOCKING. M1 cannot close.

### 7.3 (Optional) 14-case smoke regression check
```bash
cd eval_interactive && uv run eval-interactive run --path case_specs/smoke/
```
Smoke is observation only per §5.5 — does NOT gate M1 close. Compare summary numbers to Sprint 28 reference (`eval_interactive/results/20260514-181257/results.json`). Run-to-run variance is expected (Sprint 31 §13 documented +84% external LLM provider drift). Surface any drift > 10% as informational, not blocking.

### 7.4 (Optional) Sprint 35 probe corpus rerun
```bash
cd eval_interactive && uv run eval-interactive run --path case_specs/probe/option_beta_coverage/
```
Sanity-check that the matrix observations are stable across reruns (per-cell AMBIGUOUS/ROUTED routing is deterministic Java; should be byte-identical). LLM-side `active_use_case` mid-loop reclassification may vary by LLM variance.

## 8. M1 close decisions to verify

The deliver-agent + human Sprint 35 close (2026-05-17) reached two key decisions; verify both are captured in the cumulative state:

1. **(a) §7.2 fold-back considered and dropped** — `docs/current/iteration_governance.md` §7.2 stays at UC-A↔UC-C as the principled teaching example. The constitution-discipline review concluded: governance teaches principles; current-implementation limits belong in the optimization backlog (`action_bank.md`).
2. **(b) `R-loosen-topic-uc-binding-llm-owned-drift` opened** in `docs/action_bank.md` §5.2 as a **constraint-removal / architecture-alignment** R-item, NOT a new Java component. Verify the R-item entry's "do NOT do" list explicitly rejects: (1) new live `AlternateUseCaseSurveyor` deterministic component, (2) per-UC if-else / regex / enum-expansion rules for cross-topic drift, (3) collapse of `topic_subject` from the runtime entirely. M2-D candidate.

## 9. Deferred / non-blocking observations

- `R-llm-provider-latency-drift-2026-05-16` is still proposed in `docs/action_bank.md` (Sprint 31-surfaced); M1 explicitly does NOT consume. If you observe latency widening in §7 reruns, surface as informational evidence for the R-item, not as an M1 blocker.
- M2-D candidate (Topic↔UC binding loosening, consumes `R-loosen-topic-uc-binding-llm-owned-drift`) is a NEW M2 candidate alongside M2-A / M2-B / M2-C named in M1 plan §11. Selection happens after M1 close. M2-D would be a semantic-touching milestone with `UseCaseRouter` edits — substantively different shape from M1 (which avoided semantic-routing surfaces by design). Surface as informational; do NOT pre-decide M2.
- Sprint 33 OQ1-OQ5, Sprint 34 OQ1-OQ5 are deferred to M2 / docs fold-backs; none block M1 close.
- Sprint 36 (conditional INTAKE-locked reroute trigger investigation per M1 §3) **deferred** — the conditional trigger ("Sprint 33+34 do NOT sufficiently close Alice") did NOT fire. Sprint 35 itself observes one related concern in §7 OQ3 (Delete cross-topic mid-loop reclassification) but flags it as a M2 lifecycle / Salesforce / handover-scope concern, not M1 close-blocker.
- Sprint 35 §7 OQ4 (backend `/v1/demo/sessions/<sid>/trace` endpoint content-negotiation quirk with `Accept: application/json` header) is a backend infrastructure observation; not M1 scope.

## 10. Output format (write to `docs/codex-findings.md`)

Replace the file content with the standard §4.2 sprint-close header — adapted to milestone-shared shape:

```
## Milestone Review Decision
milestone: M1 — DISCOVER + Intake
commit_range: c9edb37..eb65e2b
sub_sprints_reviewed: Sprint 33, Sprint 34, Sprint 35
decision: pass | fix_required | out_of_scope_review
blocking_count: <number>
summary: <one paragraph>

## Review Evidence
<bullet list — review scope, commit range, what you re-ran, what passed>

## Blocking Findings (if any)
<numbered list; each entry quotes diff snippet + cited file:line; attribute to which sub-sprint>

## Anti-Hardcode Kernel — Sprint 33 (prompt_projection)
<nine-question walk; each Q with one-line verdict>

## Anti-Hardcode Kernel — Sprint 34 (skill_state)
<nine-question walk; each Q with one-line verdict>

## Anti-Hardcode Kernel — Sprint 35 (eval_spec)
<nine-question walk; each Q with one-line verdict>

## Hard-Fence Verification (per sub-sprint + milestone-level)
<the §5 checks; pass/fail per fence per sub-sprint>

## Schema And Reproducibility Checks
<the §6 checks; cited recipes re-run, output noted>

## Validation Runs
<the §7 results; commands + output; Alice bad-case trace evidence per §7.2>

## M1 Close Decisions Verified
<the §8 items; per-decision verdict>

## Deferred / Non-Blocking Notes
<the §9 items>
```

## 11. Expected verdict shape

If all gates pass: **`decision: pass / blocking_count: 0`**. The cleanest outcome for a multi-sub-sprint milestone that ships per its sub-sprint contracts is a single-pass close.

If §2 scope-discipline fails on any sub-sprint: **`decision: fix_required`** with the violating diff snippet quoted as Finding #1, attributed to the sub-sprint commit.

If §7.2 fold-back was inadvertently committed despite the constitution-discipline drop: **`decision: fix_required`** with the diff snippet quoted (this is the §2.3 / §2.4 fence violation; revert required).

If Alice bad-case rerun FAILS closure-criterion (all three sub-bullets fail): **`decision: fix_required`** with trace evidence quoted. M1 cannot close on a failing primary gate.

If a substance concern is found that's NOT in scope for M1 (e.g., a Sprint 33 deferred OQ that surfaces as a new finding requiring runtime work): **`decision: out_of_scope_review`** with the concern named, the deferral rationale cited, and the suggested M2 R-item if applicable.

## 12. Self-check before submitting

- [ ] §2 scope-discipline gate walked per sub-sprint (three passes) + milestone-level.
- [ ] §3 §4.1 nine-question kernel walked three times (once per sub-sprint).
- [ ] §4 §1.7 + §1.6 checks walked.
- [ ] §5 hard-fence verification — cumulative range diff scanned for forbidden surfaces.
- [ ] §6 schema validation + reproducibility re-run.
- [ ] §7.1 Java baseline re-run from clean checkout.
- [ ] §7.2 Alice bad-case rerun executed; closure-criterion verdict (PASS/FAIL) documented with trace evidence.
- [ ] §8 M1 close decisions verified against the cumulative state (action_bank + iteration_governance + agent-memory file).
- [ ] §9 deferred items noted as non-blocking.
- [ ] `docs/codex-findings.md` written per §10 milestone-shared format.
- [ ] Verdict per §11 expected shape.
