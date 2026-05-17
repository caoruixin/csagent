# Current Handoff

Date: 2026-05-17
Branch: `refactor/remove-the-shackles`

## 1. Current phase

Current phase:
**Milestone M2 — Skill Registry Abstraction + Wholesale Retroactive Externalization (LLM-led, Policy-bounded)** (NEW M2 framing; **supersedes OLD M2-Skill mid-flight 2026-05-17** per human direction — OLD framing's minimum-surface incrementalism preserved the scattered Zhang-Sanfeng pattern that M2 was supposed to fix; NEW M2 promotes Skill to first-class abstraction with externalized YAML/JSON definitions + SkillRegistry + retroactive migration of ALL 6 phase content + Sprint 23/31/33 teaching paragraphs + Sprint 6/7/11 predicates). **State: NEW M2 milestone_objective drafted + approved 2026-05-17; NEW Sprint 37 design freeze contract drafted + approved 2026-05-17; deliver-agent housekeeping bundle staged (OLD M2-Skill archived at `docs/milestones/M2-Skill_objective.md` status superseded; OLD Sprint 36 freeze doc at `docs/proposals/skill_foundation_design.md` superseded in-place via supersede pattern; action_bank updated). Pending: human commit of deliver-agent bundle, then Sprint 37 dev session launch (Claude Code via `compact/sprint-037-dev-prompt.md`).** `docs/milestone_objective.md` carries NEW M2 contract (5 sub-sprints: S37 design freeze → S38 SkillRegistry core + 4 simpler phase Skills → S39 RESOLVE_FAQ + RESOLVE_INTAKE + Sprint 6/7/11 predicate migration + S1/S2 new predicates + unified dispatcher → S40 teaching extraction from system_prompt.txt → S41 UC switch + state preservation). `docs/sprint_objective.md` carries NEW Sprint 37 = Skill Registry + state-across-Skill design freeze contract (docs-only, NO server/ code; produces docs/proposals/skill_registry_design.md with 10 design decisions a-j locked; PROSPECTIVE §7 stanza covering Sprints 38/39/40/41; Codex per-sub-sprint review at S37 close per §4.3 trigger #1 Tier-0 candidate + #2 §1.7 boundary discussion). **NEW M2 acceptance bar recalibrated 2026-05-17 per human direction**: bad-case suite (Alice) + interactive eval composite_score are OBSERVATION only (NOT hard gate) — architecture-focused milestone, eval unstable, single-case sparsity, Skill design itself should be flexible rather than gated on specific case PASS; primary gate = functional review + Java tests + Sprint 37 freeze decisions honored across implementation sub-sprints (S38-S41). **M2-Skill scope (per `docs/milestone_objective.md`):** 5 sub-sprints across two tracks — Track A (anchor, 4 sub-sprints): **Sprint 36** Skill foundation + UC-switching continuity design freeze (`eval_spec` / governance per §3.2 Q6; CLOSED PASS 2026-05-17, commit `ed71031`; produced `docs/proposals/skill_foundation_design.md` ~750 lines / 8 sections / 6 sub-decisions D1-D6 + §4.1 walk + Tier-0 candidate write-ups) + **Sprint 37** S1 (`Resolve.FAQ.GroundedAnswer`) implementation (`prompt_projection` + `semantic_planner` + Runtime grounding floor per §1.4; NEW citation predicate in `RecordOutcomeTool` adjacent to existing Sprint 11 §M1 `shouldRejectPrematureResolveOutcome` per Sprint 36 §1.3 material finding; predicate scope BOUNDED per verbatim human authorization §6 #4 — RESOLVE_FAQ + `record_outcome(class=resolve)` + `source_id` citation present; anchored on Alice multi-trace closure-criterion (a) PASS with zero fabrication-condition triggers) + **Sprint 38** S2 (`Resolve.Intake.CollectAndHandover`) implementation (MUCH LIGHTER than original Sprint 5 F2 framing per Sprint 36 §1.3 material finding — predicate logic ALREADY ships as Sprint 7 §I2 `shouldRejectIncompleteIntakeHandover`; Sprint 38 ships envelope teaching in `system_prompt.txt` + optional reframing) + **Sprint 39** UC-switching wide continuity state implementation (`skill_state` + `prompt_projection`; 5×5 invariant matrix per Sprint 36 D5 freeze — principle-level + registry-driven intersection via `IntakeFieldsRegistry.canonicalFieldName` + `requiredFieldsFor`, NOT per-UC-pair branch; introduces NEW `prior_use_case_carry` soft-signal projection slot per Sprint 31/33 precedent); Track C (parallel, 1 sub-sprint): **Sprint 40** per-LLM-call latency characterization (consumes `R-llm-provider-latency-drift-2026-05-16`; pure `infra`; unchanged from prior M2 draft). **M2-Skill hard fences (per `docs/milestone_objective.md` §6, 19 items):** §6 #1 no per-UC-branch if-else in any skill body (YAML, Java, or prompt) per Constitution §1.7; §6 #2 Skill terminal predicate is Java guard ONLY for Runtime-owned floor per §1.4 (grounding-citation, capability, safety, PII, idempotency); §6 #3 Skill recommended order is soft prompt guidance, NOT hard enforcement; §6 #4 HARD FENCE INVERSION on `D-hard-citation-gate` (bounded Skill-bounded exception per verbatim human authorization 2026-05-17; predicate fires ONLY on RESOLVE_FAQ + `record_outcome(class=resolve)` + `source_id` citation presence; generic gate deferral preserved); §6 #5 no `RuntimeIntentClassifier.java` / `IntentClassification.java` / `DriftResult.java` / `DriftDetector.java` / `UseCaseRouter.java` / `ClassifyUseCaseTool.java` touch (M3-D `R-loosen-topic-uc-binding-llm-owned-drift` deferred to M3+); §6 #6 no edit to `PhaseEvaluator.INTAKE_UCS` (M3-B `D-single-handover-orchestrator` deferred to M3 unless Salesforce cutover calendar pressure); §6 #7 no `escalation_reason` enum widening at `PhaseEvaluator.java:39-63` (`D-new-escalation-reason-enum` deferral preserved; M3-A sibling rationale/confidence fields deferred to M3); §6 #8 no Tier-0 invariant without explicit human-review escalation (Sprint 36 surfaced two Tier-0 candidates OQ 7.1 + 7.2; both DEFER confirmed by deliver-agent + human + Codex; re-evaluate at M2 close); §6 #9-#10 no edits to existing case families / shadow case families (cascade fence carried from M1); §6 #12 no `iteration_governance.md` edit during M2 (constitution-discipline preserved per `feedback_constitution_discipline_vs_planning_anticipation.md`); §6 #17 no mocked-LLM as primary evidence per `feedback_mocked_llm_cannot_prove_prompt_causal_change.md`; §6 #18 no Skill prescribes LLM customer-facing language (LLM-owned per §1.3); §6 #19 no Skill hard-encodes per-step argument values (LLM-owned per §1.3). **M2-Skill acceptance bar:** Alice bad-case closure-criterion (a) PASS across at least 3 fresh real-LLM traces with **zero fabrication-condition triggers** via Sprint 37 S1 grounding-floor predicate enforcement (LLM cannot persist `record_outcome(class=resolve)` on a fabricated answer because the citation check fails). **Codex review plan:** per-sub-sprint for all semantic-touching sub-sprints (36 done, 37/38/39 upcoming) + milestone-shared cumulative architectural-posture check at M2 close. Sprint 36 per-sub-sprint Codex review fired per §4.3 trigger #1 (Tier-0 candidates surfaced; DEFER confirmed) + trigger #2 (§1.7 boundary discussion on hybrid framing); verdict `pass / 0` on first pass. **Sprint 36 close residual-risk note (Codex §10 deferred / non-blocking):** S1 predicate is minimum citation-presence floor only; intentionally does NOT judge citation relevance or factual content quality. If Sprint 37 traces still fabricate while carrying a `source_id`, that's a new Sprint 37/M2-close evidence item for human/Codex review, NOT a reason to broaden Sprint 36's frozen scope silently.

---

Preceding milestone:
**Milestone M1 — DISCOVER + Intake** (closed PASS 2026-05-17 per `docs/milestones/M1_objective.md` §12 closure verdict; archive). Classification **A-with-Codex-finding-OOSR-classification + deliver-agent-finding-2-fix-in-close** (first milestone-shared close under the §8 framework, 2026-05-16 governance upgrade
to milestone framework per `iteration_governance.md` §8 — 3-5
coordinated sub-sprints per milestone, milestone-shared Codex
review at close, smoke composite_score demoted to observation per
§5.5, curated bad-case suite at `eval_interactive/case_specs/bad_cases/`
as new primary acceptance gate). M1 shipped three sub-sprints
(Sprint 33 `prompt_projection` commit `8a22aa6`; Sprint 34
`skill_state` commit `e532f0d`; Sprint 35 `eval_spec` commit
`eb65e2b`) across cumulative range `c9edb37..eb65e2b`. Java baseline
preserved 932 → 983 (+51 new tests; UC-K regression guard preserved
through M1). Codex milestone-shared review (`docs/sprints/M1-codex-review.md`)
returned `fix_required / blocking_count: 2`. Deliver-agent + human
classification at M1 close 2026-05-17: **Finding 1** (Alice
grounding fabrication on Codex independent rerun; layer =
`semantic_planner` per Codex triage) reclassified as
**`out_of_scope_review`** per M1 §6 hard fence #1 (semantic_planner
work is M1-out-of-scope by construction); opened new R-item
`R-grounding-discipline-iterative-search-fabrication` as
high-priority M2 anchor. **Finding 2** (Sprint 35 matrix §5/§6
stale vs close decisions) resolved at M1 close via deliver-agent
commit appending "Close decision update (2026-05-17)" addendum to
`docs/diagnostics/option_beta_coverage_matrix.md` per Decision
B-2a. **Constitution-discipline §7.2 revert** at Sprint 35 close —
planning-anticipated §7.2 worked-example re-anchor was *considered
and dropped* (governance teaches principles; current-implementation
limits belong in the optimization backlog); captured in
`.claude/agent-memory/sprint-deliver-orchestrator/feedback_constitution_discipline_vs_planning_anticipation.md`.
Sprint 36 (conditional INTAKE-locked reroute investigation per M1
§3 row) **deferred** — the conditional trigger ("Sprint 33+34 do
NOT sufficiently close Alice") did NOT fire. **M2 candidate
selection** per Decision C 2026-05-17 cross-validated by two
parallel research-agents: M2 = M2-A ∪ M2-E.option-3 (grounding
discipline + escalation honesty + customer-honesty); M2-C
parallel-track instrumentation; M2-B orchestrator → M3 unless
Salesforce cutover calendar pressure surfaces; M2-D Topic↔UC
binding loosening → deferred to M3+. M1 final classification:
**A-with-Codex-finding-OOSR-classification + deliver-agent-finding-2-fix-in-close**
(first milestone-shared close under the §8 framework; first
instance of this milestone-close classification pattern). M1
objective archived at `docs/milestones/M1_objective.md` (with §12
closure verdict appended); §6.5 closed-milestone index row added
to `docs/action_bank.md`.

---

Preceding sub-sprint:
**Sprint 36 (OLD M2-Skill sub-sprint 1) — Skill foundation design freeze** closed PASS 2026-05-17 (commit `ed71031`; Codex per-sub-sprint review `decision: pass / blocking_count: 0` on first pass). Produced `docs/proposals/skill_foundation_design.md` (~1100 lines / 8 sections / 6 sub-decisions D1-D6 + §4.1 walk + Tier-0 candidate write-ups) under minimum-surface incrementalism framing (reuse PhasePlan + new system_prompt.txt teaching paragraph + new predicate adjacent to existing shouldRejectXxx family). **OLD M2-Skill (which Sprint 36 shipped under) was wholesale superseded mid-flight on 2026-05-17 per human direction** — the minimum-surface freeze preserved the scattered Zhang-Sanfeng pattern that NEW M2 was supposed to fix. Sprint 36 commit + archive stay immutable per `doc_governance.md` sprint-archive rule; the Sprint 36 freeze doc (`docs/proposals/skill_foundation_design.md`) is superseded in-place via supersede pattern (status: superseded + superseded_by: `docs/proposals/skill_registry_design.md` to be authored by NEW Sprint 37 dev); the OLD M2-Skill milestone_objective is archived at `docs/milestones/M2-Skill_objective.md` (status: superseded; supersession-mid-flight). §6 #4 verbatim human authorization on S1 `must_cite_source` bounded inversion of `D-hard-citation-gate` CARRIED FORWARD into NEW M2 §6 #4. Sprint 36 §1.3 MATERIAL FINDING about Sprint 11 §M1 `shouldRejectPrematureResolveOutcome` precedent informs NEW Sprint 37 design decision (g) on predicate migration mapping. Full handoff: `docs/sprints/sprint-036-handoff.md` (53.4K, 12 sections); Codex archive: `docs/sprints/sprint-036-codex-review.md`.

---

Earlier sprint:
Sprint 32 (alternate_candidate_use_cases case family — Sprint 31
OQ4 deferral implementation, single-track semantic-touching sprint,
layer `eval_spec` per §3.2 Q6) closed on **2026-05-16** as
**PASS — A-with-investigation-finding** (first instance of this
pattern on the `eval_spec` layer; Sprint 29 in-flight downgrade
precedent applied). Dev commit `c9edb37` shipped 10+ files: source
brief at `docs/diagnostics/failure-briefs/sprint32-uc-a-uc-c-alternate-uc-readiness.md`
+ 5 visible CaseSpecs at `eval_interactive/case_specs/case_families/sprint32_alternate_uc/`
+ 2 shadow CaseSpecs at `eval_interactive/case_specs_shadow/case_families/sprint32_alternate_uc/`
(per `_ACCESS_BOUNDARY.md:91` convention) + 1 local manifest + 2
manifest appends + dev handoff. Java baseline preserved
(917 / 1-inherited / 0 / 2; zero Sprint 32 Java change). 14-case
smoke rerun showed no production-code-attributable regression.
**Codex sprint-close returned `fix_required / blocking_count: 2`**
at commit range `8d3e73b..c9edb37`. Deliver-agent + human classified
Finding 1 (shadow path scope mismatch) as **`out_of_scope_review`**
— deliver-agent objective `docs/sprint_objective.md` had wrong
shadow path; dev correctly followed `_ACCESS_BOUNDARY.md:91`;
deliver-agent corrected objective post-facto in close cycle.
Finding 2 (target case observed alternates lack UC-C; neighbor #2
ROUTED not AMBIGUOUS — Sprint 30 Option β coverage prediction of
§7.2 worked-example shape empirically falsified) classified as
**in-flight downgrade to investigation finding**, new R-item
`R-option-beta-coverage-gap-uc-a-uc-c-shape` registered (`eval_spec`
/ `prompt_projection`; proposed; consumed by M1 sub-sprint 2 or 3).
Positive findings tucked inside Finding 2: neighbor #1 DID exercise
populated-slot shape (alternates contained UC-FP); both negative
cases observed populated alternates AND LLM stayed in active UC —
validates §1.7 non-enforcement at LLM-behaviour level (Sprint 31
fix-iteration #2 T8 proved at runtime level; Sprint 32 negatives
extend the proof to LLM-behavior level). Full handoff: `docs/sprints/sprint-032-handoff.md`
(13 sections including the §13 findings-classification narrative);
Codex archive: `docs/sprints/sprint-032-codex-review.md`.

---

Earlier earlier sprint:
Sprint 31 (alternate_candidate_use_cases projection slot — Option β
implementation, single-track semantic-touching sprint, layer
`prompt_projection` per `iteration_governance.md` §3.2 Q3, §7 stanza
filled) closed on **2026-05-16** as **PASS — chained close (path A
+ fix-iteration #2)**
(deliver-agent + human applied; full closure rationale in
`docs/sprints/sprint-031-handoff.md` §12). Two commits on
`refactor/remove-the-shackles`: `2c1fd41` (Sprint 31 dev ship) and
`de47635` (§13 fix-iteration append). Sprint 31 ships the runtime
implementation of the Option β design frozen at
`docs/proposals/alternate_uc_signal_data_source_design.md`:
captures the existing `RoutingResult.AMBIGUOUS` candidate list that
`SessionManager.java:185–189` previously **discarded** (now
preserved), persists it on a new
`BotSession.intakeAmbiguousCandidates: String[]` field with the
Flyway V14 migration at
`server/src/main/resources/db/migration/V14__intake_ambiguous_candidates.sql`,
projects per-turn as `alternate_candidate_use_cases` (minus the
active UC) adjacent to the existing `candidate_use_cases` slot at
`ContextProjectionBuilder.java:380–415`, plus a sibling teaching
paragraph in `server/src/main/resources/prompts/system_prompt.txt`
adjacent to the `already_called` paragraph (lines 23–28). Plus two
new regression-test files: 9-test
`server/src/test/java/com/gumtree/csagent/service/runtime/IntakeAmbiguousCandidatesProjectionTest.java`
(mirror of Sprint 20 `AlreadyCalledProjectionTest`) + 1-test
non-enforcement integration test at
`server/src/test/java/com/gumtree/csagent/integration/AgentRunLoopAlternateCandidateUseCasesNonEnforcementIntegrationTest.java`
(mirror of Sprint 20 `AgentRunLoopAlreadyCalledNonEnforcementIntegrationTest`).
All five OQ1–OQ5 pre-picks adhered to (OQ1 = null on ROUTED;
OQ2 = sibling teaching placement; OQ3 = normal fold-back cadence;
OQ4 = case-family authoring deferred to Sprint 31+1; OQ5 = no
sequencing dependency with `R-uc-cdf-get-customer-context-bot-actual-usage`).
Java test bar: 10/10 new Sprint 31 tests PASS; full server suite
912 / 1-inherited / 0 / 2 (zero new regressions; the inherited
`SystemPromptUserRequestedTiebreakerTest` failure persists from
the Sprint 24-era unauthored `system_prompt.txt:60` mod
unchanged). New slot observable on **4 AMBIGUOUS-intake cases**
(`cs_interactive_015 / 040 / 176 / 192`) carrying non-empty
`alternate_candidate_use_cases` arrays in rerun #1's
`per_turn_trace[].projection` at
`eval_interactive/results/20260516-024934/results.json`. §10 smoke
acceptance bar shows a composite/outcome/judge floor regression vs
Sprint 28 reference (`eval_interactive/results/20260514-181257/results.json`);
the §13 fix iteration (three smoke reruns across rerun #1 / #2 / #3
on differing bot instances) disambiguated this as **external LLM
provider drift** (mean elapsed_ms +84% vs Sprint 28 reference with
zero Sprint 31 latency-config / model / retry change). Both
falsifiable internal hypotheses — cold-start race (§7.1(a) per
dev handoff) and system_prompt teaching paragraph (§13.3) — were
REJECTED by the fix iteration. Codex sprint-close review COMPLETED:
**initial verdict `fix_required / blocking_count: 2`** at
`8908775` (Finding 1 smoke regression, Finding 2 T8 too weak);
deliver-agent + human classified Finding 1 as `out_of_scope_review`
(carried by `R-llm-provider-latency-drift-2026-05-16` per
Constitution §1.6) and Finding 2 as `fix_required (targeted P1)`.
**Fix-iteration #2** at commit **`8d3e73b`** strengthened T8 from 1
happy-path scenario to 6 parameterised variants × 5 invariance bars;
dedicated handoff at `docs/sprints/sprint-031-fix-handoff.md`. **Codex
re-review verdict `pass / blocking_count: 0`** at `8908775..8d3e73b`
(Finding 2 closed; Finding 1 confirmed OOSR carry). Full chronological
review archive at `docs/sprints/sprint-031-codex-review.md`. Final
classification: **A-with-fix-iteration-investigation +
A-with-fix-iteration-#2 (chained close; first instance of the pattern)**.
Follow-on R-items in `docs/action_bank.md`:
**`R-llm-provider-latency-drift-2026-05-16`** (`infra` /
observability — characterize the +84% latency widening per §13.7;
deferred behind Sprint 31+1 per 2026-05-16 planning pick) and
**Sprint 31+1 case-family-authoring sprint** (`eval_spec` — picked
as next current sprint per OQ4 pre-pick + 2026-05-16 deliver-agent +
human decision).

---

Earlier sprint:
Sprint 30 (Alternate-UC signal data-source design — investigation-
only docs-only architectural-design sprint, single deliverable: one
new design proposal doc at `docs/proposals/alternate_uc_signal_data_source_design.md`
plus one R-item registration `R-alternate-uc-signal-data-source` at
`docs/action_bank.md`) closed on **2026-05-16**. Classification
**A-with-Codex-skipped** — third instance of the pattern (Sprint 26
first, Sprint 27 second). Codex intentionally skipped per §4.1
docs-only exemption (human-applied 2026-05-16); no
`docs/sprints/sprint-030-codex-review.md` archive exists (intentional).
The §4.1 verdict that would have been returned is `approve
(exemption: docs-only design freeze; no semantic surface touched)`.
The Sprint 30 dev session re-verified all 5 §4 premise items from
the Sprint 30 objective at HEAD `df8b8cd` with no drift, surfaced
the load-bearing Sprint 31 premise (the AMBIGUOUS-branch discard
at `SessionManager.java:185–189`), evaluated 5 candidate options
(α through ε), and recommended **Option β** with explicit
Constitution citation. Sprint 31 inherited the design freeze
verbatim via its `docs/sprint_objective.md` §6 file table and
shipped the runtime change on commit `2c1fd41`. Full handoff:
`docs/sprints/sprint-030-handoff.md` + design freeze:
`docs/proposals/alternate_uc_signal_data_source_design.md`.

---

Earlier sprint:
Sprint 29 (R-prompt-phase-plan-directive-followship (R2) probe
follow-on — two-track semantic-touching sprint with `eval_spec`
Track A CaseSpec authoring + `infra` Track B (c) phase-derivation
verification, in-flight downgrade clause on Track B per
`feedback_corpus_undecidable_premise_check.md`) closed on
**2026-05-15** with the in-flight downgrade clause firing (Track B
Q2 = NO, no `CONFIRM → CLOSE` turn pair observable in Track A's
authored corpus). Track A landed 4 hand-authored CaseSpecs under
`eval_interactive/case_specs/case_families/sprint29_directive_probe/`
exercising D485.2 / D564.7 / D616.2 preconditions; smoke ran at
`eval_interactive/results/20260514-225419/results.json`. D564.7
followed (1/1); D616.2 violated (1/1; `cs29d616_uc_c` escalated
with `user_distress` instead of producing a grounded RESOLVE
answer); D485.2 undecidable on Track A's corpus (user-simulator
`goal_achieved` early-stop at `eval_interactive/eval_interactive/simulator/session_runner.py:224`
pre-empts the bot's CONFIRM phase plan + a `record_outcome` infra
error on `cs29d485_uc_c`). Track B's in-flight downgrade named (but
did not open) `R-per-turn-phase-transition-dump-for-smoke-harness`
as a proposed follow-on R-item. `R-prompt-phase-plan-directive-followship`
at `docs/action_bank.md:450` was updated with the Sprint 29 finding
(D616.2 n=1 violation; (R1) structural-sprint trigger requires
n ≥ 2 confirmed non-fulfillments and Sprint 29 contributes 1) but
**NOT closed**. Sprint 29 ships zero `server/` / `eval_interactive/`
infra code edits; zero `system_prompt.txt` edits; zero edits to
existing Sprint 20 case families. Closure verdict pending Codex
review per Sprint 29 sprint-close convention. Full handoff:
`docs/sprints/sprint-029-handoff.md`.

---

Earlier sprint:
Sprint 28 (Per-case trace dump for smoke harness — single-track,
single-layer `infra` / eval-harness bundle shipping
`R-per-case-trace-dump-for-smoke-harness` per
`docs/action_bank.md:448`) closed on **2026-05-15** with Codex
sprint-close review verdict **`decision: pass / blocking_count: 0`**
on the first pass (no fix iteration required). Classification:
**A — Clean close** (cleanest A in the run since Sprint 24; no
packaging-rollforward needed, no Codex-skip applied).

Option B (writer-side eval-harness enrichment) chosen per Sprint 25
precedent. The planning-turn premise check established (and the dev
session re-verified) that all four R-item-named fields are already
hydrated into `TraceCollector.collect(session_id)`'s `TurnTrace`
records from the bot's `/v1/demo/sessions/{id}/trace` endpoint; the
eval-harness read them but did not serialise them into
`results.json`. The dev edit: a new
`_build_per_turn_trace(trace_data)` static helper in
`eval_interactive/eval_interactive/batch/executor.py` (+62 LOC) plus
one new additive list field `per_turn_trace` emitted on each of the
four `case_result` writer paths (`_build_case_result` success-path +
`_timeout_result` / `_error_result` / `_contract_violation_result`
placeholder paths, each with `[]` for schema uniformity). New
regression test file
`eval_interactive/tests/test_executor_per_turn_trace_enrichment.py`
(7 tests, all pass) mirrors Sprint 25's 8-test
`test_executor_llm_calls_enrichment.py` shape: populated path /
empty trace / missing `phase_plan` in projection / non-dict /
non-list defensive fallbacks / backwards-compat byte-identity /
JSON-serialisability / all three placeholder paths. Smoke rerun on
2026-05-15 (`eval_interactive/results/20260514-181257/results.json`,
14 cases) — 12 cases populate `per_turn_trace` with ≥ 1 entry; 2
cases hit the defensive `[]` branch by design (`cs_interactive_001`
bot-500 path, `cs_interactive_259` CONTRACT_VIOLATION path; both
expected and covered by tests). Mean case-level `elapsed_ms` =
26437.64 ms vs Sprint 25 reference 26139.43 ms vs Sprint 26 close
28846.64 ms — Sprint 28's run sits inside the run-to-run variance
band, no overhead regression.

**All four R-item-named axes shipped**:

- `tool_calls` per bot turn → new `per_turn_trace[].tool_calls`
- `phase_plan` per bot turn → new `per_turn_trace[].phase_plan`
- `projection` per bot turn (including `intake_state`) → new
  `per_turn_trace[].projection`
- `LlmCallEvents` at case level → preserved via Sprint 25's existing
  `case_results[].llm_calls[]` (NOT re-shipped this sprint to avoid
  the opportunistic-field-duplication anti-pattern)

**`R-per-case-trace-dump-for-smoke-harness`** at
`docs/action_bank.md:448` is **CLOSED** by this Sprint 28 close
commit. The disposition note records the closure with file-axis
mapping; §6 closed-action index appends the Sprint 28 row.

**This sprint unblocks Sprint 27's (R2) follow-on probe.**
`R-prompt-phase-plan-directive-followship` at
`docs/action_bank.md:450` had a gating dependency on this R-item
(named three times in Sprint 27 handoff §6, §10, and §11); that
gating is now removed. The directive-followship workstream
(promoted on n=3 in Sprint 19 from cs_259 + manual-probe + cs_011
T2 evidence; probed in Sprint 27 with recommendation (R2) targeted
probe sprint) can now run with per-turn `phase_plan` + per-turn
`projection.intake_state` evidence on authored CaseSpecs. The line
450 R-item disposition is updated to reflect the gating removal but
remains open (awaiting the (R2) probe sprint).

Codex review-pass evidence (`docs/sprints/sprint-028-codex-review.md`):
§4.1 Anti-Hardcode verdict `approve` (exemption: pure infra /
eval-harness writer-side serialization, no semantic surface
touched). All 5 handoff reproducibility recipes re-extracted by
Codex and returned the cited values verbatim. Backwards-compat
verified: `added=['per_turn_trace']`, `removed=[]`; Sprint 25
`llm_calls[]` 13-key shape preserved. Option B-only verified: no
new bot endpoint, no new HTTP client method, no Java persistence,
no Flyway migration, no DB schema change. Opportunistic-field
check passed (only 3 R-item-named per-turn fields shipped;
`LlmCallEvents` not duplicated). Test runs: new Sprint 28 tests
`7 passed`; full Python `306 passed, 3 failed` (3 inherited from
`test_case_spec_overrides.py` × 2 + `test_corpus_lint.py` × 1 per
Sprint 25 §11); full Java `902 / 1 / 0 / 2` (1 inherited
`SystemPromptUserRequestedTiebreakerTest` from the unauthored
`system_prompt.txt` working-tree mod, same as Sprints 24/25/26/27).

Sprint 28 handoff §7 surfaces a **proposed but NOT opened**
follow-on R-item: `R-per-turn-phase-transition-dump-for-smoke-harness`
— would extend `per_turn_trace[]` with `phase_before`, `phase_after`,
`turn_index` so the Sprint 27 (R2) consumer's transition-confirmation
step on D485.2 / D564.7 / D616.2 preconditions can read phase deltas
without re-deriving them. **Named only by the dev, not opened**;
human decides at next-sprint planning whether to (a) open it
standalone, (b) fold it into the Sprint 27 (R2) probe sprint as a
prerequisite, or (c) defer.

Files committed in the single Sprint 28 dev commit `ca55d8e`
("sprint 28: per-case trace dump surfaces into results.json",
2026-05-15):

- `eval_interactive/eval_interactive/batch/executor.py` (modified,
  +62 LOC) — `_build_per_turn_trace` helper + 4 writer-path
  emissions.
- `eval_interactive/tests/test_executor_per_turn_trace_enrichment.py`
  (NEW, 7 tests, all pass).
- `docs/sprints/sprint-028-handoff.md` (NEW, 12-section archive
  including the closure-verdict §12 placeholder authored by the dev
  with explicit-delegation language per
  `.claude/agent-memory/sprint-deliver-orchestrator/feedback_handoff_verdict_section_delegation.md`).
- `docs/10-handoff.md` (parent-dev-commit modification to set Sprint
  28 as "Current phase" and demote Sprint 27 to "Previous phase";
  this close commit then refreshes the §1 lead to the close
  summary above and demotes Sprint 27 to "Preceding sprint" full
  paragraph).

Files added at close commit (deliver-agent close-out, not part of
the dev's commit):

- `docs/sprints/sprint-028-objective.md` — archive copy of the
  running `docs/sprint_objective.md` carrying the Sprint 28
  objective. Deliver-agent-owned and untracked at session start;
  plain `mv` (no `git mv` HEAD-content risk per
  `.claude/agent-memory/sprint-deliver-orchestrator/feedback_git_mv_uses_head_content.md`
  because the source was never tracked). Front-matter updated at
  close (`doc_tier: sprint-archive`, `status: archived`,
  `implementation_status: historical`, `review_cadence: ad hoc`);
  the running file is removed by this close commit.
- `docs/sprints/sprint-028-codex-review.md` — archive copy of
  `docs/codex-findings.md` (untracked at top level prior to this
  commit; staged as new file + deletion of the top-level file per
  `.claude/agent-memory/sprint-deliver-orchestrator/feedback_packaging_codex_findings_supersession.md` —
  delete+add supersession, not a rename).
- `docs/action_bank.md` — line 448 `R-per-case-trace-dump-for-smoke-harness`
  status flipped to `done (Sprint 28)` with disposition note;
  line 450 `R-prompt-phase-plan-directive-followship` disposition
  appended with the gating-removed note; §6 closed action index
  appended Sprint 28 row.
- `compact/sprint-028-dev-prompt.md`,
  `compact/sprint-028-review-prompt.md` — deliver-agent-owned
  planning prompts authored during this sprint; rolled forward in
  this close commit per
  `.claude/agent-memory/sprint-deliver-orchestrator/feedback_commit_at_end_bundles_deliver_artefacts.md`.

This sprint is `docs/current/iteration_governance.md` §7
stanza-**REQUIRED** with target layer `infra` per Sprint 28
objective §6 (the archived `docs/sprints/sprint-028-objective.md`
§6 carries the stanza verbatim: target layer `infra`,
"This sprint adds no Tier-0 invariant", "No semantic hardcode
introduced", generalization coverage = 14 target / 0 neighbor
(eval-harness writer is single-surface) / 14 negative (all 14
cases must preserve existing fields byte-identical) / 0 shadow
(deferred — shadow set is the G2 case-family deliverable not yet
existent)). The §4.1 Anti-Hardcode kernel ran externally; Codex
returned `approve` with the exemption note.

The natural follow-on sprint candidate per Sprint 28's R-item
closure + Sprint 27's (R2) recommendation is now the
**Sprint 27 (R2) targeted probe sprint** — author N≥3 CaseSpecs
exercising D485.2 (CLOSE record_outcome-if-not-already-recorded) /
D564.7 (INTAKE intake-complete handover with structured args) /
D616.2 (RESOLVE FAQ premature-escalation prohibition) preconditions
using Sprint 28's per-turn `phase_plan` + `projection.intake_state`
trace evidence. The directive-followship workstream has now chased
this evidence path for 9 sprints (Sprint 18 G1 → Sprint 19
3-instance promotion → Sprint 27 probe + (R2) recommendation →
Sprint 28 trace dump unblocks → Sprint 29 (R2) probe), and letting
the workstream complete is high-value. Alternative Sprint 29 picks
on the action_bank §5.2 backlog include: open
`R-per-turn-phase-transition-dump-for-smoke-harness` (handoff §7
proposal — small `infra` increment that benefits the (R2) consumer,
could be folded into the (R2) sprint as prerequisite OR run
standalone); `R-uc-k-intake-complete-case-id-binding` (Sprint 19
§3.6; cs_066 UC-K state-loss; `skill_state` layer; deterministic
bug fix, not gated on anything; standing UX-leverage reframe
alignment); `R-uc-cdf-get-customer-context-bot-actual-usage`
(Sprint 22-surfaced behavioural question, investigation-only);
`R-handover-orchestrator-write-side` (Sprint 16 design freeze;
`docs/release_gate.md` §1.1 P1-pre-cutover blocker — awaits cutover
trigger). The Sprint 25 §7 open questions (Q2 / Q3 / Q4), the
Sprint 26 §7 conditional `R-pre-post-f2d4cb2-pass-rate-isolation`
opening, the Sprint 27 §11 Q1–Q5 governance questions, and the
Sprint 28 §7 follow-on R-item proposal all continue to require
human direction to open.

Pattern captured in deliver-agent memory: this is the cleanest
first-pass A close in the run since Sprint 24. The Sprint 28
review-pass was reinforced by the writer-side enrichment pattern
established in Sprint 25 (Option B; mirroring exactly), the
explicit-delegation handoff §12 placeholder convention from
Sprint 22 (per
`.claude/agent-memory/sprint-deliver-orchestrator/feedback_handoff_verdict_section_delegation.md`),
and the reproducibility-recipe-as-handoff hygiene introduced by
Sprint 25's fix iteration (per
`.claude/agent-memory/sprint-deliver-orchestrator/feedback_deliver_agent_cited_numbers_must_be_reproducible.md`).

---

Preceding sprint:
Sprint 27 (PhasePlan directive-shape probe — investigation-only
docs-only probe sprint, single deliverable: a 12-section decision
handoff) closed on 2026-05-15 (commit `8a7703a`) as the probe
sprint triggered by the planning-turn premise check on
`R-prompt-phase-plan-directive-followship`
(`docs/action_bank.md:450`, Sprint 19 close 3-instance promotion).
Premise check found detection-axis fragmentation across the three
cited instances (cs_259 / manual-probe / cs_011 T2): only the
manual-probe UC-A RESOLVE instance is fully event-shape detectable;
cs_259 has a content-shape precondition; cs_011 T2's directive
source is bot-content (not `phase_plan.systemInstruction`). Per
the `docs/sprints/sprint-018-handoff.md` §8.8 conditional-broadening
rule (n=1 evidence insufficient to ship structural action),
Sprint 27 did NOT ship a slot, did NOT restructure `PhasePlan`,
did NOT author probe scenarios — Sprint 27 enumerated the six
`PhaseEvaluator.plan(...)` `.systemInstruction(...)` string
literals (lines 411 / 458 / 485 / 517 / 564 / 616), classified the
21 identified directives under a 4-question rubric (precondition
shape / action shape / source / turn scope), surveyed two existing
reference smoke runs (`eval_interactive/results/20260514-111724/results.json`
Sprint 25 reference + `…/20260514-114628/results.json` Sprint 26
close), and produced an evidence table + recommendation from a
closed set (R1 / R2 / R3). Findings: 4 of 21 directives classify
as `target` (event-shape precondition AND event-shape action AND
`phase_plan` source) — D485.2 (CLOSE record_outcome-if-not-
already-recorded), D517.2 (ESCALATE tautology recap), D564.7
(INTAKE intake-complete handover), D616.2 (RESOLVE FAQ premature-
escalation prohibition); the remaining 17 are `content-shape-defer`.
Across both reference smokes the LLM emits ZERO `request_handover`
and ZERO `record_outcome` tool calls (138 chat calls combined);
`observed_triggers=0` and `observed_non_fulfillments=0` for every
target row (§9.8 valid-finding: insufficient detection capability
+ insufficient case coverage). (R1) requires n≥2 target directives
with observed non-fulfillment; evidence is 0. (R3) requires
structural incompatibility; population IS structurally compatible
(4 of 21 target-shape). **Recommendation: (R2) targeted probe
sprint follow-on** — author N≥3 CaseSpecs exercising D485.2 /
D564.7 / D616.2 preconditions (skipping D517.2 tautology). The
(R2) follow-on was **gated on `R-per-case-trace-dump-for-smoke-
harness` (`docs/action_bank.md:448`) landing first** — Sprint 28
above closes that gating. **`R-prompt-phase-plan-directive-
followship` at `docs/action_bank.md:450` was UPDATED with the
probe finding but NOT closed**; disposition becomes "open, awaiting
(R2) probe sprint with confirmed observed non-fulfillment ≥ 2".
Sprint 27 also names — but does NOT open — 6 follow-on R-items
(handoff §7: `R-discover-weak-candidate-cue-soft-signal`,
`R-discover-classify-or-clarify-projection`,
`R-confirm-sentiment-projection`,
`R-close-phase-completion-checklist-projection`,
`R-intake-instruction-decomposition`,
`R-resolve-faq-terminal-sequence-projection`). Open governance
question surfaced (handoff §11 Q2): does
`phase_plan.systemInstruction` text containing user-message-shape
detection (D411.3 / D411.4) constitute a §1.7 boundary case
(prompt-side soft-matching vs code-side keyword/regex)? Sprint 27
ships ZERO code, config, prompt, eval-spec, judge, CaseSpec,
override, Tier-0 edit. Single deliverable: 12-section
`docs/sprints/sprint-027-handoff.md`. Closure verdict: PASS
(Codex review intentionally skipped per §4.1 exemption clause for
docs-only investigation/probe sprints, human-applied 2026-05-15;
the §4.1 verdict that would have been returned is `approve
(exemption: docs-only investigation/probe sprint)`).
Classification **A-with-Codex-skipped** — second instance of the
pattern (Sprint 26 was the first). Pattern captured in deliver-
agent memory at
`.claude/agent-memory/sprint-deliver-orchestrator/feedback_close_with_codex_skipped_docs_only_outcome.md`
+ the probe-sprint-shape memory at
`.claude/agent-memory/sprint-deliver-orchestrator/feedback_probe_sprint_shape_for_conditional_broadening.md`.
No `docs/sprints/sprint-027-codex-review.md` archive exists
(intentional). Full handoff: `docs/sprints/sprint-027-handoff.md`.

Earlier sprint (Codex skipped, docs-only outcome — first instance):
Sprint 26 (Latency decision sprint — consumes Sprint 25's per-LLM-
call latency instrumentation) closed on 2026-05-15 (commit
`1f4a1db`) as the investigation+decision sprint that walked the
(A) widen deadline budget / (B) revert pre-`f2d4cb2` model /
(C) accept current latency / (D) change retry-backoff / (E) other
option space against the Sprint 25 reference smoke. Decision:
**(C) Accept current latency.** Classification:
**A-with-Codex-skipped (new variant — first instance)** — Codex
review intentionally skipped per `iteration_governance.md` §4.1
exemption clause for docs-only PRs, human-applied 2026-05-15. The
Sprint 26 dev session re-verified all seven Sprint 26 objective
§3 premise checks (handoff §3; all hold), extracted the decision-
relevant per-call data from the Sprint 25 reference smoke (243/243
LLM-call successes / 0 deadline exceedance / max chat latency
11.547s vs 30s budget), walked the three-hypothesis attribution of
the separate 4/14 pass-rate signal (latency REFUTED; model-quality
PLAUSIBLE-but-CONFOUNDED with concurrent prompt / corpus /
override changes; run-to-run variance PARTIAL), and rejected (A) /
(B) / (D) / (E). Surfaced ONE follow-on conditional R-item:
`R-pre-post-f2d4cb2-pass-rate-isolation` recorded at `§5.2` as
`proposed; deferred` per human direction 2026-05-15. Sprint 26
shipped zero code / config / instrumentation / Sprint 24-landed
or Sprint 25-landed code touched / eval-spec / case-family /
prompt / Tier-0 / rubric edit. Full handoff:
`docs/sprints/sprint-026-handoff.md`.

Earlier sprint (Codex fix re-review pass, single-track infra/eval-harness):
Sprint 25 (Per-LLM-call latency instrumentation — single-track
`R-per-llm-call-latency-instrumentation`, `infra` / eval-harness)
closed on 2026-05-14 after one narrow handoff-edit-only fix
iteration on parent handoff's reproducibility hygiene.
Classification: **A — Clean close** (Codex fix re-review `pass /
blocking_count: 0` against fix commit `c8b8c85`). Track A —
Option B (writer-side eval-harness enrichment) chosen: surfaces
existing `LlmCallLogger`-emitted timing into each `case_results[]`
entry on `results.json` via new `llm_calls` field populated from
`GET /v1/demo/sessions/{id}/llm-calls`. Parent commit `1b54b14`;
worked-example pre/post-`f2d4cb2` chat p95 8.4s → 11.7s
(+3.3s widening). Fix iteration was handoff-edit-only (commit
`c8b8c85`): replaced `ARRAY[…]` placeholders with literal `jq`
extractors + executable mean-computation. Sprint 25 ships zero
decision-action; the latency decision belongs to Sprint 26. The
writer-side enrichment pattern Sprint 25 established is the
precedent Sprint 28 above mirrored exactly. Full handoff:
`docs/sprints/sprint-025-handoff.md`. Fix Codex archive:
`docs/sprints/sprint-025-fix-codex-review.md`. Lessons in
`.claude/agent-memory/sprint-deliver-orchestrator/feedback_deliver_agent_cited_numbers_must_be_reproducible.md`.

Earlier sprint (Codex pass, no fix iteration required):
Sprint 24 (Slow-LLM Placeholder Coalesce + Coarse Latency Proxy —
two-track, semantic-touching on Track A; investigation-only on
Track B) closed clean on 2026-05-14 with Codex `decision: pass,
blocking_count: 0` on the first review pass. Track A delivered the
deterministic UX repair on the cross-turn slow-LLM placeholder-loop
surface: new `consecutive_deadline_count` `@Column` +
`@Builder.Default = 0` `Integer` field on `BotSession`; `SessionManager`
builder init `.consecutiveDeadlineCount(0)`; `PhaseEvaluator` reset
hook in the outcome-dispatch prologue; split `DEADLINE_EXCEEDED` /
`LLM_UNAVAILABLE` case-block with threshold-gated honest next-step
emission on the second consecutive deadline; single Flyway V13
migration; behaviour-level regression suite. Trigger is the
event-shape count of consecutive `DEADLINE_EXCEEDED` outcomes, not a
regex / keyword / if-else on user content; runtime owns timeout
fallback emission deterministically (§1.4). Track B documented the
honest coarse-proxy latency baseline and proposed
`R-per-llm-call-latency-instrumentation` (Sprint 25 implemented).
Single dev commit `e21b1b6`. Full handoff:
`docs/sprints/sprint-024-handoff.md`.

Earlier sprint (Codex fix re-review pass, PASS branch):
Sprint 23 (repeated FAQ calls + LLM stall root-cause investigation —
two-track investigation+bundle, semantic-touching) closed on
2026-05-14 after one strict-evidence-gate fix iteration. Parent
dev commit `39cb1b9` landed the `already_called` teaching paragraph
in `server/src/main/resources/prompts/system_prompt.txt`; fix
commit `19ce2ae` ran a real-LLM cs_040 target rerun confirming the
duplicate-`search_knowledge` shape reversed. Track B
investigation-only — proximate cause confirmed; narrow UX-repair
shape proposed as `R-slow-llm-placeholder-coalesce-honest-next-step`
(Sprint 24 landed). Mocked-LLM hard fence honored. Full handoff:
`docs/sprints/sprint-023-handoff.md`.

Earlier sprint (docs-only governance, Codex pass):
Sprint 22 (phase 2 line 358 reconciliation + R-item closure) closed
on 2026-05-14 as a narrow docs-only scope-correction sprint:
reconciled the cross-UC `get_customer_context` policy misread
Sprint 21 carried forward (phase 2 §2.10.1 line 1098 already
permits the tool for UC-C / UC-D / UC-F), closed
`R-generator-get-customer-context-policy-mismatch` and the
routed-to `R-phase2-uc-cdf-customer-context-policy-widen` as
premise-invalidated, and opened the residual behavioural R-item
`R-uc-cdf-get-customer-context-bot-actual-usage`. No runtime,
prompt, CaseSpec, override, judge, FAQ corpus, or case-family
change. Full handoff: `docs/sprints/sprint-022-handoff.md`.

Earlier sprint (single-track eval_spec, Codex A-with-evidence-gap-acknowledgment close):
Sprint 21 (Wave A5/A6 L3 Review Batch — per-case + 1 systematic)
closed on 2026-05-14 as a single-track semantic-touching sprint on
the eval_spec surface. Three approved overrides written to
`eval_interactive/case_spec_overrides.yaml` (cs_001
escalation_trigger flip + bot_handling_pattern rewrite; cs_192
secondary_ucs dedupe; cs_095 UC-A → UC-D classification flip,
supersedes Wave A2.1 legacy entry); two rejected (cs_176
reclassified eval_spec → `semantic_planner`; systematic
`R-generator-get-customer-context-policy-mismatch` reclassified
eval_spec → `product_policy` — Sprint 22 later reframed as
premise-invalid); two deferred (cs_038, cs_040) on missing
override-schema scoring extension. Codex fix re-review returned
`fix_required, blocking_count: 3` on typographical-fidelity grounds
(5-character markdown/punctuation drops on quote blocks); the
human accepted as close-eligible (A-with-evidence-gap-acknowledgment
classification — distinct from Sprint 20's A-with-packaging-note
and from B). No remediation against runtime, prompt, CaseSpec
content, judge, or governance surfaces. Full handoff:
`docs/sprints/sprint-021-handoff.md`.

Earlier sprint (G2 case-family + already_called soft signal, Codex A-with-packaging-note close):
Sprint 20 (G2 Interactive Case Family + Shadow Split + Already-Called
Soft Signal — parallel A + B) closed on 2026-05-13 after a narrow
fix iteration. Track A delivered 10 case families × (≥1 target +
≥2 neighbor + ≥2 negative + ≥2 shadow) = 70 CaseSpec-shaped
entries under `eval_interactive/case_specs/case_families/` and
`eval_interactive/case_specs_shadow/` plus the v0 shadow-split
mechanism (directory boundary + custom-path-only loading +
documented self-restraint). Track B delivered the `already_called:
[{tool, arguments_hash, at_step}]` projection slot in
`ContextProjectionBuilder.build(...)` (observability-only, runtime
non-enforcement). Fix iteration closed Codex's two original
findings (runtime non-enforcement test at `AgentRunLoopImpl.run`
granularity + `_ACCESS_BOUNDARY.md` reconciliation); fix
re-review classified as A-with-packaging-note (substantive
findings closed cleanly per Codex's own evidence; the single
blocker was deliver-agent-owned files bundled into the dev's fix
commit). Full handoff: `docs/sprints/sprint-020-handoff.md`.

Earlier sprint (Codex pass):
Sprint 19 (Smoke Regression Investigation + Orchestrator Tool-Call
De-Dup — parallel A + B) walked `docs/current/iteration_governance.md`
§3.2 per case for the two R-items the Sprint 18 G1 backlog named
as blocking G2: `R-smoke-regression-investigation` (Track A) and
`R-runtime-orchestrator-tool-call-deduplication` (Track B). Outcome
was proposal-only on both tracks: Track A found five of six regressed
cases share a slow-LLM placeholder shape (layer `infra` per §3.2
Q1; deferred remediation as `R-slow-llm-placeholder-coalesce`),
cs_011 lands at `semantic_planner` (failure to escalate on explicit
handover request), cs_066 at `skill_state` (UC-K case_id-binding
loss). Track B re-classified the orchestrator-de-dup R-item from
`infra` to `semantic_planner` and proposed two follow-on items:
`R-prompt-projection-already-called-soft-signal` (read-side soft
signal — delivered by Sprint 20 Track B) and
`R-idempotent-read-tool-short-circuit` (conditional). Full handoff:
`docs/sprints/sprint-019-handoff.md`.

Earlier sprint (no Codex run):
Sprint 18 (Human-led Failure Portfolio — G1) filed 10 representative
real failures as Failure Briefs under
`docs/diagnostics/failure-briefs/` per the Sprint 17 §2 template and
recorded 18 G1-surfaced R-items + 2 open observations under
`docs/action_bank.md` §5.2. No runtime, prompt, FAQ corpus, CaseSpec,
judge, eval harness, test, or script change. G1 declared the §7
stanza **exempt** under the docs-only governance exemption. Full
handoff: `docs/sprints/sprint-018-handoff.md`. Archive:
`docs/sprints/sprint-018-g1-failure-portfolio-objective.md`.

Earlier sprint (Codex pass):
Sprint 17 (Iteration Governance Lite — G0) landed the docs-only
governance bundle (`docs/current/iteration_governance.md` 7 sections
+ `AGENTS.md` constitution chain + `docs/action_bank.md` §5.1
governance-track backlog); Codex `decision: pass, blocking_count: 0`;
archive under `docs/sprints/sprint-017-*`.

Earlier accepted sprint:
Sprint 16 (Handover Exactly-Once Contract and Repro) landed as a
docs + characterization-test sprint; no runtime behaviour change.
Defined the handover exactly-once contract in
`docs/proposals/handover_orchestrator_design.md`, added
`Sprint16HandoverDualPathReproTest`, and opened the
`docs/release_gate.md` §1.1 blocking rule + the **Single Handover
Orchestrator** action item.


## 2. Sprint 14 goal

Sprint 14 upgrades FAQ / KB grounding trustworthiness by making the
knowledge source chain, published safety, canonical URL availability,
and citation observability explicit — without introducing a broad hard
runtime citation gate, a new skill runtime framework, broad routing
rewrites, or additional mechanical escalation.

Scope is exactly the three Sprint 14 actions L0 / L1 / L2.

## 3. Sprint 14 implementation

### L0 — KB canonical URL / Help URL / published safety audit + fix

Code changes:

- `server/src/main/java/com/gumtree/csagent/repository/KbChunkRepository.java`
  — added `findNearestByEmbeddingPublishedOnly` and
  `findNearestByEmbeddingWithUcTagsPublishedOnly` queries that join
  `kb_articles` with `is_published = true`. Existing legacy methods
  preserved for backward compatibility.
- `server/src/main/java/com/gumtree/csagent/service/knowledge/KnowledgeSearchService.java`
  — routes through the new published-only ANN methods so unpublished
  candidates never enter the rerank pipeline. Added defense-in-depth
  hit-projection filter that drops any post-fetch unpublished article
  even if the ANN returned it via a stale cache. Stamps
  `canonical_url_missing` per hit.
- `server/src/main/java/com/gumtree/csagent/model/KnowledgeHit.java`
  — added `canonicalUrlMissing` boolean (Jackson `canonical_url_missing`).
- `server/src/main/java/com/gumtree/csagent/service/tools/SearchKnowledgeTool.java`
  — projects `hits[*].canonical_url_missing` into the agent-visible
  response so missing-URL data-quality gaps are observable.
- `server/src/main/java/com/gumtree/csagent/service/tools/ResolveArticleTool.java`
  — refuses unpublished articles with deterministic
  `article_unpublished_safe_refuse` reject reason
  (`UNPUBLISHED_REJECT_REASON` constant). Exposes `canonical_url`
  alongside legacy `source_url`. Stamps `canonical_url_missing` and
  `safe_to_show` (= published AND non-blank body).
- `server/src/main/java/com/gumtree/csagent/service/knowledge/KnowledgeIngestionRunner.java`
  — extracted `buildKbArticleFromJson(...)` package-private helper so
  the FAQ → KbArticle field-preservation contract (including
  `source_url` and `is_published`) is unit-testable.
- `scripts/build_knowledge_base.py` — additive: `mapping_summary_report.md`
  now carries a "URL & Published-Safety Coverage" block enumerating
  total / published / unpublished / with-canonical-URL / missing-URL /
  unsafe-to-show counts. JSON content unchanged.

QA report: `qa-reports/faq-kb-lineage-and-url-audit.md`.

Tests added (5 focused tests of the form Sprint 14 §L0 prescribed):

- `Sprint14KnowledgeIngestionCanonicalUrlTest`
  (4 cases — preservation of `Help_Site_URL__c` → `source_url`, respects
  explicit `published_status=false`, missing URL surfaces null,
  CSV-mapping fallback for `uc_tags`).
- `Sprint14KnowledgeSearchPublishedFilterTest`
  (4 cases — UC-scoped path routes through published-only ANN, no-tag
  path same, post-rerank projection drops still-unpublished article,
  missing-URL hit stamps `canonical_url_missing=true`).
- `Sprint14ResolveArticleSafetyTest`
  (4 cases — refuses unpublished with canonical reject reason,
  exposes `canonical_url` mirroring `search_knowledge`, missing URL
  classified as observable DQ gap, blank body marks `safe_to_show=false`).

### L1 — Separate retrieved / resolved / cited source evidence

Code:

- `server/src/main/java/com/gumtree/csagent/service/knowledge/CitationExtractor.java`
  (new) — passive citation extractor. Detects Salesforce KA `source_id`
  pattern, canonical URL pattern, and (conservatively) title fuzzy
  match against retrieved/resolved candidates. Stateless;
  side-effect-free.
- `server/src/main/java/com/gumtree/csagent/service/knowledge/SourceEvidenceLineage.java`
  (new) — record `(retrievedSourceIds, resolvedSourceIds,
  citedSourceIds, citedCanonicalUrls, retrievedCanonicalUrls)` with
  `fromToolEvents(toolEvents, botResponse)` constructor walking
  `search_knowledge` (retrieved) and `resolve_article` (resolved) tool
  events. §L0 contract: a refused unpublished resolve is NOT counted
  as resolved — the article remains in the
  `retrievedButUnresolvedSourceIds` set.
- `server/src/main/java/com/gumtree/csagent/model/BotSession.java`
  — added 4 new `@Transient` slots: `retrievedSourceIds`,
  `resolvedSourceIds`, `citedSourceIds`, `citedCanonicalUrls`. Schema
  unchanged; `bot_turns.source_ids` column preserved verbatim.
- `server/src/main/java/com/gumtree/csagent/service/runtime/ControlKernel.java`
  — `recordRunResult(...)` now stamps the new transient slots via the
  new `stampFaqGroundingObservability(...)` helper. The existing
  `bot_turns.source_ids` write path is untouched. Stamping is wrapped
  in try/catch so any unexpected pattern in tool events / bot text
  cannot break record-turn persistence.

Tests:

- `Sprint14SourceEvidenceLineageTest` (7 cases):
  retrieved-only does not imply cited, resolved evidence tracked
  separately, source_id mention detected, URL mention detected and
  mapped back to candidate, third-party URL recorded as free-form
  citation, missing citation observable but non-blocking, refused
  unpublished resolve excluded from resolved set.

### L2 — FAQ grounding contract + soft diagnostics

Docs:

- `docs/current/faq_grounding_contract.md` (new, normative) — defines the
  output class taxonomy, the §L1 evidence lineage construction, the
  §L2 diagnostic state transition table, and the non-blocking
  guarantee. Records future narrow Sprint 16 §S1 hardening candidates
  surfaced by the §L0 audit (`R-faq-grounded-resolve-bypass`,
  `R-cited-but-unresolved`, `R-resolved-but-uncited-rate`,
  `R-canonical-url-missing-rate`).

Code:

- `FaqOutputClass` enum — six classes: `factual_answer`,
  `clarification`, `empathy_ack`, `handover`, `tool_status`,
  `intake_collection`. Only `factual_answer` requires grounding.
- `FaqOutputClassifier` — heuristic classifier with
  `ClassifierContext(intakeUseCase, handoverDispatched)` so the
  runtime's `handoverDispatched` flag overrides any wording-based
  guess and intake UCs prefer `intake_collection` over
  `clarification` on a question shape.
- `FaqGroundingDiagnostics` — record `(outputClass, faqGroundingState,
  citationPresent, citationMatch, citationDrift, resolvedButUncited,
  retrievedButUnresolved)`. State table: `factual_grounded`,
  `factual_uncited`, `factual_unresolved`, `factual_unretrieved`,
  `non_factual`, `unknown`.
- `BotSession` — added 7 transient slots: `faqOutputClass`,
  `faqGroundingState`, `citationPresent`, `citationMatch`,
  `citationDrift`, `resolvedButUncited`, `retrievedButUnresolved`.
- `ControlKernel.stampFaqGroundingObservability(...)` — single call
  from `recordRunResult` populates §L1 + §L2 slots; observability-only
  with no rejection / rewrite / loop.

Tests:

- `Sprint14FaqGroundingDiagnosticsTest` (14 cases):
  taxonomy exemption check (only factual requires grounding),
  classifier coverage for each shape (clarification / empathy /
  handover-flag override / tool_status / intake / factual default), and
  the full diagnostic state table (`factual_grounded`,
  `factual_uncited`, `factual_unresolved`, `factual_unretrieved`,
  `citation_drift`, all five non-factual classes skip grounding,
  missing citation is observable not blocking).

### Audit findings (factual-answer bypass — Sprint 16 candidate)

Per Sprint 14 §L2, if the audit found a current factual-answer bypass
of search/resolve, document it as a future narrow Sprint 16 candidate.
The audit recorded **R-faq-grounded-resolve-bypass** in
`docs/current/faq_grounding_contract.md` §6: the existing Sprint 6 §G2 guard
refuses the `request_handover(faq_miss_threshold_exceeded)` shape, but
does NOT refuse a FINAL_ANSWER shape that paraphrases an unresolved
hit. This is observable today as `retrieved_but_unresolved=true` on a
factual-answer turn. Sprint 14 explicitly does NOT close it — the
guidance is for a future narrow Sprint 16 §S1 hardening if real-traffic
evidence motivates it.

## 4. Files changed (Sprint 14)

Production code:

- `server/src/main/java/com/gumtree/csagent/repository/KbChunkRepository.java`
- `server/src/main/java/com/gumtree/csagent/service/knowledge/KnowledgeSearchService.java`
- `server/src/main/java/com/gumtree/csagent/service/knowledge/KnowledgeIngestionRunner.java`
- `server/src/main/java/com/gumtree/csagent/service/knowledge/CitationExtractor.java` (new)
- `server/src/main/java/com/gumtree/csagent/service/knowledge/SourceEvidenceLineage.java` (new)
- `server/src/main/java/com/gumtree/csagent/service/knowledge/FaqOutputClass.java` (new)
- `server/src/main/java/com/gumtree/csagent/service/knowledge/FaqOutputClassifier.java` (new)
- `server/src/main/java/com/gumtree/csagent/service/knowledge/FaqGroundingDiagnostics.java` (new)
- `server/src/main/java/com/gumtree/csagent/service/runtime/ControlKernel.java`
  (added §L1/§L2 stamping helper; preserved all existing logic)
- `server/src/main/java/com/gumtree/csagent/service/tools/SearchKnowledgeTool.java`
- `server/src/main/java/com/gumtree/csagent/service/tools/ResolveArticleTool.java`
- `server/src/main/java/com/gumtree/csagent/model/KnowledgeHit.java`
- `server/src/main/java/com/gumtree/csagent/model/BotSession.java`
  (added 11 `@Transient` slots; no schema change)

Tests (new):

- `Sprint14KnowledgeIngestionCanonicalUrlTest` (4 cases)
- `Sprint14KnowledgeSearchPublishedFilterTest` (4 cases)
- `Sprint14ResolveArticleSafetyTest` (4 cases)
- `Sprint14SourceEvidenceLineageTest` (7 cases)
- `Sprint14FaqGroundingDiagnosticsTest` (14 cases)

Docs:

- `docs/current/faq_grounding_contract.md` (new, normative)
- `qa-reports/faq-kb-lineage-and-url-audit.md` (new — §L0 audit + repair)
- `docs/10-handoff.md` (this file)
- `docs/action_bank.md` (Sprint 14 row added)
- `docs/sprint_objective.md` retained — already contains the Sprint 14
  objective.

Scripts:

- `scripts/build_knowledge_base.py` — additive URL & Published-Safety
  Coverage block in the auto-generated mapping report.

No FAQ corpus content, CaseSpec, judge, broad routing taxonomy,
handover payload contract, Salesforce backend contract, full Issue
Ledger, all-UC task taxonomy, escalation enum, or system prompt was
touched.

No DB schema migration. The `bot_turns.source_ids` column is
preserved verbatim; new lineage / diagnostics fields are
session-transient observability material consumed via projection /
trace evidence.

## 5. Tests run

- `mvn -pl server test -Dtest='Sprint14*'`
  → **33 / 0 / 0 / 0** (Sprint 14 §L0/§L1/§L2 focused tests).
- `mvn -pl server test -Dtest='Sprint10*Test,Sprint11*Test,
  Sprint12*Test,Sprint13*Test,PhaseEvaluatorPlanTest,Cs014*,
  Cs066*,Cs095*,Cs002*,Cs029*,Cs176*,Cs001*,EscalationReason*Test,
  Sprint6*,Sprint7*Test,Sprint8*Test,Sprint9*Test'`
  → **269 / 0 / 0 / 0** (named Sprint 14 regression guards).
- `mvn -pl server test`
  → **854 / 0 / 0 / 0** at the Sprint 14 milestone (was 821
  pre-Sprint-14; +33 new Sprint 14 §L0/§L1/§L2 tests). Post Sprint
  14.1 closure the suite is **859 / 0 / 0 / 0** (+5 trace-persistence
  tests; see §15).
- `pytest eval_interactive/tests/`
  → **294 / 0** (full Python eval test suite).
- Smoke runs were NOT executed for Sprint 14. Sprint 14 introduces no
  FAQ corpus content change, no judge change, no CaseSpec change, no
  prompt change, no escalation enum change, no routing taxonomy
  change, and no eval-output schema change. The current canonical
  baseline (`docs/current_eval_baseline.md`) is preserved
  (post-Sprint-8 r1 / r2 runs).

## 6. KB URL / published audit result

| Metric | Value |
|--------|-------|
| Total articles | 218 |
| Articles published (`published_status=true`) | 218 |
| Articles unpublished (`published_status=false`) | 0 |
| Articles with `source_url` (canonical URL) | 180 |
| Articles missing `source_url` (canonical-URL DQ gap) | 38 |
| Articles unsafe to show (unpublished OR empty body) | 0 |

The 38 articles missing a canonical URL come from rows the Salesforce
export shipped without `Help_Site_URL__c`. They remain searchable but
the §L0 fix surfaces the gap as `canonical_url_missing=true` on every
search-hit and resolve-article response so a reviewer can classify the
case as a corpus-side curation task rather than a runtime fix. The
broader curation work is out of Sprint 14 scope and queued for the Eval
Governance / corpus-audit owner alongside `G-FAQ-corpus-answerability`.

Per `qa-reports/faq-kb-lineage-and-url-audit.md` §3, nine concrete §L0
fixes landed this sprint (published-safety filter at SQL layer + at hit
projection, refusal of unpublished `resolve_article`, exposure of
`canonical_url` alongside `source_url`, observable
`canonical_url_missing` flag on both tools, and the `safe_to_show`
conjunctive predicate on `resolve_article`).

## 7. Retrieved / resolved / cited evidence contract

Sprint 14 §L1 splits the historically-overloaded `sourceIds` concept
into four observably-distinct dimensions, all populated each turn and
durably persisted on `bot_turns.projected_context.faq_grounding`
(Sprint 14.1 closure) and mirrored onto in-memory `BotSession`
transient slots:

- `retrievedSourceIds` — IDs from successful `search_knowledge` events
  (post §L0 published-safety filter). De-duplicated; insertion order
  preserved.
- `resolvedSourceIds` — IDs that successfully passed through
  `resolve_article`. A refused unpublished resolve is NOT counted.
- `citedSourceIds` — IDs detected in the customer-visible reply by
  the passive `CitationExtractor` (source_id mention, URL mention
  mapped back to candidate, conservative title match).
- `citedCanonicalUrls` — URLs in the reply that did NOT correspond to
  any candidate `canonical_url`. Typically third-party (e.g. gov.uk).

The `bot_turns.source_ids` column is preserved verbatim as the
backward-compatible aggregate. Sprint 14 §L1 explicitly does NOT use
the diff between the four dimensions to gate / rewrite / loop the bot
response. The output is observability material — see
`docs/current/faq_grounding_contract.md` §3 for the canonical contract.

## 8. Soft diagnostic fields

Sprint 14 §L2 surfaces seven additional fields per turn under
`bot_turns.projected_context.faq_grounding` (Sprint 14.1 closure;
`docs/current/faq_grounding_contract.md` §4) and mirrors the same values onto
`BotSession` transient slots for in-process readers:

- `faqOutputClass` — taxonomy token (`factual_answer`, `clarification`,
  `empathy_ack`, `handover`, `tool_status`, `intake_collection`).
- `faqGroundingState` — coarse state (`factual_grounded`,
  `factual_uncited`, `factual_unresolved`, `factual_unretrieved`,
  `non_factual`, `unknown`).
- `citationPresent`, `citationMatch`, `citationDrift` — citation
  observability triplet.
- `resolvedButUncited`, `retrievedButUnresolved` — evidence-diff
  observability pair.

All seven are observability-only. Sprint 14 §L2 does NOT block, rewrite,
re-loop, or escalate based on any of them. The §G2 FAQ-grounded-resolve
guard from Sprint 6 remains the only enforced runtime check on this
surface.

## 9. Regression guard outcomes

All Sprint 14 active guards pass:

- `L1:escalation_reason_consistency` = **0** (never re-introduced).
- `CONTRACT_VIOLATION:active_use_case` = **0**.
- Existing FAQ S1 guard tests remain green
  (`AgentRunLoopS1FaqGroundedResolveGuardTest`).
- Sprint 6 §G0 ReadTimeout closure intact
  (`test_agent_client_session_create_timeout.py`).
- cs014 remains UC-C
  (`Cs014RouteAndLoopHandoverIntegrationTest`,
  `Cs014RouteAndDistressRegressionTest`).
- cs066 remains UC-K.
- cs095 remains UC-A / not UC-K / not UC-FP.
- cs002 distress reconciliation remains green.
- cs029 remains UC-D + `user_requested`.
- cs176 explicit-human-help → `user_requested` regression remains
  green (`Cs176ExplicitHumanHelpHandoverIntegrationTest`).
- All Sprint 7 / 7.1 / 8 / 8.1 / 8.2 / 9 / 9.1 / 10 / 11 / 11.1 / 12 /
  13 hard invariants remain green
  (named regression suite: 269 / 0).
- `RuntimeIntentClassifier` remains runtime-internal.
- `bot_turns.source_ids` write path unchanged.
- `bot_turns` / `bot_sessions` / `kb_articles` / `kb_chunks` schemas
  unchanged.

New Sprint 14 §L0 / §L1 / §L2 guards (33 deterministic JUnit tests):

- §L0 — `Sprint14KnowledgeIngestionCanonicalUrlTest`,
  `Sprint14KnowledgeSearchPublishedFilterTest`,
  `Sprint14ResolveArticleSafetyTest`.
- §L1 — `Sprint14SourceEvidenceLineageTest`.
- §L2 — `Sprint14FaqGroundingDiagnosticsTest`.

## 10. Remaining risks

**No new P0 / P1 blockers opened by Sprint 14.** The change is additive
observability + a narrow §L0 published-safety / canonical-URL fix; no
runtime main-flow architecture file was modified.

Sprint 14 §L0 audit surfaced four narrow follow-ups (recorded in
`docs/current/faq_grounding_contract.md` §6 — all deferred):

- **R-faq-grounded-resolve-bypass** — factual answers paraphrasing
  retrieved-but-unresolved hits. Observable today as
  `retrieved_but_unresolved=true` on a factual-answer turn. Future
  narrow Sprint 16 §S1 hardening candidate IF real-traffic shows
  reproducible cases.
- **R-cited-but-unresolved** — `citation_drift=true` with cited
  source_id never retrieved/resolved. Hallucination signal candidate;
  Eval Governance owns until reproduced.
- **R-resolved-but-uncited-rate** — corpus-level rate of uncited
  factual answers. Eval Governance.
- **R-canonical-url-missing-rate** — 38 articles missing
  `Help_Site_URL__c`. Corpus curation, not runtime.

Residuals carried forward from Sprint 13 §8 (unchanged):

- `R-cs015-description-keyword`, `R-cs176-UC-I-drift`,
  `R-S3-no-prior-search-guard`, `R-S5-Tier2-runtime-guard`,
  `R-stall-detector-calibration`, `R-L3-relevance-tone`,
  `R-FAQ-corpus-answerability`, `R-advert-link-product-decision`,
  `R-rerank-fallback-diagnostics`, `R-task-type-token-naming`,
  `R-record-outcome-loop`, `R-clean-baseline-promote`,
  `R-full-issue-ledger`, `R-skill-runtime-framework`,
  `R-prompt-risk-signal-handling`.

The current canonical eval baseline remains the post-Sprint-8 r1 / r2
runs documented in `docs/current_eval_baseline.md`. Sprint 14
explicitly does NOT promote a new canonical eval baseline.

## 11. Recommendation for Sprint 15 or Sprint 16

Recommended next phase:

**Eval Governance docs sprint** (or equivalent governance-only work)
remains the primary recommendation. Sprint 14 + 14.1 closed the FAQ /
KB evidence lineage and safety workstream (including the persistence
gap Codex flagged) that Sprint 13 §8 / §9 had open; the largest
residual category is still `judge_volatility` / `faq_corpus_gap` /
`product_policy_gap`, all governance-owned.

(The Sprint 14.1 Codex review surfaced **Script / Policy Config
Governance Sprint** as an alternative recommended next phase. Either
phase is reasonable; choose by prioritisation, not by Sprint 14 / 14.1
state.)

Alternative phases ranked:

1. **Eval Governance** — primary recommendation (above).
2. **Narrow Sprint 16 §S1 hardening** — only if real-traffic evidence
   demonstrates `R-faq-grounded-resolve-bypass` reproducibly affects
   answer correctness on a high-traffic UC. The Sprint 14 §L1 / §L2
   diagnostics surface (`retrieved_but_unresolved=true` on a
   `factual_answer` turn) is the trigger to look for. Until then, the
   §G2 prompt-side nudge already handles the dominant case.
3. **Narrow Corpus Curation** — fill the 38 missing
   `Help_Site_URL__c` rows. Owner: Eval Governance / corpus audit.
4. **Re-run validation after infra cleanup** — viable if Kimi /
   smoke credentials are available; runs alongside Eval Governance.
5. **Release Candidate Hardening** — premature; depends on a
   clean canonical baseline that has not yet been promoted.

Do not start another runtime sprint unless triage finds a new
P0 / P1 runtime blocker. Sprint 14 explicitly does NOT promote a new
canonical eval baseline.

## 12. Was Sprint 14 objective met?

Sprint 14 alone was **not** met under its initial Codex review: the
review correctly identified that the §L1 / §L2 diagnostics were stamped
only onto `@Transient` `BotSession` fields after `BotTurn.save(...)`,
so a normal save / reload trace could not observe them. Sprint 14.1
(§15 below) closed that single blocking gap. The Sprint 14.1 closure
review returned `decision: pass, blocking_count: 0`. Sprint 14 + 14.1
together are accepted:

- L0 KB canonical URL / Help URL / published safety audit + fix landed.
  Source chain traced (CSV → build script → JSON → DB → service →
  tools); `Help_Site_URL__c` confirmed preserved; published-safety
  filter added at SQL + projection layer; `resolve_article` refuses
  unpublished with deterministic reject reason; missing canonical URL
  classified as observable DQ gap; QA report
  `qa-reports/faq-kb-lineage-and-url-audit.md` shipped; 12 focused
  tests.
- L1 separate retrieved / resolved / cited source evidence landed.
  `SourceEvidenceLineage` value object splits the historically-
  overloaded `sourceIds` concept; `CitationExtractor` provides passive
  citation extraction (source_id pattern, URL pattern, conservative
  title match); existing `bot_turns.source_ids` write path preserved
  for back-compat; 7 focused tests confirm retrieved-only does not
  imply cited, resolved tracked separately, citation extractor
  patterns work, missing citation observable not blocking.
- L2 FAQ grounding contract + soft diagnostics landed.
  `docs/current/faq_grounding_contract.md` defines the output class taxonomy,
  the §L1 evidence lineage construction, the §L2 diagnostic state
  table, and the non-blocking guarantee. `FaqOutputClass`,
  `FaqOutputClassifier`, `FaqGroundingDiagnostics` services compute
  the seven soft diagnostic fields per turn. 14 focused tests.
- Diagnostics are durably observable on
  `bot_turns.projected_context.faq_grounding` (Sprint 14.1 closure);
  the in-memory `BotSession` transient slots are mirrored from the
  same computed values for in-process readers / tests. No DB schema
  migration; no broad runtime / prompt / routing scope; no hard
  citation gate; existing §G2 guard preserved verbatim.
- Full server suite 859 / 0 (post Sprint 14.1; was 854 pre-closure);
  named Sprint 14 regression suite 269 / 0; full Python eval 294 / 0;
  `L1:escalation_reason_consistency = 0`;
  `CONTRACT_VIOLATION:active_use_case = 0`; cs014 / cs066 / cs095 /
  cs002 / cs029 / cs176 regressions all green.

Out-of-scope items (broad hard citation gate, new skill runtime
framework, broad S1 rewrite, broad routing taxonomy rewrite,
escalation enum changes, CaseSpec churn, FAQ corpus content rewrite,
judge calibration, eval expansion, broad prompt rewrite, mechanical
risk-keyword escalation) were NOT touched.

## 13. Current-doc maintenance rule

`docs/10-handoff.md`, `docs/diagnostics/codex-findings.md`,
`docs/sprint_objective.md`, and `docs/action_bank.md` are
overwrite-current-state files. Before replacing one of them:

1. archive the previous version under `docs/sprints/` if it
   belongs to a sprint closure, or
   `docs/archive/current-docs/` if it is an ad hoc transition;
2. then overwrite the working file;
3. keep only actionable current state in the working file.

Historical detail belongs in `docs/sprints/`,
`docs/archive/current-docs/`, `eval_interactive/results/`, and
`qa-reports/`.

Sprint 13 archives are at `docs/sprints/sprint-013-*`;
Sprint 14 will archive to `docs/sprints/sprint-014-*` on closure.

## 14. Do not reopen

- broad full review
- broad routing rewrite
- judge calibration implementation
- CaseSpec churn
- anchor / exploration / promotion hard-gate expansion
- smoke 14/14 optimization
- S3 no-prior-search guard unless explicitly selected
- S5 Tier-2 runtime guard unless explicitly selected
- broad TraceViewer redesign
- llm_call_log dashboard / per-tool latency dashboard
- search threshold tuning / answer_miss / faq_miss semantic redesign
- tool-deadline guard / bypass-DISCOVER redesign
- advert-link generator / direct listing URL tool
- full Issue Ledger / `issues[]` / per-issue budgets / all-UC task
  taxonomy / full skill runtime framework / handover payload rewrite
- new escalation reason enum value
- runtime sprint unless a new P0 / P1 runtime blocker is found
- broad system prompt rewrite — narrow risk-signal-handling proposal
  in `docs/runtime_freeze_and_risk_policy.md` §6.2 only on Eval
  Governance trigger
- broad FAQ corpus rewrite — narrow corpus curation for the 38
  `Help_Site_URL__c` gap rows is a queued Eval Governance / corpus
  audit task
- hard runtime citation gate — Sprint 14 §L2 explicitly carved this
  out; only consider after real-traffic evidence escalates the
  `citation_drift` / `retrieved_but_unresolved` signals

## 15. Sprint 14.1 closure (FAQ grounding observability persistence)

Status: **closed (Codex pass)**. The Sprint 14.1 closure review
returned `decision: pass, blocking_count: 0`, accepting Sprint 14 +
14.1 as a unit.

The initial Sprint 14 Codex review had returned `decision:
fix_required, blocking_count: 1` against the Sprint 14 diff. The
single blocker was:

> Sprint 14 L1/L2 lineage + grounding diagnostics are computed but not
> durably observable. They are stamped onto `@Transient` `BotSession`
> fields after `BotTurn` is saved, so a normal save/reload trace cannot
> see `retrieved_source_ids`, `resolved_source_ids`, `cited_source_ids`,
> `faq_grounding_state`, `citation_present`, `citation_match`,
> `citation_drift`, `resolved_but_uncited`, `retrieved_but_unresolved`.

### Closure summary

- **Blocker fixed:** the §L1 `SourceEvidenceLineage`, §L2
  `FaqOutputClass`, and §L2 `FaqGroundingDiagnostics` are now computed
  in `ControlKernel.recordRunResult(...)` BEFORE
  `turnRepository.save(turn)`, and the snake_case payload is merged
  into the existing `bot_turns.projected_context` JSONB column under a
  new top-level `faq_grounding` key.
- **Trace surface used:**
  `bot_turns.projected_context.faq_grounding` (JSONB; no migration —
  the column already exists and is opaque JSON).
- **Fields persisted under `faq_grounding`:**
  - `retrieved_source_ids` (array of source_id strings),
  - `resolved_source_ids` (array of source_id strings),
  - `cited_source_ids` (array of source_id strings),
  - `cited_canonical_urls` (array of free-form URLs that did NOT
    correspond to any candidate `canonical_url`),
  - `output_class` (one of `factual_answer`, `clarification`,
    `empathy_ack`, `handover`, `tool_status`, `intake_collection`, or
    `null` when classification was skipped),
  - `faq_grounding_state` (`factual_grounded` / `factual_uncited` /
    `factual_unresolved` / `factual_unretrieved` / `non_factual` /
    `unknown`),
  - `citation_present`, `citation_match`, `citation_drift`,
  - `resolved_but_uncited`, `retrieved_but_unresolved`.
- **Backwards compatibility:** the existing
  `bot_turns.source_ids` `text[]` column is preserved verbatim; its
  write path in `recordRunResult` is unchanged. The `BotSession`
  `@Transient` slots are still populated (mirroring the durable
  values) so any in-memory reader / projection / test that already
  consumes them keeps working.
- **Out of scope (carved out, per Sprint 14.1 task):**
  - no DB migration (uses the existing JSONB column);
  - no hard citation gate (no rejection / rewrite / re-loop on
    missing citation);
  - no new skill runtime framework;
  - no broad S1 hardening;
  - no FAQ corpus / CaseSpec / judge / prompt / routing /
    escalation-enum / risk-policy edit.

### Files changed (Sprint 14.1)

Production code:

- `server/src/main/java/com/gumtree/csagent/service/runtime/ControlKernel.java`
  — `recordRunResult(...)` reordered to compute lineage / output class
  / diagnostics BEFORE saving the turn; new
  `mergeFaqGroundingIntoProjection(...)`,
  `buildFaqGroundingPayload(...)`, and
  `stampFaqGroundingObservabilityFromComputed(...)` helpers replace
  the old post-save `stampFaqGroundingObservability(...)` call site.

Tests added:

- `server/src/test/java/com/gumtree/csagent/integration/Sprint141FaqGroundingTracePersistenceTest.java`
  (5 cases — retrieved-only, resolved-only, cited-by-source-id,
  cited-by-canonical-url, missing-citation-non-blocking).

Docs:

- `docs/10-handoff.md` (this section + corrections to §1, §7, §8,
  §12 — Sprint 14 was NOT met before Codex review).
- `docs/current/faq_grounding_contract.md` §5 — wiring narrative updated to
  name the durable trace surface
  (`bot_turns.projected_context.faq_grounding`).
- `docs/action_bank.md` — Sprint 14 status reflects the §14.1 closure.

### Tests run

- `mvn -pl server test -Dtest='Sprint14*,Sprint141*'`
  → **38 / 0 / 0 / 0** (33 Sprint 14 + 5 Sprint 14.1 closure tests).
- `mvn -pl server test -Dtest='Sprint10*Test,Sprint11*Test,
  Sprint12*Test,Sprint13*Test,PhaseEvaluatorPlanTest,Cs014*,
  Cs066*,Cs095*,Cs002*,Cs029*,Cs176*,Cs001*,EscalationReason*Test,
  Sprint6*,Sprint7*Test,Sprint8*Test,Sprint9*Test'`
  → **269 / 0 / 0 / 0** (named Sprint regression suite — including
  Sprint 6 §G2 FAQ S1 guard, cs014 / cs066 / cs095 / cs002 / cs029 /
  cs176 regressions).
- `mvn -pl server test`
  → **859 / 0 / 0 / 0** (was 854; +5 new Sprint 14.1 closure tests).
- `pytest eval_interactive/tests/` — not re-run; Sprint 14.1 changes
  no eval-output schema and no Python parsing path.
- Smoke runs not required — Sprint 14.1 changes no FAQ corpus, no
  prompt, no judge, no CaseSpec, no escalation enum, no routing
  taxonomy, no eval-output schema.

### What Sprint 14.1 explicitly does NOT do

- No DB migration. The trace surface is the existing `jsonb`
  `bot_turns.projected_context` column — `faq_grounding` rides as an
  additive top-level key. Old rows simply won't have the key.
- No hard citation gate. Missing citation remains observable
  (`citation_present=false`, optionally `resolved_but_uncited=true` /
  `retrieved_but_unresolved=true`) but is never used to reject,
  rewrite, or re-loop the bot reply.
- No broad S1 hardening. The Sprint 16 §S1 candidates documented in
  `docs/current/faq_grounding_contract.md` §6 (`R-faq-grounded-resolve-bypass`,
  `R-cited-but-unresolved`, `R-resolved-but-uncited-rate`,
  `R-canonical-url-missing-rate`) remain deferred.
- No new skill runtime framework, no new escalation reason value, no
  CaseSpec edits, no FAQ corpus content edit, no judge / prompt /
  routing / risk-policy change.

## 16. Sprint 15 — Script / Policy Config Governance

Status: **accepted / ready to archive**. The Sprint 15 Codex review
returned `decision: pass, blocking_count: 0`, accepting Sprint 15
as inside the config-governance scope (no prompt, routing, judge,
corpus, CaseSpec, hard citation gate, hot reload, dashboard, or
broad runtime work). Sprint 15 is the latest accepted sprint and
introduced no runtime semantic change. Diff stays narrow: three
actions only.

### 16.1 Implemented actions

#### M0 — Externalize DriftDetector / risk keywords to YAML

What landed:

- New config: `server/src/main/resources/config/risk-keywords.yaml`
  (`version: 1`). Carries the exact same five hard-shift groups
  (UC-J / UC-G / UC-I / UC-J / UC-H, in the original declaration
  order) with the exact same keyword strings as the previous
  hardcoded `DriftDetector.HARD_SHIFT_KEYWORDS` list, plus the same
  escalation regex under `escalation-pattern`.
- New loader: `RiskKeywordsConfig` (Spring `@Service`) loads the YAML
  at startup, compiles the escalation pattern, and validates:
  required fields, non-empty groups, non-blank keywords, no
  cross-group keyword duplicates (which would be unreachable under
  first-match-wins), and a parseable regex. Fail-fast on any
  violation.
- `DriftDetector` constructor-injects `RiskKeywordsConfig`. The
  matching contract is unchanged — same lower-case substring check,
  same first-match-wins precedence within and across groups, same
  same-UC suppression, same `DriftResult` outputs.
- A no-arg test convenience constructor on `DriftDetector` loads the
  bundled default classpath resource so legacy unit tests
  (`DriftDetectorTest`) keep working without behavioural change.

Sprint 15 §M0 explicitly does NOT add new risk semantics, new risk
levels, new escalation reasons, or auto-handover for Level 1 / Level
2 risk signals. Future risk-policy reviews are expected to land as
config diff rather than Java source edits.

Tests added:

- `Sprint15RiskKeywordsConfigTest` (11 cases) — config validation
  parity (default loads, group / keyword set parity vs the hardcoded
  reference, version pin), six structural-validation negative tests
  (missing escalation pattern, empty group list, empty keywords,
  blank target UC, duplicate keyword, invalid regex), the full
  parity matrix across escalation phrases / hard-shift groups /
  same-UC suppression / escalation-precedence / no-drift baseline,
  the cross-group first-match-wins precedence assertion, and a
  keyword-uniqueness sanity guard.

#### M1 — Script library version pin and docs ↔ YAML consistency check

What landed:

- `server/src/main/resources/scripts/templates.yaml` now declares
  `library_version: "v1.1"` and `library_version_date: "2026-04-21"`,
  matching the latest entry of the `## 11. Version` table in
  `docs/fixed_script_library_v1.md`.
- `ScriptLibraryService` reads and exposes the version pin
  (`getLibraryVersion()`, `getLibraryVersionDate()`) and fails fast
  at startup if `library_version` is missing.
- New build-time check:
  `Sprint15ScriptLibraryConsistencyTest` parses both surfaces and
  fails the build when:
  - `library_version` / `library_version_date` is absent or blank,
  - the YAML version pin diverges from the latest doc version row,
  - any required template ID listed in the test's reference manifest
    is missing from the YAML,
  - a required template's `variables:` declaration drifts from the
    doc's variable contract,
  - a top-level template id is duplicated.

Sprint 15 §M1 explicitly does NOT rewrite script copy, change tone /
escalation wording, alter forbidden-phrase rules, or introduce a new
template engine. ScriptLibraryService substitution semantics are
unchanged except for additive metadata validation.

Tests added:

- `Sprint15ScriptLibraryConsistencyTest` (5 cases) — version pin
  presence, version + date parity vs the doc's §11 Version table,
  required-template-IDs presence, required-variables contract
  parity for every template that takes parameters, and duplicate
  top-level id detection.

#### M2 — Retrieval / answer gate / rerank fallback thresholds + diagnostics

What landed:

- New `@ConfigurationProperties("knowledge.retrieval")` bean
  `KnowledgeRetrievalProperties` carrying the previously hardcoded
  thresholds with **defaults unchanged**:
  - `ann-limit: 20`
  - `retrieval-gate-threshold: 0.3`
  - `answer-gate-threshold: 3.5`
  - `rerank-candidates: 8`
  - `top-results: 3`
  - `rerank-fallback-score: 2.5`
- Range validation runs in a `@PostConstruct` `validate()` step:
  `ann-limit ≥ rerank-candidates ≥ top-results ≥ 1`, retrieval gate
  ∈ [0.0, 1.0], answer gate ∈ [1.0, 5.0], fallback score ∈ [1.0,
  5.0], and `rerank-fallback-score < answer-gate-threshold` so a
  fallback-score result can never satisfy the answer gate.
- `application.yml` adds an explicit `knowledge.retrieval` block
  whose values match the defaults verbatim — future tuning becomes
  an auditable config diff rather than a hidden Java edit.
- `KnowledgeSearchService` no longer holds `static final`
  thresholds; it reads them per-call from the injected properties
  bean. Pipeline behaviour (ANN limit, retrieval gate test, dedup,
  rerank window, answer gate test, top-results truncation) is
  unchanged.
- `RerankService` now sources the neutral fallback score from
  `KnowledgeRetrievalProperties.rerankFallbackScore()` (default 2.5,
  unchanged) for both the parse-fallback path and the LLM-failure /
  future-failure paths.
- Diagnostics added (observability-only, no behaviour change):
  - `KnowledgeSearchService` logs `retrieval_miss` /
    `answer_miss` with `reason=retrieval_gate` or `reason=answer_gate`
    and the threshold value; on `answer_miss` it also logs
    `top_is_fallback_score=true|false` so a parse-failure /
    call-failure attribution is visible.
  - `RerankService` logs `Rerank parse fallback` with
    `reason=empty_response | unparseable` plus the fallback score
    used, and `Rerank LLM call failed` /
    `Rerank future failed` with the fallback score on the failure
    paths.

Sprint 15 §M2 explicitly does NOT tune thresholds, change FAQ
answerability semantics, change corpus content, or change eval
expected outcomes.

Tests added:

- `Sprint15KnowledgeRetrievalConfigTest` (9 cases) — defaults parity
  (each default pinned by literal value), defaults pass validation,
  and seven structural-validation negative tests (ann-limit < 1,
  ann-limit < rerank-candidates, rerank-candidates < top-results,
  retrieval gate out of range, answer gate out of range,
  fallback ≥ answer gate, fallback out of range).

Updates to existing tests for constructor signature changes:

- `RerankServiceTest` — passes the default
  `KnowledgeRetrievalProperties` instance into the constructor; the
  pre-existing parse-failure / call-failure cases remain green and
  continue to assert the 2.5 fallback score (now sourced from the
  default config).
- `Sprint14KnowledgeSearchPublishedFilterTest` — passes the default
  `KnowledgeRetrievalProperties` instance into the constructor;
  Sprint 14 §L0 published-only filter / projection contract is
  preserved and remains green.

### 16.2 Files changed (Sprint 15)

Production code:

- `server/src/main/java/com/gumtree/csagent/service/runtime/RiskKeywordsConfig.java` (new)
- `server/src/main/java/com/gumtree/csagent/service/runtime/DriftDetector.java`
  (constructor-injects `RiskKeywordsConfig`; matching contract
  unchanged)
- `server/src/main/java/com/gumtree/csagent/service/guardrails/ScriptLibraryService.java`
  (reads `library_version` / `library_version_date`; fail-fast on
  missing pin; substitution semantics unchanged)
- `server/src/main/java/com/gumtree/csagent/config/KnowledgeRetrievalProperties.java` (new)
- `server/src/main/java/com/gumtree/csagent/service/knowledge/KnowledgeSearchService.java`
  (constructor-injects `KnowledgeRetrievalProperties`; reads
  thresholds from config; adds threshold-gate diagnostics; pipeline
  behaviour unchanged)
- `server/src/main/java/com/gumtree/csagent/service/knowledge/RerankService.java`
  (constructor-injects `KnowledgeRetrievalProperties`; reads
  fallback score from config; adds parse / call / future failure
  diagnostics; scoring contract unchanged)

Config / resources:

- `server/src/main/resources/config/risk-keywords.yaml` (new)
- `server/src/main/resources/scripts/templates.yaml`
  (additive `library_version` + `library_version_date` keys; no
  template copy change)
- `server/src/main/resources/application.yml`
  (additive `knowledge.retrieval.*` block matching the previous
  hardcoded defaults verbatim)

Tests (new):

- `Sprint15RiskKeywordsConfigTest` (11 cases — config validation,
  group / keyword parity, full DriftDetector behavioural parity
  matrix, first-match-wins precedence)
- `Sprint15ScriptLibraryConsistencyTest` (5 cases — version pin
  presence, doc-vs-YAML version + date parity, required template
  IDs, required variables contract, duplicate-id guard)
- `Sprint15KnowledgeRetrievalConfigTest` (9 cases — defaults parity,
  validation positive + 7 negatives)

Tests (updated for constructor signature):

- `RerankServiceTest`
- `Sprint14KnowledgeSearchPublishedFilterTest`

Docs:

- `docs/10-handoff.md` (this section + §1 update).
- `docs/action_bank.md` (Sprint 15 deliverables added).
- `docs/sprint_objective.md` retained — already contains the
  Sprint 15 objective.
- No edit to `docs/current_eval_baseline.md` — Sprint 15 changes no
  eval expected outcome; baseline remains the post-Sprint-8 r1 / r2
  runs.

No FAQ corpus content, CaseSpec, judge, broad routing taxonomy,
handover payload contract, Salesforce backend contract, full Issue
Ledger, all-UC task taxonomy, escalation enum, system prompt, DB
schema migration, runtime main-flow architecture file, or hard
citation gate was touched.

### 16.3 Tests run

- `mvn -pl server test -Dtest='Sprint15*,DriftDetectorTest,RerankServiceTest'`
  → **55 / 0 / 0 / 0** (25 Sprint 15 §M0 / §M1 / §M2 focused tests +
  18 legacy `DriftDetectorTest` cases + 12 `RerankServiceTest`
  cases, confirming parity under the new wiring).
- `mvn -pl server test -Dtest='Sprint10*Test,Sprint11*Test,
  Sprint12*Test,Sprint13*Test,PhaseEvaluatorPlanTest,Cs014*,
  Cs066*,Cs095*,Cs002*,Cs029*,Cs176*,Cs001*,EscalationReason*Test,
  Sprint6*,Sprint7*Test,Sprint8*Test,Sprint9*Test,Sprint14*,
  Sprint141*,Sprint15*'`
  → **332 / 0 / 0 / 0** (extended named regression suite — including
  Sprint 6 §G2 FAQ S1 guard, cs014 / cs066 / cs095 / cs002 / cs029 /
  cs176 regressions, all Sprint 14 §L0 / §L1 / §L2 + §14.1 trace
  persistence, plus the new Sprint 15 §M0 / §M1 / §M2 focused
  suite).
- `mvn -pl server test`
  → **884 / 0 / 0 / 0** (was 859 pre-Sprint-15; +25 new Sprint 15
  §M0 / §M1 / §M2 tests).
- `pytest eval_interactive/tests/` — not re-run; Sprint 15 changes
  no Python parsing path and no eval-output schema.
- Smoke runs not required — Sprint 15 changes no FAQ corpus, no
  prompt, no judge, no CaseSpec, no escalation enum, no routing
  taxonomy, no eval-output schema. The current canonical baseline
  (`docs/current_eval_baseline.md`) is preserved (post-Sprint-8 r1 /
  r2 runs).

### 16.4 Behaviour-preservation evidence

- §M0 — `Sprint15RiskKeywordsConfigTest.detect_parityMatrix_matchesHardcodedReference`
  exercises every escalation phrase, every hard-shift keyword group,
  the same-UC suppression rule, the escalation-precedence rule, and
  the no-drift baseline against the previously hardcoded
  expectations. The legacy `DriftDetectorTest` (18 cases) keeps
  passing under the new YAML-loaded path. The
  cross-group-first-match-wins assertion locks the precedence
  ordering. No DriftResult shape, type, or field has changed.
- §M1 — `ScriptLibraryService.getTemplate(...)` /
  `renderTemplate(...)` substitution semantics are unchanged. The
  approved template copy (50+ templates / 14 categories) is
  bit-for-bit identical to v1.1 of the doc; the consistency test
  enforces no silent copy drift in the variables contract.
- §M2 — `Sprint15KnowledgeRetrievalConfigTest.defaults_matchPreviouslyHardcodedConstants`
  pins each default to the literal value of the prior `static
  final` constant. `Sprint14KnowledgeSearchPublishedFilterTest`
  (Sprint 14 §L0 contract) and `RerankServiceTest` (parse / call
  failure → 2.5 fallback) keep passing under the new wiring. The
  full retrieval pipeline (ANN limit, retrieval gate test, dedup,
  rerank window, answer gate test, top-results truncation) is
  observable-equivalent under default config; the validator
  forbids any threshold combination that would change semantic
  ordering.

### 16.5 Config files added / changed

- `server/src/main/resources/config/risk-keywords.yaml` (added) —
  source of truth for `DriftDetector` escalation pattern + hard-shift
  groups.
- `server/src/main/resources/scripts/templates.yaml` (changed —
  additive `library_version`, `library_version_date` only).
- `server/src/main/resources/application.yml` (changed — additive
  `knowledge.retrieval.*` block; defaults match prior hardcoded
  constants verbatim).

### 16.6 Regression guard outcomes

All Sprint 15 active guards pass:

- `L1:escalation_reason_consistency` = **0** (never re-introduced).
- `CONTRACT_VIOLATION:active_use_case` = **0**.
- Sprint 14 `bot_turns.projected_context.faq_grounding` persistence
  trace remains intact (`Sprint141FaqGroundingTracePersistenceTest`,
  5 cases).
- Sprint 14 published-only KB search remains intact
  (`Sprint14KnowledgeSearchPublishedFilterTest`, 4 cases).
- Existing FAQ S1 guard remains green
  (`AgentRunLoopS1FaqGroundedResolveGuardTest`).
- Sprint 6 §G0 ReadTimeout no-retry closure intact.
- cs014 remains UC-C
  (`Cs014RouteAndLoopHandoverIntegrationTest`,
  `Cs014RouteAndDistressRegressionTest`).
- cs066 remains UC-K.
- cs095 remains UC-A / not UC-K / not UC-FP.
- cs002 distress reconciliation remains green.
- cs029 remains UC-D + `user_requested`.
- cs176 explicit-human-help → `user_requested` regression remains
  green.
- All Sprint 7 / 7.1 / 8 / 8.1 / 8.2 / 9 / 9.1 / 10 / 11 / 11.1 /
  12 / 13 hard invariants remain green
  (named regression suite: 332 / 0).
- `RuntimeIntentClassifier` remains runtime-internal.
- `bot_turns.source_ids` write path unchanged.
- `bot_turns` / `bot_sessions` / `kb_articles` / `kb_chunks`
  schemas unchanged.

### 16.7 Remaining risks

**No new P0 / P1 blockers opened by Sprint 15.** The change is
config-governance + diagnostics; no runtime main-flow architecture
file gained new semantics, no DriftResult / KnowledgeSearchResult /
ScoredCandidate shape changed.

Carry-over notes:

- The `library_version` pin must be updated whenever
  `docs/fixed_script_library_v1.md` ships a new approved version
  row — `Sprint15ScriptLibraryConsistencyTest` will fail the build
  if the two surfaces diverge. This is the intended invariant.
- Future threshold tuning must land as a `knowledge.retrieval.*`
  config diff. The validator constraints
  (`fallback < answer-gate`, `ann ≥ rerank ≥ top`) are the floor,
  not a tuned recommendation.
- `RiskKeywordsConfig` is loaded once at startup. Hot-reload remains
  out of scope; Sprint 15 explicitly does NOT introduce ops-owned
  runtime config. A risk-keyword change still requires a redeploy.

Residuals carried forward from Sprint 14 §10 remain unchanged
(`R-faq-grounded-resolve-bypass`, `R-cited-but-unresolved`,
`R-resolved-but-uncited-rate`, `R-canonical-url-missing-rate`, plus
the older Sprint 13 §8 list).

### 16.8 Recommended next phase

The Sprint 15 closure surfaces no runtime regression. The previously
listed alternatives remain viable:

1. **Eval Governance docs sprint** — primary recommendation. The
   largest residual category is still `judge_volatility` /
   `faq_corpus_gap` / `product_policy_gap`, all governance-owned.
2. **Narrow Sprint 16 §S1 hardening** — only if real-traffic
   evidence demonstrates `R-faq-grounded-resolve-bypass`
   reproducibly affects answer correctness on a high-traffic UC.
3. **Narrow Corpus Curation** — fill the 38 missing
   `Help_Site_URL__c` rows. Owner: Eval Governance / corpus audit.
4. **Re-run validation after infra cleanup** — viable if Kimi /
   smoke credentials are available; runs alongside Eval Governance.

Do not start another runtime sprint unless triage finds a new
P0 / P1 runtime blocker. Sprint 15 explicitly does NOT promote a
new canonical eval baseline.

### 16.9 What Sprint 15 explicitly does NOT do

- No new risk semantics, no new escalation reasons, no new risk
  levels, no auto-handover for Level 1 / Level 2 risk signals.
- No new hard Java guard.
- No prompt rewrite, no system prompt edit, no broad routing
  rewrite.
- No hard runtime citation gate.
- No FAQ corpus rewrite.
- No judge calibration, no CaseSpec changes, no eval expansion.
- No anchor / exploration / promotion hard-gate expansion.
- No hot reload / ops-owned runtime config.
- No dashboard work, no full TraceViewer redesign.
- No new skill runtime framework.
- No broad S1 rewrite.
- No DB migration. No schema change. No bot_turns / bot_sessions /
  kb_articles / kb_chunks edit.
- No threshold tuning. Defaults are pinned to the previous
  hardcoded values verbatim.

## 17. Sprint 16 — Handover Exactly-Once Contract and Repro

Status: **landed (docs + characterization-test sprint, no runtime
change)**. Sprint 16 freezes the handover exactly-once contract and
adds the characterization tests that demonstrate the current
LLM-driven dual local-persistence shape, without modifying any
runtime behaviour. It does not yet implement
`HandoverOrchestrator`; that is the explicitly deferred future
runtime sprint trigger.

### 17.1 Exact docs / tests changed

Docs (new + edited):

- `docs/proposals/handover_orchestrator_design.md` (new) — defines the
  exactly-once contract. §1 names the current dual-path shape
  (writer #1 = `RequestHandoverTool` →
  `SalesforceService.requestHandover` →
  `MockSalesforceService` → `mock_handover_log` row #1; writer #2
  = `SessionManager.recordHandover` → direct
  `handoverLogRepository.save(...)` → `mock_handover_log` row
  #2). §2 distinguishes confirmed local persistence duplication
  from unproven real Salesforce double transfer. §3 freezes three
  contracts: trace evidence (`request_handover`), outcome
  persistence (`record_outcome`), and the future
  `HandoverOrchestrator` handover side-effect (idempotent by
  `session_id`). §4 sketches the future orchestrator shape. §5
  records why Sprint 16 is docs + characterization only. §6
  pre-specifies acceptance criteria for the future runtime sprint.
- `docs/runtime_freeze_and_risk_policy.md` (edited) — added §10
  "Known unspecced surfaces" with §10.1 entry covering the
  handover dual-path. Renumbered References to §11; added
  cross-reference links to the Sprint 16 design / release_gate /
  source files.
- `docs/release_gate.md` (new) — opens the release-gate ledger.
  §1.1 blocking rule: before real Salesforce cutover, the
  handover side-effect must be idempotent by `session_id`. The
  rule is **not satisfied** at Sprint 16 close.
- `docs/action_bank.md` (edited) — Sprint 16 row added to §3
  active deliverables (H0 / H1 / H2); new
  D-single-handover-orchestrator entry in §4 deferred runtime
  candidates ("Single Handover Orchestrator: exactly-once
  side-effect owner before Salesforce production cutover");
  Sprint 16 row added to §6 closed-action index.
- `docs/sprint_objective.md` retained — already contains the
  Sprint 16 objective.
- `docs/10-handoff.md` (this file) — §1 and new §17 (this
  section).

Tests (new):

- `server/src/test/java/com/gumtree/csagent/service/runtime/Sprint16HandoverDualPathReproTest.java`
  (3 cases): two passing characterization cases plus one
  `@Disabled` future-invariant case.
  - `dualLocalPersistence_llmDrivenRequestHandoverPath_producesTwoMockHandoverLogWrites_currentBehaviour`
    (passing) — exercises `RequestHandoverTool.execute(...)` with
    a real `MockSalesforceService` backed by a mocked
    `MockHandoverLogRepository`, then replays the
    `SessionManager.recordHandover` surface (real
    `HandoverPayloadAssembler` → direct `repo.save(...)`).
    Asserts two saves on the same repository for the same
    `session_id`; asserts the two payloads carry distinct
    `version` (`"1.0"` vs `"1.1"`) and distinct `transfer_result`
    (`MockSalesforceService` set vs `mock_transfer` literal).
  - `distinguishesLocalPersistenceDuplication_fromUnprovenRealSalesforceDoubleTransfer`
    (passing) — pins that today the only `SalesforceService`
    implementation is `MockSalesforceService`. Designed to break
    on first wire-up of a real Salesforce client so the reviewer
    is forced to re-read the §10.1 contract before passing the
    release gate.
  - `futureInvariant_atMostOneTransmittedHandoverDecisionPerSessionId_disabledUntilOrchestratorLands`
    (`@Disabled`, TODO) — encodes the future invariant: at most
    one transmitted / `offline_logged` handover decision per
    `session_id`. Would fail today by design (the LLM-driven
    path writes twice). Removing the `@Disabled` annotation is
    listed as one of the acceptance criteria for the future
    "Single Handover Orchestrator" runtime sprint
    (`docs/release_gate.md` §1.1 #4-#5).

Source comments (new, no behaviour change):

- `server/src/main/java/com/gumtree/csagent/service/tools/RequestHandoverTool.java`
  — class-level Javadoc carries a Sprint 16 §H0
  known-unspecced-surface marker pointing to
  `docs/proposals/handover_orchestrator_design.md` and the future-invariant
  test.
- `server/src/main/java/com/gumtree/csagent/service/runtime/SessionManager.java`
  — `recordHandover(...)` carries the matching marker on its
  Javadoc.

### 17.2 Current confirmed risk

- **Duplicated local persistence (P2 today, mock / local).** On
  the LLM-driven `request_handover` path two
  `mock_handover_log` rows are written for the same `session_id`
  on the same ESCALATE turn. The two writers are unaware of each
  other and persist payloads with different `version` and
  `transfer_result` shapes.
- **P1 launch-readiness severity.** The dual-path shape is one
  wire-up away from a real double transfer at the moment a
  production `SalesforceService` implementation lands.
- **P0 / P1 production-incident severity** if a real double
  transfer occurs after cutover (case re-routed twice in
  Salesforce: re-assignment, mis-prioritisation, queue
  duplication; user-visible).
- The hard-OOS path (`SessionManager.createSession` → synthetic
  `request_handover` evidence + single `recordHandover`) and the
  kernel force-escalate / Step 2.5 path remain single-write today
  (pinned by Sprint 16 §H0 contract; not opened or weakened by
  Sprint 16).

### 17.3 What is not confirmed

- A real **double Salesforce transfer** is not currently proven.
  The production Salesforce client is not wired; only
  `MockSalesforceService` (`@Profile("local")`) implements
  `SalesforceService`, and `SessionManager.recordHandover` does
  not flow through `SalesforceService` at all (it writes directly
  to `mock_handover_log`).
- `HandoverPayloadAssembler` is the candidate single payload
  builder for the future orchestrator. Sprint 16 does not commit
  to it as the final shape; the orchestrator design doc names it
  as the candidate but leaves payload-schema decisions to the
  runtime sprint.
- No production case routing is currently affected. The
  characterization test runs against an in-memory mocked
  repository.

### 17.4 Future runtime sprint trigger

A future "Single Handover Orchestrator" runtime sprint must start
when either:

1. a real Salesforce client is staged for cutover (the launch-time
   wire-up forces the orchestrator to land first to satisfy
   `docs/release_gate.md` §1.1), or
2. a real-traffic case shows duplicated handover routing in
   Salesforce (would be the first P0 / P1 confirmation that
   moves the issue out of P2 territory).

Until then, `Sprint16HandoverDualPathReproTest` is the standing
guard that the dual-path shape has not been silently fixed (the
two passing cases would break) and that the future invariant has
not been silently re-introduced as a hard gate (the `@Disabled`
case would need to be re-enabled deliberately).

The runtime sprint's acceptance criteria are pre-specified in
`docs/proposals/handover_orchestrator_design.md` §6 and
`docs/release_gate.md` §1.1.

### 17.5 Tests run

- `mvn -pl server test
  -Dtest='Sprint16*,RequestHandoverToolTest,HandoverPayloadAssemblerTest,Cs014RouteAndLoopHandoverIntegrationTest,Cs176ExplicitHumanHelpHandoverIntegrationTest,AgentRunLoopHandoverReasonNormalizationIntegrationTest'`
  → **22 / 0 / 0 / 1** (Sprint 16 §H1 focused suite + handover
  regression suite; the 1 skipped is the `@Disabled`
  future-invariant case).
- `mvn -pl server test`
  → **887 / 0 / 0 / 1** (was 884 pre-Sprint-16; +2 new passing
  Sprint 16 tests + 1 new disabled future-invariant test). No
  regression in any Sprint 6 / 7 / 7.1 / 8 / 8.2 / 9 / 9.1 / 10
  / 11 / 11.1 / 12 / 13 / 14 / 14.1 / 15 invariant. The cs014 /
  cs066 / cs095 / cs002 / cs029 / cs176 / cs001 regression suite
  remains green. `L1:escalation_reason_consistency = 0` and
  `CONTRACT_VIOLATION:active_use_case = 0` are preserved.
- `pytest eval_interactive/tests/` — not re-run; Sprint 16
  changes no Python file, no eval-output schema, no judge, no
  CaseSpec, no FAQ corpus. The Sprint 16 diff is two Markdown
  files (new), three Markdown files (edited), one new Java test,
  and two Javadoc-only edits on existing Java source.
- Smoke runs not required — Sprint 16 changes no FAQ corpus, no
  prompt, no judge, no CaseSpec, no escalation enum, no routing
  taxonomy, no eval-output schema, no DB schema. The current
  canonical baseline (`docs/current_eval_baseline.md`) is
  preserved (post-Sprint-8 r1 / r2 runs).

### 17.6 Next recommended action

Recommended next phase: **defer**. Sprint 16 is intentionally a
docs + characterization sprint; the runtime fix
("Single Handover Orchestrator") is queued as a deferred runtime
candidate (`docs/action_bank.md` §4 row
`D-single-handover-orchestrator`) and gated by the real-Salesforce
cutover plan (`docs/release_gate.md` §1.1).

In priority order:

1. **Eval Governance Follow-up** — primary recommendation
   carried over from Sprint 15. Largest residual category is
   still `judge_volatility` / `faq_corpus_gap` /
   `product_policy_gap`.
2. **Single Handover Orchestrator runtime sprint** — start ONLY
   when (a) a real Salesforce client is staged for cutover, or
   (b) a real-traffic case shows duplicated handover routing.
   Acceptance criteria pre-specified in
   `docs/proposals/handover_orchestrator_design.md` §6 and
   `docs/release_gate.md` §1.1; the `@Disabled` future-invariant
   test is the closing artifact.
3. **Narrow Sprint 16 §S1 hardening** (FAQ-grounded resolve
   bypass) — only on real-traffic evidence per
   `docs/current/faq_grounding_contract.md` §6.
4. **Narrow Corpus Curation** — fill the 38 missing
   `Help_Site_URL__c` rows.

Do not start the Single Handover Orchestrator runtime sprint
opportunistically. Sprint 13's runtime freeze remains in force; a
new runtime sprint requires a P0 / P1 trigger or the real
Salesforce cutover plan.

### 17.7 What Sprint 16 explicitly does NOT do

- No `HandoverOrchestrator` implementation. Sprint 16 is design
  + characterization only; the runtime fix is the future
  "Single Handover Orchestrator" sprint.
- No production Salesforce client. The release-gate rule
  (`docs/release_gate.md` §1.1) blocks that wire-up until the
  orchestrator lands.
- No change to `RequestHandoverTool` behaviour. Class-level
  Javadoc adds a §H0 marker only.
- No change to `SessionManager.recordHandover` behaviour. Method
  Javadoc adds a §H0 marker only.
- No change to `MockSalesforceService` behaviour or the
  `SalesforceService` interface.
- No change to the Phase 3 §3.6.2 handover payload schema.
- No change to `HandoverPayloadAssembler` behaviour.
- No prompt edit. No `system_prompt.txt` change.
- No routing change. No new escalation reason. No CaseSpec
  change. No FAQ corpus change. No judge calibration change. No
  eval expected-outcome change.
- No DB migration. No schema change. `mock_handover_log` /
  `bot_turns` / `bot_sessions` / `kb_articles` / `kb_chunks`
  schemas unchanged.
- No new live smoke baseline. No anchor / exploration /
  promotion hard-gate change.
