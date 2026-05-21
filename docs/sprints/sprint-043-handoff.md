---
title: Sprint 43 (NEW M3-Eval sub-sprint 2, S-Eval-2) — Skill `critical_steps` schema + extractor + projection wiring — DEV HANDOFF
doc_tier: sprint-archive
status: current
implementation_status: implemented
source_of_truth: this file (dev-authored); deliver-agent + human append §12 closure verdict at close
last_reviewed: 2026-05-21
review_cadence: per sub-sprint
supersedes: []
superseded_by: null
notes: >
  Sprint 43 / S-Eval-2 dev handoff. Ships the multi-layer (Java + Python)
  schema + contract + structural defence for the M3-Eval Tier-2
  Critical-flow band: NEW `Skill.critical_steps` field + `CriticalStep`
  Java record + `SkillLoader` allowlist validation, `ContextProjectionBuilder`
  LLM-visible projection wiring, NEW Python `SkillProcedureExtractor` with
  a minimal 6-primitive `trace_check` DSL that **rejects regex / keyword /
  message-content matching by parser construction** (the milestone's
  primary §1.7 structural defence), and `composite.py` Tier-2 gate band.
  Ships **NO `critical_steps` content** — populated in S-Eval-3 under
  per-sub-sprint Codex review (§4.3 trigger #2). Codex deferred to
  M3-Eval milestone close per §4.3 default (no S-Eval-2 trigger fired).
---

# Sprint 43 (NEW M3-Eval sub-sprint 2, S-Eval-2) — Dev Handoff

## 1. Sprint identity

- **Sprint number**: 43.
- **Sub-sprint**: S-Eval-2 (second sub-sprint of M3-Eval; schema +
  contract + structural defence sub-sprint per
  `docs/milestone_objective.md` §3).
- **Milestone**: M3-Eval (Coarse-to-Fine Evaluation Architecture).
- **Layer (per `iteration_governance.md` §3.2)**: `eval_spec` (primary —
  extractor + DSL parser + composite Tier-2 wiring + Skill schema
  extension) + `prompt_projection` (auxiliary — LLM-visible
  `phase_plan.critical_steps` projection slot). Multi-layer
  semantic-touching sub-sprint; §7 stanza REQUIRED (filled at
  `docs/sprint_objective.md` §8).
- **Codex review plan**: milestone-shared at M3-Eval close (default
  per §4.3); no per-sub-sprint trigger fired for S-Eval-2 (only S-Eval-3
  carries §4.3 trigger #2 for its LLM-visible content authoring).
- **HEAD baseline at sub-sprint start**: `357e949` (`docs: S-Eval-1
  close (A — Clean PASS) + M3-Eval setup-bundle (deliver-agent)`).

## 2. Summary

S-Eval-2 lands the Java + Python schema, contract, and structural
defence for the M3-Eval Tier-2 Critical-flow band. The Skill YAML
schema gains a NEW optional `critical_steps: list[CriticalStep]` field
with five sub-fields (`id`, `desc`, `trace_check`, `mandatory_for`,
`severity`); `SkillLoader` validates per the Sprint 38-fix allowlist
precedent; `ContextProjectionBuilder` renders the LLM-visible `id` +
`desc` pair as a list-of-objects under `phase_plan.critical_steps`
(empty list → no key, parity preservation); a NEW Python
`SkillProcedureExtractor` evaluates each step's `trace_check` against
the session trace via a minimal 6-primitive recursive-descent DSL
parser that **rejects regex / keyword / message-content matching at
parse time** (the milestone's primary §1.7 structural defence); and
`composite.py` adds a Tier-2 `skill_procedure_followship` gate band
keyed on the S-Eval-1 (D-2.5) `severity` convention. Sprint 43 ships
NO `critical_steps` content on any of the 6 Skills — that lands in
S-Eval-3 under per-sub-sprint Codex review per §4.3 trigger #2.

## 3. Files shipped (per-file numstat)

Reproduce via `git show --numstat <commit>` after the bundle lands.
Pre-commit numstat (from `git diff --numstat` + `wc -l` on new files):

### Java (6 files)

| Path | Change | + | − | Notes |
|---|---|---:|---:|---|
| `server/src/main/java/com/gumtree/csagent/service/runtime/skill/Skill.java` | EDIT | 42 | 3 | Add `criticalSteps` to canonical 16-arg ctor + compact-ctor normalization + secondary 15-arg backward-compat ctor (per choice (d) below) + `fromYaml` JSON wiring. |
| `server/src/main/java/com/gumtree/csagent/service/runtime/skill/CriticalStep.java` | NEW | 89 | 0 | NEW record (Sprint 43). 5 fields + nested `Severity` enum (`MANDATORY`/`ADVISORY`) + `fromYaml` Jackson hooks. |
| `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillLoader.java` | EDIT | 64 | 0 | NEW `VALID_CRITICAL_STEP_SEVERITIES` allowlist + per-step validation block + `requireStepField` helper. Duplicate-id check guards against authoring slip-up. No `select(...)` / dispatch touch. |
| `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java` | EDIT | 29 | 0 | NEW `critical_steps` projection-rendering block, inserted between `system_instruction` and `escalation_policy` inside `phase_plan`. Empty list → no key (parity preservation). |
| `server/src/test/java/com/gumtree/csagent/service/runtime/skill/SkillCriticalStepsLoadingTest.java` | NEW | 333 | 0 | 14 tests: load 6 production YAMLs (empty); parse valid block (2 steps); empty / absent block defaults; 8 negative cases (missing id / blank desc / missing trace_check / empty mandatory_for / unknown UC / invalid severity / missing severity / duplicate id); severity enum lower-case YAML; backward-compat secondary ctor. |
| `server/src/test/java/com/gumtree/csagent/service/runtime/CriticalStepsProjectionTest.java` | NEW | 298 | 0 | 5 tests: empty `critical_steps[]` → no projection key; SkillRegistry miss → no key; populated → list-of-objects with `id`+`desc` only (no `trace_check` / `severity` / `mandatory_for` leak to LLM); rendering order check (after `system_instruction`, before `escalation_policy`); desc verbatim (no substitution). Uses Mockito to mock `SkillRegistry` — preserves M2 §6 hard fence #5. |

### Python (5 files)

| Path | Change | + | − | Notes |
|---|---|---:|---:|---|
| `eval_interactive/eval_interactive/scoring/skill_procedure_check.py` | NEW | 771 | 0 | NEW module. `CriticalStep` / `Skill` / `CriticalStepResult` / `Tier2Result` dataclasses; recursive-descent DSL parser (`parse_trace_check` + `_Parser` + 6 AST node types); DSL evaluator (`evaluate_trace_check`); `TraceView` adapter over `per_turn_trace`; minimal embedded `load_skills_from_dir` (pyYAML); `SkillProcedureExtractor` public entry; `tier2_results_to_gate` composite adapter. |
| `eval_interactive/eval_interactive/scoring/composite.py` | EDIT | 61 | 4 | Tier-2 gate band wired alongside Tier-0/L1, L2, L3. Imports + new `tier2_result` arg (default empty advisory PASS for backward compat) + `case_passed = l1 AND mandatory_l2 AND NOT tier2_critical_failed` + `TIER2:` / `TIER2_ADVISORY:` failure tags + updated `_build_detail` signature. |
| `eval_interactive/tests/test_skill_procedure_extractor.py` | NEW | 588 | 0 | 30 tests across 5 primitive classes + combinator nesting + extractor end-to-end PASS/FAIL/N/A + Tier-2 gate adapter + load 6 production Skills as smoke. |
| `eval_interactive/tests/test_skill_procedure_dsl_parser_rejects_hardcodes.py` | NEW | 152 | 0 | **§1.7 STRUCTURAL DEFENCE test.** 36 tests: 15 hardcode-flavoured `trace_check` strings rejected (well above contract §9 ≥6 floor; parameterized); positive grammar still accepts 11 canonical forms; 10 malformed-syntax inputs raise the same error class. |
| `eval_interactive/tests/test_composite_gate.py` | EDIT | 167 | 0 | NEW `TestTier2Gate` class (6 tests): empty default → no gate effect; mandatory fail → `case_passed=False` + `TIER2:` tag; advisory fail → `case_passed=True` + `TIER2_ADVISORY:` tag; all-N/A → no effect; all-pass clean detail; combined L1 + Tier-2 fail detail. |

### Handoff (1 file)

- `docs/sprints/sprint-043-handoff.md` (this file) — dev-authored
  12-section archive per Sprint 35 / 41 / 42 precedent.

**Bundle file count**: 12 (6 Java + 5 Python + 1 handoff). Right at the
contract §7 lower bound; matches estimate.

### Implementation-choice rationale (5 choice points)

**(a) Outcome 1 — `Severity` enum location**: **nested in `CriticalStep`
(`CriticalStep.Severity { MANDATORY, ADVISORY }`)**. Smallest surface
area; no separate file; severity is only meaningful in the context of
a critical step (no other Skill schema element carries the concept).
The matching Python literal `Literal["mandatory","advisory"]` mirrors
the S-Eval-1 (D-2.5) convention on `HardCheckResult` /
`OutcomeCheckResult` `severity` (which is a plain string field, not an
enum on the Python side). Rejected alternative: separate
`Severity.java` file in the same package — would add a third
record-shape file with no behavioural benefit and would invite
re-purposing the enum for other Skill-schema fields, which is a
scope-creep surface S-Eval-2 explicitly avoids.

**(b) Outcome 3 — projection rendering: list-of-strings vs
list-of-objects**: **list-of-objects `[{id, desc}]`**. The `id` lets
S-Eval-3 authors cross-reference steps from one `desc` to another by
stable id (e.g., "after `search_before_answer` completes, resolve the
top hit"). Byte overhead is ~30-40 chars per step for the `{id: "...", }`
wrapper; the trade-off vs the bare list-of-strings form is small
compared to a typical `desc` string (~80-150 chars). The empty-array
parity invariant (no `critical_steps` key when empty) is independent
of this choice and is asserted by `CriticalStepsProjectionTest`. The
projection deliberately excludes `trace_check` / `severity` /
`mandatory_for` from the LLM-visible payload (those are eval-side
contracts); verified by negative-assertion tests. Rejected
alternative: rendering only `desc` as a list-of-strings — saves ~150
bytes per Skill but loses the step-id semantic that S-Eval-3 may need.

**(c) Outcome 4 — DSL parser implementation**: **hand-rolled minimal
recursive-descent parser**. ~200 lines including AST nodes; no
third-party dependency (no `lark` / `pyparsing` / `antlr` vendor-in);
clear error messages naming the rejected primitive for §1.7
structural-defence transparency; trivially auditable for Codex review.
The 6 frozen primitives map cleanly to 6 AST node types + 2
combinator-result types; the tokenizer is a single `re` pattern.
Rejected alternative: `lark`-based PEG grammar — would add a non-stdlib
dep through `uv` and would put the §1.7 structural defence behind a
third-party grammar declaration that's harder to audit. The hand-rolled
parser is the cheapest path that gives clear errors.

**(d) Outcome 1 — ctor-update strategy**: **secondary 15-arg
constructor on `Skill` defaulting `criticalSteps` to `List.of()`**.
Zero existing test ctor sites need updating (the 13 `new Skill(...)`
call sites in `SkillTest.java`, `SkillRegistryTest.java`,
`SkillStateBusTest.java`, `SkillGuardrailDispatcherTest.java`,
`Skill.fromYaml(...)` all compile unchanged via the secondary ctor).
Per Sprint 41 OQ-S41.3 / Drift §7-a precedent: prefer the
minimum-touch path when adding an optional schema field with a clear
default. The canonical 16-arg ctor remains the public API for
constructing a Skill with explicit criticalSteps (used by the
projection test). Rejected alternative: positional update to 13 test
ctor sites — would balloon the bundle file count from 12 to ~25 with
no semantic benefit and would touch test files that have nothing to
do with S-Eval-2 scope.

**(e) Outcome 4 — Python Skill loader strategy**: **minimum-viable
embedded helper (`load_skills_from_dir`) in `skill_procedure_check.py`,
~40 lines**. No existing Python Skill loader in
`eval_interactive/eval_interactive/`; the extractor only needs `name`
+ `applicable_use_cases` + `critical_steps` from each YAML; a thin
pyYAML-based pass keeps the surface tight. The Java `SkillLoader` is
the authoritative validator; the Python loader is intentionally
permissive (skips malformed step entries with no error) so a broken
YAML still permits the rest of the eval suite to run while the Java
loader catches the schema break at Spring bootstrap. Rejected
alternative: separate `eval_interactive/eval_interactive/skills/loader.py`
file — would add an extra module for ~40 lines, and the loader has no
other consumer at S-Eval-2 close (S-Eval-3 may surface a need; can
graduate to a separate module at that point).

## 4. Schema extension details

### `Skill.java` (D-1.1)

- New field appended at end of canonical record components:
  `List<CriticalStep> criticalSteps`. Compact ctor normalizes null →
  `List.of()`; non-null copies via `List.copyOf`.
- NEW backward-compat secondary 15-arg ctor that omits `criticalSteps`
  and forwards to the canonical ctor with `List.of()`. Lets the 13
  pre-Sprint-43 `new Skill(...)` call sites compile unchanged. Per
  choice (d) above.
- `Skill.fromYaml(...)` extended with `@JsonProperty("critical_steps")
  List<CriticalStep>` parameter; passes through to the canonical ctor.

### `CriticalStep.java` (D-1.2; NEW record)

- 5 fields: `id: String`, `desc: String`, `traceCheck: String`,
  `mandatoryFor: List<String>` (canonical UC ids), `severity: Severity`.
- Nested enum `Severity { MANDATORY, ADVISORY }` with `@JsonCreator
  fromYaml(String)` that upper-cases the YAML value (`mandatory` /
  `advisory` per Skill-YAML convention) before enum lookup.
- Compact ctor normalizes `mandatoryFor` null → empty list.

### `SkillLoader.java` allowlist entries (D-2.1 / D-2.2 / D-2.3 / D-2.4)

- NEW `VALID_CRITICAL_STEP_SEVERITIES = Set.of("MANDATORY", "ADVISORY")`.
- Per-step validation block in `validate(Skill, String)`:
  - Each step's `id`, `desc`, `traceCheck` are required non-blank
    (`requireStepField` helper).
  - Step `id` must be unique within a Skill (duplicate-id guard).
  - `mandatoryFor` must be non-empty; each UC must pass
    `useCaseRegistry.isKnownUseCase(uc)` per the existing
    `applicable_use_cases` precedent.
  - `severity` must be present (Jackson `fromYaml` returns null on
    missing) and within `VALID_CRITICAL_STEP_SEVERITIES`.
- DSL syntax validation is **explicitly NOT** done in Java per contract
  §5 D-2.4 — the Python `SkillProcedureExtractor` owns DSL parsing
  (`TraceCheckDSLSyntaxError` raised at extractor construction time).
  Java preserves the `traceCheck` string verbatim.
- NO behavioural change in `SkillRegistry.select(...)` /
  `SkillGuardrailDispatcher.dispatch(...)` /
  `SkillStateBus.applyOnSkillSwitch(...)`. Verified via `git diff` on
  each file (empty).

## 5. Projection wiring details

### `ContextProjectionBuilder.java` integration point

- Location: lines 868-901 (post-edit), within the existing
  plan-aware `build(...)` method's `if (plan != null) { ... }` block.
  Inserted between `system_instruction` (line 869) and
  `escalation_policy` (line 901). Verified by
  `CriticalStepsProjectionTest.projection_criticalStepsRenderedAfterSystemInstruction_within_phasePlan`
  which walks the JSON child name order.
- Why this position: contract §5 D-3.2 specifies "immediately after the
  existing `procedure` field". `procedure` is folded into
  `systemInstruction` at `PhaseEvaluator.java:459` (`.systemInstruction(substitutePlaceholders(skill.procedure(), activeUc))`);
  the projection has no separate `procedure` field. Rendering
  immediately after `system_instruction` (which IS the procedure text
  post-substitution) is the closest match to contract intent.
- Skill resolution: `skillRegistry.select(plan.phase(), plan.useCase())`
  per existing precedent at line 1088 (`prior_skill_name` resolution).
  `Optional.ifPresent(...)` guards the empty case (registry miss → no
  key).
- Empty-array parity: `if (!skill.criticalSteps().isEmpty())` is the
  branch guard — empty list → no key added to `planNode`. This
  preserves the M2-landed prompt-composition golden tests at the byte
  level (the entire 6-production-Skill set ships with empty
  `criticalSteps` at S-Eval-2 close).

### Per-turn projection byte delta observation (S-Eval-2)

- For the 6 production Skills (all with empty `critical_steps[]`):
  **0-byte delta** vs pre-S-Eval-2 baseline. The `if (!criticalSteps.isEmpty())`
  guard prevents any byte change in the projection output. Verified
  by:
  - `CriticalStepsProjectionTest.projection_emptyCriticalSteps_omitsKeyEntirely`
    — asserts the key is absent on the empty case.
  - The pre-existing M2 prompt-composition golden tests in
    `ContextProjectionBuilderTest.java` (15 tests using the plan-aware
    `build(...)` path) all pass unchanged.
- Synthetic projection-size simulation (Python; not a Java run): a
  2-step populated `critical_steps` block adds ~171 bytes to the
  `phase_plan` JSON. For the S-Eval-3 anticipated 3-5 steps × 6
  Skills × ~80-150-char `desc`, the per-turn projection delta is
  expected to land in the proposal §5.4 estimate of 500-900 tokens
  (~2-4 KB raw bytes; tokenizer-dependent). Recorded here as
  observation for S-Eval-3 calibration; not a hard gate at S-Eval-2.

## 6. §4.1 anti-hardcode self-walk verdicts

| Q | Verdict | Justification |
|---|---|---|
| Q1 — Semantic hardcode introduced (keyword / regex / if-else / enum / per-UC matrix)? | **pass** | Sprint 43 ships NO `critical_steps` content (no `desc` strings, no `traceCheck` strings populated on any of the 6 Skills). The DSL parser at `eval_interactive/eval_interactive/scoring/skill_procedure_check.py` is the **structural mechanism that PREVENTS hardcodes** from being introduced in S-Eval-3 content. The structural-defence test `eval_interactive/tests/test_skill_procedure_dsl_parser_rejects_hardcodes.py` asserts ≥ 6 (actually 15) representative hardcode-flavoured `trace_check` strings fail parse with `TraceCheckDSLSyntaxError` (well above the contract §9 floor of 6). The §1.7 protection is the POSITIVE grammar of the parser (only the 6 frozen primitives are accepted); the blocklist is a transparency mechanism for clearer error messages on common §1.7 shapes. |
| Q2 — Tier-0 invariant claim? | **pass** | No Tier-0 invariant added. The new Tier-2 `skill_procedure_followship` gate is added at the composite-scoring layer (eval-side), not the runtime layer; it gates `case_passed` but is NOT a Tier-0 runtime invariant per `docs/runtime_freeze_and_risk_policy.md` §1/§2. Milestone §6 hard fence #2 "no new Tier-0 invariant" preserved. |
| Q3 — Could a soft signal replace a hard branch? | **pass** | The projection wiring DOES surface `critical_steps[].desc` as a soft signal to the LLM (LLM-visible procedural guidance; the LLM owns whether to act on it per §1.3). The eval-side `traceCheck` is a structural check, not a hard branch in the runtime. The Java side adds zero new semantic decision branches; the Python side adds Tier-2 evaluation as a NEW eval-side gate (the LLM doesn't see it; it's eval-only signal). |
| Q4 — Eval phrase / trace-specific phrasing / CaseSpec-id encoded? | **pass** | No eval phrase / CaseSpec id / trace-specific phrasing is referenced anywhere in the S-Eval-2 diff. The DSL parser primitives reference TOOL NAMES (`search_knowledge`, `resolve_article`, `create_case_controlled`, etc. — canonical runtime tool registry) and SLOT NAMES (`ad_id`, `appeal_reason` — canonical intake-field registry), which are runtime contractual surfaces, not eval-specific. The test data in `test_skill_procedure_dsl_parser_rejects_hardcodes.py` is illustrative (`'refund'`, `'appeal'`, `'sorry'` etc.) and lives only in the NEGATIVE test file; it never reaches runtime or LLM. |
| Q5 — LLM ownership shrunk (§1.3 surfaces moved to Java)? | **pass** | The LLM's per-turn projection GAINS a soft signal slot (`critical_steps[].desc`). It does NOT lose any §1.3 surface. The LLM still owns user goal, issue relation, UC hypothesis, drift / topic shift, next action, escalation posture, response strategy, customer-facing wording. The eval-side Tier-2 check observes the LLM's behavioural trace; it does NOT make the LLM's decisions. The S-Eval-2 wiring strictly augments LLM-owned territory with a soft signal; nothing shrinks. |
| Q6 — Prompt if-else added? | **pass** | No prompt change in S-Eval-2 (no `system_prompt.txt` edit; no Skill `procedure` text edit). The `critical_steps[].desc` payload is principle-level guidance per the proposal §5.3 standard table (an exemplar like "Retrieve a knowledge article before answering" is soft narrative, not "IF user.message.contains(...)"). S-Eval-2 ships no `desc` content; the structural rejection of `message.contains(...)` etc. in the DSL parser is the protection for S-Eval-3 content. |
| Q7 — Tool schema / capability / PII / grounding floor preserved? | **pass** | No tool schema change; no capability / permission change; no PII-handling change; FAQ grounding contract unchanged. Tier-0 safety floor checks (`no_pii_leakage`, `no_critical_policy_violation`, `escalation_compliance`, `phase_transition_validity`) all remain critical-severity (S-Eval-1 D-2.5 default). No `tool-policy.yaml` edit. |
| Q8 — Generalization coverage (target / neighbor / negative / shadow)? | **pass** | Per §8 below. Target: NEW schema field + extractor + projection wiring (all 5 outcomes shipped + tested). Neighbor: 6 Skill YAMLs + 14 smoke + 159 anchor + 12 anchor_outcome + 12 case-family + 1 Alice all load and run unchanged through extended SkillLoader (verified by Java + Python loaders). Negative: 8 SkillLoader negative cases + 15 DSL-parser-rejects-hardcodes cases (well above ≥6 floor). Shadow: S-Eval-3 + S-Eval-4 + S-Eval-5 + M3-Eval close cumulatively exercise; S-Eval-2 does NOT touch shadow CaseSpecs. |
| Q9 — Rollback / sunset plan if temporary? | **N/A** | S-Eval-2 changes are intended permanent (the schema, DSL parser, and Tier-2 gate are the foundational substrate for the M3-Eval pyramid; future milestones build on top, they don't roll back). |

No `concern` / `fail` verdict on any of Q1-Q9; no STOP signal fired
during implementation.

## 7. Open questions for deliver-agent + human at close

- **OQ-S43.1** (DSL parser blocklist surface): the parser ships with an
  illustrative `_HARDCODE_BLOCKLIST` set (`re.match`,
  `.matches(...)`, `keyword_match`, `user_intent in [...]`, etc.) that
  produces named errors for common §1.7 shapes. The POSITIVE grammar
  (only the 6 frozen primitives accepted) is the structural guarantee;
  the blocklist is a transparency aid. Confirm with Codex at M3-Eval
  close that the dual layer (positive grammar + illustrative
  blocklist) is acceptable, or whether the blocklist should be removed
  in favor of a generic "unknown primitive" error for everything
  outside the 6 (uniform error surface, slightly worse §1.7 dev
  experience).

- **OQ-S43.2** (projection slot — `id` inclusion vs `desc` only):
  Outcome 3 chose list-of-objects `[{id, desc}]` over list-of-strings
  per choice (b) rationale. The byte overhead is small but real (~30
  chars per step). If S-Eval-3 content surfaces no need to
  cross-reference steps by `id` (a `desc` like "after the
  `search_before_answer` step" can hard-code the id by string), the
  `id` field is dead weight. Defer the call to S-Eval-3 author
  feedback; if useful, document in M3-Eval close as a calibration
  finding.

- **OQ-S43.3** (Python Skill loader graduation): the embedded
  `load_skills_from_dir` helper in `skill_procedure_check.py` is
  minimum-viable per choice (e). If S-Eval-3 / S-Eval-4 surface
  additional consumers (e.g., a per-Skill linter, a populated-coverage
  audit), graduate the helper to a separate
  `eval_interactive/eval_interactive/skills/loader.py` module at that
  point. Not a blocker for S-Eval-2 close.

- **OQ-S43.4** (`tool_event_seq` unsuccessful-dispatch semantics): the
  extractor ignores `success=false` dispatches when computing
  `first_seq_index_for(tool)` (verified by
  `test_unsuccessful_dispatch_is_ignored`). This is conservative — a
  rejected `create_case_controlled` does NOT count as "case created
  before handover". Confirm with Codex at M3-Eval close that this
  interpretation matches the §5.3 standard intent (a step like "case
  created before handover" requires the case CREATION to have
  succeeded, not merely been attempted).

- **OQ-S43.5** (`session.<flag>_present` fallback to bare flag): the
  evaluator first checks `session.<flag>_present` (explicit boolean
  key), then falls back to truthiness of `session.<flag>` (bare name).
  This double-fallback matches two observed trace shapes but expands
  the §1.7 surface slightly — a flag named `account_status` truthy
  would satisfy `session.account_status_present`. If S-Eval-3 author
  feedback shows this is too permissive, tighten to the explicit
  `_present` key only at S-Eval-3 fix-iteration.

## 8. Generalization coverage (filled per §8 stanza)

- **Target**: NEW `Skill.critical_steps` field + `CriticalStep` Java
  record + `CriticalStep.Severity` enum + `SkillLoader`
  `VALID_CRITICAL_STEP_SEVERITIES` allowlist + per-step required-field
  + UC-membership + duplicate-id + severity-range validation +
  `ContextProjectionBuilder` `phase_plan.critical_steps` projection
  slot + NEW Python `SkillProcedureExtractor` + 6-primitive
  `trace_check` DSL parser with §1.7 structural defence + `Tier2Result`
  composite adapter + Tier-2 gate band in `composite.py`.
- **Neighbor**: existing 14 smoke + **159 anchor** + 12 anchor_outcome
  (S-Eval-1) + 12 case-family directories + 1 Alice bad case all load
  and run unchanged at the composite-gate level (extractor returns
  empty list for empty `critical_steps[]` → Tier-2 default = PASS /
  advisory → no `case_passed` flip). All 6 Skill YAMLs load unchanged
  via extended Java SkillLoader (empty default for `critical_steps:`
  block) and via the new Python `load_skills_from_dir` (also empty).
  Existing 12 `test_composite_gate.py` tests pass unchanged (Tier-2
  default empty adds zero gate effect for callers that have not
  updated to pass `tier2_result`).
- **Negative**:
  - 8 SkillLoader negative cases: missing `id`, blank `desc`, missing
    `trace_check`, empty `mandatory_for`, unknown UC in
    `mandatory_for`, missing `severity`, invalid `severity` enum
    value, duplicate step id within a Skill.
  - **§1.7 structural defence**: 15 representative hardcode-flavoured
    `trace_check` strings rejected at parse time by
    `parse_trace_check` (parameterized test). Includes
    `message.contains(...)`, `re.search(...)`, `re.match(...)`,
    `keyword_match(...)`, `user_intent in [...]`, string-suffix
    checks, and combinator-wrapping-forbidden-primitive shapes.
  - 10 malformed-syntax inputs (unbalanced parens, missing args,
    wrong operator, unknown primitive head) raise
    `TraceCheckDSLSyntaxError` (same error class as §1.7 rejections).
- **Shadow**: S-Eval-3 (next sub-sprint) populates `critical_steps`
  content on the 6 Skills under per-sub-sprint Codex review per §4.3
  trigger #2; S-Eval-4 expands the bad-case suite (10-12 new bad
  cases) and exercises Tier-2 against the populated content; S-Eval-5
  closes the 4 R-items; M3-Eval close runs the full bad-case suite
  manual review against the now-populated Tier-2 surface. S-Eval-2
  does NOT touch any case under `eval_interactive/case_specs_shadow/`.

## 9. Validation runs

### Java test suite

```
$ mvn test -q
...
[ERROR] Failures:
[ERROR]   SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly:53 ACTIVE-UC TIEBREAKER must be tagged with the Sprint 6 anchor ==> expected: <true> but was: <false>
[ERROR] Tests run: 1163, Failures: 1, Errors: 0, Skipped: 2
```

**1163 total / 1 inherited failure / 0 errors / 2 skipped** (up from
pre-S-Eval-2 baseline of `1144 / 1-inherited / 0 / 2`).

- **+19 new tests**: 14 in `SkillCriticalStepsLoadingTest` + 5 in
  `CriticalStepsProjectionTest`.
- **Inherited failure unchanged**: `SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly:53`
  persists per M2-close baseline status quo (Sprint 24-era working-tree
  mod; documented in S-Eval-1 §9 and Sprint 41 handoff). No new
  failures introduced by S-Eval-2.

### Python test suite

```
$ cd eval_interactive && uv run pytest
...
======================== 9 failed, 392 passed in 11.75s ========================
```

**392 passed / 9 pre-existing failed** (up from pre-S-Eval-2 baseline
of `320 passed / 9 failed`).

- **+72 new passing tests**: 30 in `test_skill_procedure_extractor.py`
  + 36 in `test_skill_procedure_dsl_parser_rejects_hardcodes.py` + 6
  new in `test_composite_gate.py::TestTier2Gate`.
- **9 pre-existing failures unchanged** (identical to S-Eval-1 close
  baseline; corroborated by walking the failure list):
  - `tests/scoring/test_escalation_enum_sync.py` × 2 (deleted
    `customer_service_tool_spec_v0_2.yaml` reference; S-Eval-1
    surfaced R-item `R-stale-test-escalation-enum-sync-v0_2-to-v0_3-migration`).
  - `tests/regression/test_corpus_lint.py` × 5 (cross-repo PYTHONPATH
    leak; S-Eval-1 §9 documented as pre-existing infra issue).
  - `tests/regression/test_case_spec_overrides.py` × 2 (17-vs-15
    override count drift; documented in milestone notes).

### §1.7 structural defence test (CRITICAL hard gate per dev prompt §9.5)

```
$ uv run pytest tests/test_skill_procedure_dsl_parser_rejects_hardcodes.py -v
...
========================= 36 passed in 0.08s =========================
```

**All 15 hardcode-flavoured `trace_check` strings rejected at parse
time** with `TraceCheckDSLSyntaxError`. Floor of ≥ 6 (contract §9)
exceeded by ~2.5×. The 11 canonical-form positive-grammar tests pass
(positive surface preserved); 10 malformed-syntax tests raise the
same exception class (uniform error surface).

### Backward-compat fixture load

```
$ uv run python -c "from eval_interactive.case_spec.loader import load_case_specs; ..."
{'smoke': 14, 'anchor': 159, 'anchor_outcome': 12, 'bad_cases': 1,
 'case_families_dirs': 12, 'case_families_total': 9}
```

All 14 smoke + 159 anchor + 12 anchor_outcome + 1 Alice + 12
case-family directories load unchanged under the loosened schema
(S-Eval-1) and the new Tier-2 path (S-Eval-2; empty critical_steps →
Tier-2 PASS / advisory default → no gate effect).

```
$ uv run python -c "from eval_interactive.scoring.skill_procedure_check import load_skills_from_dir; ..."
loaded 6 Skills
  confirm: critical_steps=0
  discover_triage: critical_steps=0
  escalate: critical_steps=0
  resolve_faq_grounded_answer: critical_steps=0
  resolve_intake_collect_and_handover: critical_steps=0
  terminal: critical_steps=0
```

All 6 production Skills load via the NEW Python embedded loader with
empty `critical_steps` (S-Eval-2 ships NO content; S-Eval-3 populates
under per-sub-sprint Codex review).

### Projection token-cost observation

**0-byte delta** for empty `critical_steps[]` (S-Eval-2 actual on the
6 production Skills). Verified by `CriticalStepsProjectionTest.projection_emptyCriticalSteps_omitsKeyEntirely`
+ pre-existing M2 prompt-composition golden tests passing unchanged.
Synthetic projection-size simulation for an S-Eval-3-style 2-step
populated block: +171 bytes for the JSON payload (~50-60 tokens).
Recorded as observation for S-Eval-3 calibration; M3-Eval-level
acceptance bar per `docs/milestone_objective.md` §5 is "≤ 1000 tokens
per turn" (proposal §5.4 estimate 500-900).

## 10. Contract drift

No drift from the S-Eval-2 contract (sprint_objective.md §1-§12). All
five §2 outcomes shipped per §5 (Files in scope); no file under §6
(Files NOT in scope) touched. No STOP signal (§10 #1-#8) fired during
implementation.

One **§7-c (pre-existing baseline)** observation, identical to
S-Eval-1 close §10 disposition:

- The 9 pre-existing Python test failures persist unchanged from
  S-Eval-1 baseline. None introduced or worsened by S-Eval-2. Three
  distinct root causes (per S-Eval-1 §9 corrected analysis):
  (a) 2 in `test_escalation_enum_sync.py` reference the deleted
  `v0_2.yaml` — S-Eval-1 surfaced `R-stale-test-escalation-enum-sync-v0_2-to-v0_3-migration`
  for follow-on; (b) 5 in `test_corpus_lint.py` hit a cross-repo
  PYTHONPATH leak; (c) 2 in `test_case_spec_overrides.py` reflect the
  17-vs-15 override count drift. S-Eval-2 introduces ZERO new test
  failures.

## 11. Bundle policy honored

Dev ships **ONE bundle commit** containing only the files listed in
§3 (6 Java + 5 Python + 1 handoff = 12 files).

**Dev did NOT stage** any deliver-agent territory file per
`feedback_commit_at_end_bundles_deliver_artefacts.md` and contract §7:

- `docs/sprint_objective.md` (live; deliver-agent + human archive at
  close to `docs/sprints/sprint-043-objective.md`).
- `docs/milestone_objective.md` (live M3-Eval; deliver-agent territory).
- `docs/10-handoff.md` §1 lead refresh (deliver-agent territory).
- `docs/codex-findings.md` (review-agent territory; scaffold).
- `docs/action_bank.md` (pre-existing 8-line modification from
  deliver-agent territory; not Sprint-43-attributable. Sprint 43
  close-action row append optional at deliver-agent + human close-out
  bundle).
- `compact/sprint-043-dev-prompt.md` (deliver-agent territory).
- The two pre-existing working-tree deletions
  (`docs/customer_service_tool_spec_v0_2.md` and `.yaml`) were already
  in `git status` BEFORE S-Eval-2 dev began (per S-Eval-1 close §11);
  not in the S-Eval-2 bundle.

Suggested commit message:

```
sprint 43 / S-Eval-2: Skill critical_steps schema + extractor + projection wiring (NEW M3-Eval sub-sprint 2)
```

## 12. Closure verdict

*This section is empty until S-Eval-2 close. Deliver-agent + human
fill at close per `feedback_handoff_verdict_section_delegation.md`.*
