---
title: Sprint 52 / M5 S3 — Projection audit (C1) + Skill-driven convergence (C2) — dev handoff
doc_tier: sprint-archive
status: current
implementation_status: implemented
source_of_truth: this file (sub-sprint dev archive); ContextProjectionBuilder.java + resolve_faq_grounded_answer.yaml (code)
last_reviewed: 2026-05-25
review_cadence: ad hoc
supersedes: []
superseded_by: null
notes: >
  Sub-sprint dev-authored handoff. THIRD sub-sprint of Milestone M5 —
  Observability Coherence and the milestone's ONLY semantic surface (§7
  REQUIRED). C1 (field consumption matrix) DELIVERED + reviewed via human
  AskUserQuestion at C1 review BEFORE any C2 projection-field change.
  C2 #3 (moderation_context stale decl strip) + C2 #4 (Skill-registry-
  driven tool_schemas base) LANDED. C2 #2 (Skill-declared context-key
  gating) + C2 #5 (state_inheritance.soft_signal_via_projection gating)
  STOP-AND-SURFACED to deliver-agent per matrix HIGH-risk analysis +
  human directive at C1 review. OQ-S51.2 disposition: option (a) leave-
  as-is + note per human directive. Real-LLM bad-case rerun STOP-AND-
  SURFACED to deliver-agent for S3 close per OQ-S51.1 established pattern
  (dev sandbox has LLM keys but the running backend is on the pre-S3
  build; restart-and-rerun deferred to deliver-agent close). §12
  reserved for deliver-agent + human at sub-sprint close.
---

# Sprint 52 / M5 S3 — Projection audit (C1) + Skill-driven convergence (C2) dev handoff

## 1. Goal and outcome

S3 first **produces the C1 field consumption matrix** for the per-turn
projection — a zero-risk diagnostic enumerating every emitted projection
field × the consumers that depend on it (system_prompt /
eval-trace-contract-validator / eval-scoring / `buildDriftAndTaskHistory`
/ Skill declaration / sampled real-LLM trace). The matrix is the gate
for every C2 change; no projection field is touched in S3 unless its
matrix row shows no consumer that would break.

S3 then **converges the projection to be Skill-driven** where M2 left it
UC-driven, subject to the matrix evidence + human approval at C1 review
(via `AskUserQuestion`, 2026-05-25). Landed in S3:

- **C2 #3** — strip the stale `moderation_context` entry from
  `resolve_faq_grounded_answer.yaml:23` (zero runtime effect because
  `PhasePlan.requiredContextKeys()` has no runtime consumer; the
  declaration was a M2-era artifact and the projection never emitted it).
- **C2 #4** — replace the UC-driven `tool_schemas` base in
  `ContextProjectionBuilder.buildProjection(...)` with a
  Skill-registry-derived equivalent (`SkillRegistry.select(phase, uc)`
  → `Skill.toolsRequired()`), falling back to the pre-M2 UC-driven
  palette only when no Skill maps the tuple. The run-loop path's
  `:845-863` plan-filtered overwrite remains as defense-in-depth.

STOP-and-surfaced (HIGH semantic risk per matrix, requires real-LLM
rerun gate not currently available in dev sandbox):

- **C2 #2** — Skill-declared context-key gating
  (`form_context`/`customer_context`/`listing_context`/etc.). The cross-
  Skill DECL coverage is not provably intentional; gating without a
  Skill-declaration audit would silently drop signals the LLM relies on.
- **C2 #5** — `state_inheritance.soft_signal_via_projection` gating for
  `alternate_candidate_use_cases` / `discover_disambiguation_signals` /
  `prior_use_case_carry`. Low data risk (signals null-when-N/A for
  non-declaring Skills) but real-LLM rerun is the only proof.

The real-LLM bad-case rerun (the §5.6 evidence gate for C2's semantic
preservation claim) is STOP-AND-SURFACED to deliver-agent for dispatch
at S3 close per the OQ-S51.1 established pattern.

LLM-visible semantic impact of LANDED changes: **byte-identical for the
agentic run-loop primary path**. C2 #3 has zero runtime consumer
(declaration only). C2 #4's run-loop end-state is byte-identical because
the existing `:845-863` overwrite was already `plan.allowedTools()` =
`skill.toolsRequired()` — the change only affects the cold-edge legacy
`buildProjection(...)` direct-LLM-invocation paths
(`PhaseEvaluator.evaluateResolveFaq*` + `ControlKernel.recordTurn` for
non-run-loop turns).

## 2. Scope (per `docs/sprint_objective.md` #1-#6)

### #1 — Field consumption matrix (DELIVERED + reviewed at C1)

Authored `docs/diagnostics/m5-s3-projection-consumption-map.md` (321
lines), covering every emitted projection field grouped by source
builder. Each row labels REQ (raises/breaks if absent) · READ (consumed
but tolerates absence) · DECL (declaration only — not yet wired) ·
PRESENT (sampled) · — (no observed consumer) for the five consumer
columns. Source-of-truth citations:

- system_prompt → `server/src/main/resources/prompts/system_prompt.txt`
- eval trace contract validator → `eval_interactive/eval_interactive/trace/collector.py:108-144` (`REQUIRED_*_FIELDS`, `CONDITIONAL_SESSION_FIELDS`, `TOOL_SPECIFIC_REQUIRED_ARGS`)
- eval scoring → `eval_interactive/eval_interactive/scoring/skill_procedure_check.py:485-537` (Tier-2 procedure followship reads `projection.session.*` / `accumulated_tool_results` / `intake_state`) + `eval_interactive/eval_interactive/batch/executor.py:524-571` (per_turn_trace.projection comes from `BotTurn.projected_context` — the OQ-S51.2 single column with overlay)
- `buildDriftAndTaskHistory` → `ContextProjectionBuilder.java:1265-1305`
- Skill declarations → `server/src/main/resources/skills/*.yaml`
- LLM raw trace → OQ-S51.1 eyeball session `92a5c7c7-2054-4b54-84f0-0dc41a95703c` per S2 handoff §12.1-§12.2

Matrix §4 summary (C2 disposition input) routes each item to LOW /
MEDIUM / HIGH risk + the convergence path it should take if landed; §5
gives the recommended close shape. Matrix delivered as a committed
artifact under `docs/diagnostics/`.

### #1a — OQ-S51.2 row (consumed from S2; mapped, NOT auto-changed)

Matrix §6 maps the FAQ-grounding overlay (`mergeFaqGroundingIntoProjection`
applied to `result.lastProjection()` in
`ControlKernel.recordRunResult:2029-2031`) against six consumer surfaces:

- **LLM actually saw** raw per-step projection (`bot_turn_llm_calls.projection`,
  S2-added) — overlay-free.
- **Eval trace contract / scoring / DT / admin UI** see the merged single
  column (`bot_turns.projected_context`) — overlay-merged.
- `Sprint141FaqGroundingTracePersistenceTest` is the only test that
  directly asserts the overlay is durable — load-bearing.

The matrix flags this is benign + pre-existing for S2 (no scorer reads
`projection.faq_grounding`; no contract field lives in the overlay).
Human disposition at C1 review (`AskUserQuestion`, 2026-05-25): **option
(a) leave-as-is + note** — the divergence is benign, post-S2 the
admin UI exposes both views, and three rational future dispositions
(leave / move-to-per-step / dedicated-column) are documented in matrix
§6 for a future R-item if the human elects to address it.

### #2 — Skill-declared context-key gating — STOP-AND-SURFACED

C1 matrix §4 row #2 + §5 conclude this convergence would silently drop
LLM-visible signals from non-declaring Skills (`discover_triage` does
not declare `customer_context`; only `resolve_faq_grounded_answer`
declares `listing_context`; etc.) — the Skill DECL coverage is not
provably intentional and gating without a Skill-declaration audit +
real-LLM rerun would regress UC-G/H listing flows or DISCOVER
clarification quality.

Per the sprint-contract hard fence: "STOP and surface if any in-scope
convergence (#2-#5) cannot be done without removing an LLM-visible
signal". STOP-surfaced via `AskUserQuestion` at C1 review; human
directive: "Land #3 + #4 only; STOP-surface #2 + #5".

**Deferred to:** S4 (conditional on deliver-agent + human planning) or a
follow-on M5/M6 sub-sprint where the deliver-agent can run the real-LLM
rerun at close.

### #3 — `moderation_context` coherence gap — LANDED

C1 matrix §3.R + §4 row #3 confirm zero consumer reads
`moderation_context`: system_prompt doesn't mention, EC doesn't require,
ES doesn't read, DT doesn't walk, no other Skill declares it, no
sampled LLM trace contains it. The declaration in
`resolve_faq_grounded_answer.yaml:23` was a M2-era artifact with no
producer and no consumer.

Edit (`git diff` numstat `0/1`): stripped the line from
`required_context_keys`. No SkillLoader validator update needed
(`required_context_keys` is free-form, not enum-validated). New test
`Sprint52ProjectionSkillDrivenTest.productionFaqSkill_doesNotDeclareModerationContext_postC2No3`
loads the real YAML and guards against re-introduction.

LLM-visible delta: **zero** (slot was never emitted; declaration is not
runtime-read).

### #4 — Remove computed-then-discarded UC-driven `tool_schemas` base — LANDED

C1 matrix §3.K + §4 row #4 establish the legacy `buildProjection(...)`
direct callers (`PhaseEvaluator.evaluateResolveFaq*` +
`ControlKernel.recordTurn`) DO consume the base path (it is not dead
code), so a blind delete would break the persisted `tool_schemas` for
non-run-loop turns. The safe convergence — registry/Skill-driven, no
per-UC if-else — is to replace the UC-driven base with a
SkillRegistry-derived equivalent.

Implementation (`ContextProjectionBuilder.java`, `git diff` numstat
`59/13`):

- Replaced the `if (activeUc != null) { for (toolPolicyEnforcer.getVisibleToolsForUc(activeUc)) { ... } }`
  block (`:621-633`) with `for (resolveProjectedToolNames(session, activeUc)) { ... }`.
- New private helper `resolveProjectedToolNames(BotSession, String)`:
  - Tries `skillRegistry.select(phase, activeUseCase)`; if present,
    returns `Skill.toolsRequired()` (the M2-correct single source of
    truth shared with `PhasePlan.allowedTools()`).
  - Falls back to `toolPolicyEnforcer.getVisibleToolsForUc(activeUseCase)`
    only when no Skill maps the tuple (defensive — mirrors pre-M2
    behaviour for legacy/unmapped tuples). When `activeUseCase` is also
    null, returns `Collections.emptyList()`.
- Added `Optional` import.
- The run-loop `build(...)`'s `:845-863` plan-filtered overwrite
  remains as defense-in-depth. For mapped Skills (the M2 steady state
  on all 6 production Skills) it now writes the same set the base
  emitted — observable end-state identical.

Run-loop primary path's LLM-visible `tool_schemas`: **byte-identical**.
Legacy `buildProjection(...)` paths' LLM-visible `tool_schemas`: now
the Skill's `toolsRequired` list (M2-correct) instead of the broader
UC-allowed palette. Real-LLM rerun is the proof for the legacy paths;
STOP-surfaced for deliver-agent dispatch at close.

§1.7 boundary verified: no per-UC if-else, no keyword / regex / enum
branch added. The path is registry/Skill-driven; the UC-driven fallback
is a single defensive call mirroring pre-M2 behaviour for unmapped
tuples — semantic ownership stays with the LLM.

### #5 — `state_inheritance.soft_signal_via_projection` gating — STOP-AND-SURFACED

C1 matrix §3.E + §4 row #5 establish convergence is correct-shaped
(registry-driven via `Skill.stateInheritance().softSignalViaProjection()`)
but the §5.6 evidence gate is a real-LLM rerun on bad cases that exercise
DISCOVER→RESOLVE (alice / cs011 / cs012 / wmkb) — and the dev sandbox
backend is on the pre-S3 build (see §6 below). Per the sprint-contract
"STOP and surface if a convergence needs a removed LLM-visible signal,
or if the real-LLM rerun regresses the bad-case distribution" fence,
deferred per human directive at C1 review.

**Deferred to:** S4 or deliver-agent-led close with real-LLM rerun.

### #6 — Tests + real-LLM evidence gate

Java wiring/rendering tests authored (LANDED-changes coverage):

- NEW `server/src/test/java/com/gumtree/csagent/service/runtime/Sprint52ProjectionSkillDrivenTest.java`
  (336 lines, 7 tests, all PASS):
  - `buildProjection_skillMatched_usesSkillToolsRequired_notUcPalette` —
    asserts buildProjection's tool_schemas comes from the resolved
    Skill's `toolsRequired`, NOT from `toolPolicyEnforcer.getVisibleToolsForUc`
    (verified via Mockito `verify(toolPolicyEnforcer, never()).getVisibleToolsForUc(anyString())`).
  - `buildProjection_skillUnmatched_fallsBackToUcDrivenPalette` —
    asserts the UC-driven fallback fires when SkillRegistry.select
    returns Optional.empty().
  - `buildProjection_nullActiveUc_noWildcardSkill_emitsEmptyToolSchemas` —
    null activeUc + no wildcard Skill yields empty tool_schemas (preserves
    pre-S3 behaviour for unclassified DISCOVER turns).
  - `buildProjection_nullActiveUc_wildcardSkillMatched_usesSkillTools` —
    wildcard Skill match for (DISCOVER, null) sources tool_schemas from
    Skill.toolsRequired (real DISCOVER pre-classification shape).
  - `planAwareBuild_skillMatched_skillToolsEqualPlanAllowedTools_overwriteIsNoOpForNames` —
    when Skill.toolsRequired == plan.allowedTools (M2 steady state), the
    end-state tool_schemas equals the Skill-driven set; defense-in-depth
    of the :845-863 overwrite holds.
  - `buildProjection_faqSkillDoesNotDeclareModerationContext_projectionDoesNotEmitIt` —
    guards against a regression that emits moderation_context.
  - `productionFaqSkill_doesNotDeclareModerationContext_postC2No3` —
    loads the real YAML via SkillLoader + UseCaseRegistryService and
    asserts `required_context_keys` no longer contains
    moderation_context.

- UPDATE `server/src/test/java/com/gumtree/csagent/service/runtime/PhaseEvaluatorResolveSkillIntegrationTest.java`
  `assertGoldenFaq` golden (7 FAQ-UC tests' shared helper): updated
  `requiredContextKeys` golden from 4 keys (form_context, customer_context,
  listing_context, moderation_context) to 3 (post-C2 #3 strip). Diff
  numstat `1/2`. This is an intentional golden update — the test was
  enforcing the stale declaration; matrix §3.R + §4 row #3 justify the
  strip.

**Real-LLM bad-case rerun (the C2 #6 behaviour-risk gate):**
STOP-AND-SURFACED to deliver-agent for S3 close per OQ-S51.1 established
pattern. Backend at `localhost:8080` is up but on the pre-S3 build (my
C2 #4 Java change is uncommitted); restarting it would disrupt the
user's session. Per human directive at C1 review: deliver-agent
dispatches the rerun at S3 close — Java baseline is the holding gate
for landed-in-S3 changes.

## 3. §4.1 anti-hardcode self-walk (per §7 stanza + §4.3 per-sub-sprint Codex review at S3 close)

The S3 deliver-agent + human dispatch a per-sub-sprint Codex review at
close per `milestone_objective.md` §8 + `iteration_governance.md` §4.3.
Self-walk below is the dev-agent input for that review.

1. **Does the PR add a keyword / regex / if-else / enum / per-UC matrix
   for a semantic decision?** No. C2 #3 strips a declaration (zero
   runtime effect). C2 #4 REPLACES a UC-driven call (`getVisibleToolsForUc`)
   with a Skill-registry-driven call (`SkillRegistry.select(...).map(Skill::toolsRequired)`),
   with a single defensive fallback to the pre-M2 UC-driven path for
   unmapped tuples. No new keyword / regex / enum / per-UC branch.
2. **Tier-0 invariant justification?** N/A — no new Tier-0 invariant
   added. Projection sits inside Runtime's "trace and eval contract"
   (§1.4); the LLM owns soft-signal interpretation (§1.3).
3. **Could the same outcome be achieved by projecting a soft signal?**
   The change IS toward soft-signal projection — moving from UC-driven
   hard branching to Skill-declaration-driven projection (the
   declarations themselves are registry data, LLM-soft).
4. **Encode visible-eval case text / trace-specific phrasing / CaseSpec
   id into runtime, prompt, or judge config?** No. Zero touch to
   `eval_interactive/case_specs/**`, `composite.py`, judge fixtures, or
   prompts.
5. **Does the change move semantic ownership from LLM to Java?** No.
   The opposite: removes a Java-side UC palette in favour of Skill
   declarations the LLM operates within. Semantic ownership stays with
   the LLM (`Skill.procedure` is LLM-soft; `Skill.toolsRequired` is the
   tool whitelist, which §1.4 explicitly assigns to Runtime — the
   change consolidates that ownership at a single Skill registry source).
6. **Add an if-else block to the prompt instead of principle-level
   guidance?** No prompt change.
7. **Preserve tool schema, capability / permission boundary, PII /
   safety floor, and grounding floor?** Yes. `tool_schemas` set for
   mapped Skills is byte-identical to the post-`:845-863` overwrite end
   state (the M2 steady state). PII redaction unchanged. Grounding
   floor (FAQ-grounding overlay path) UNTOUCHED — explicitly preserved
   per OQ-S51.2 fence + human directive. Tier-0 safety hard_checks
   schema unchanged.
8. **Ship generalization eval coverage — target, neighbor, negative,
   shadow?** Java wiring/rendering coverage shipped (7 tests cover
   Skill-match / Skill-unmatched-fallback / null-uc-no-wildcard / null-
   uc-wildcard-match / steady-state-overwrite / C2-#3-no-emit / C2-#3-
   YAML-decl). Real-LLM bad-case rerun (the semantic-preservation gate)
   STOP-surfaced to deliver-agent at close — bad case distribution
   PASS×5 / IMPROVING×4 / FAIL×3 / OOSR×0 is the M4-close baseline that
   must hold.
9. **If temporary, rollback / sunset plan?** Not temporary. C2 #3 +
   C2 #4 are the M2-correct steady state per the C1 matrix; C2 #2 + #5
   are STOP-surfaced for future sub-sprints.

**Expected Codex verdict at S3 close:** `approve` IF the real-LLM
bad-case rerun holds the M4-close distribution. STOP-surfaced items
(#2/#5) carry no PR-level §4.1 concern (they were not landed).

## 4. Hard fences honoured

Per `docs/sprint_objective.md` "Hard fences / STOP conditions":

- ✅ **C1 BEFORE C2** — matrix `docs/diagnostics/m5-s3-projection-consumption-map.md`
  committed AND reviewed via `AskUserQuestion` BEFORE any C2 projection-
  field change. C2 #3 + #4 landed only after human directive.
- ✅ **Registry/Skill-driven — no per-UC if-else** — C2 #4 uses
  `SkillRegistry.select(phase, useCase)`; no keyword/regex/enum/per-UC
  matrix added. C2 #3 strips a registry declaration.
- ✅ **No change to LLM-visible semantic information for the agentic
  run-loop primary path** — C2 #3 has zero runtime effect; C2 #4's
  run-loop end-state is byte-identical to pre-S3 (the `:845-863`
  plan-filtered overwrite was already `plan.allowedTools()` =
  `skill.toolsRequired()`). The legacy `buildProjection(...)` paths
  see Skill-narrowed tool palette — real-LLM rerun is the proof,
  STOP-surfaced.
- ✅ **No `escalation_reason` enum / tool-schema / PII / safety /
  grounding floor change** — none touched. Zero edits under
  `eval_interactive/case_specs/**`, `composite.py`, eval fixtures.
- ✅ **OQ-S51.2 FAQ-grounding overlay UNTOUCHED** — explicitly
  preserved per human option-(a) disposition.
- ✅ **S2 `bot_turn_llm_calls` schema + `/trace` endpoint shape
  UNTOUCHED** — S3 is read-only consume of S2's per-step projections
  (in the C1 matrix evidence column).
- ✅ **STOP-and-surface for #2 / #5** — both items STOP-surfaced
  explicitly per the §4 row + §5 matrix recommendation and human
  directive at C1 review.

## 5. Files touched + footprint

Numstat (per `git diff --numstat` at sprint close, against HEAD `cf9c120`):

| File | Insertions | Deletions | Kind |
|---|---|---|---|
| `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java` | 59 | 13 | C2 #4 + helper |
| `server/src/main/resources/skills/resolve_faq_grounded_answer.yaml` | 0 | 1 | C2 #3 |
| `server/src/test/java/com/gumtree/csagent/service/runtime/PhaseEvaluatorResolveSkillIntegrationTest.java` | 1 | 2 | golden update (post-C2 #3) |
| `docs/diagnostics/m5-s3-projection-consumption-map.md` | 321 | 0 | NEW (C1 deliverable) |
| `server/src/test/java/com/gumtree/csagent/service/runtime/Sprint52ProjectionSkillDrivenTest.java` | 336 | 0 | NEW (7 wiring/rendering tests) |
| `docs/sprints/sprint-052-handoff.md` | (this file) | 0 | NEW (dev handoff) |

S3 scope only: no edits under `eval_interactive/`, `data/`, `ui/`,
`config/**`, `composite.py`, or any other surface beyond the four named
in the sprint-contract Commit-discipline §.

## 6. Baselines + gates

- **Java**: `mvn test -B` → `Tests run: 1172, Failures: 1, Errors: 0,
  Skipped: 2`.
  - **`+7` vs the `1165` baseline** = the 7 new Sprint52ProjectionSkillDrivenTest
    tests.
  - The lone failure is the **inherited
    `SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly`**
    (OQ-S41.5 STATUS QUO, persists from before Sprint 24). **No NEW
    Java regression.**
- **Python**: `cd eval_interactive && uv run python -m pytest --tb=no
  -q` → `3 failed, 486 passed`. **UNCHANGED vs baseline** (S3 did not
  touch eval-harness Python).
  - The 3 failures are env-specific per OQ-S47.3:
    `tests/regression/test_case_spec_overrides.py::test_v2_schema_loads_cleanly`,
    `test_smoke_review_report_tracks_smoke_set_and_overrides`, and
    `tests/regression/test_corpus_lint.py::test_full_corpus_lints_clean_with_smoke_subset_flag`.
- **Real-LLM bad-case rerun (the C2 #6 evidence gate)**: **STOP-AND-
  SURFACED to deliver-agent for S3 close** per OQ-S51.1 pattern. Backend
  at `localhost:8080` is UP but on the pre-S3 build; restarting would
  disrupt the user's session. Risk profile (matrix §4 row #3 + #4): C2
  #3 has zero runtime consumer (declaration only); C2 #4's run-loop
  primary path is byte-identical (the `:845-863` overwrite was already
  Skill-driven). Legacy `buildProjection(...)` paths are cold-edge.
  Expected M4-close distribution: **PASS×5** (cs001, cs014, cs029,
  cs066, fg5q) + **IMPROVING×4** (alice, cs011, cs012, wmkb) +
  **FAIL×3** (cs015, cs095, iwzx) + **OOSR×0**. A regression =
  in-flight downgrade → revert C2 #4 and S3 closes C1-only (per
  contract's "C1 alone is a valid partial close" clause).
- **Tier-0 safety floor + grounding floor**: untouched. No edits to
  `FaqGroundingDiagnostics`, `SourceEvidenceLineage`, `FaqOutputClassifier`,
  or the `mergeFaqGroundingIntoProjection` overlay.
- **Codex §4.1 anti-hardcode kernel**: §3 self-walk above expected
  `approve` at S3 close per `milestone_objective.md` §8 + §4.3
  per-sub-sprint review trigger.

## 7. R-items consumed / surfaced

No R-items consumed in S3 (M5 scope; C2 #2 + #5 STOP-surfaced, not
opened as R-items per the sprint-contract pattern of folding open
questions into the next sub-sprint).

Surfaced (handoff-only; deliver-agent decides whether to open R-items
at S3 close):

- **OQ-S52.1** — C2 #2 (Skill-declared context-key gating) deferred:
  the matrix-flagged Skill DECL omissions (`discover_triage` does not
  declare `customer_context`; only `resolve_faq_grounded_answer`
  declares `listing_context`) are not provably intentional. A
  prerequisite Skill-declaration audit is needed before gating. Suitable
  for S4 or a follow-on milestone where the deliver-agent can run a
  real-LLM rerun at close.
- **OQ-S52.2** — C2 #5 (Skill-soft-signal gating) deferred: the
  convergence shape is correct (registry-driven via
  `state_inheritance.soft_signal_via_projection`); the §5.6 evidence
  gate (real-LLM rerun on DISCOVER→RESOLVE bad cases: alice / cs011 /
  cs012 / wmkb) was not available in the dev sandbox without backend
  restart. Suitable for S4 or a deliver-agent close where the rerun
  fits the cadence.
- **OQ-S52.3** — OQ-S51.2 disposition: human chose option (a)
  leave-as-is + note for S3. Matrix §6 documents three rational future
  dispositions if the human elects to address the overlay divergence
  in a later sub-sprint.
- **OQ-S52.4** — `knowledge_hits` / `accumulated_tool_results` dual
  knowledge-projection path (M5 §7 flagged for S3 C1): matrix §3.N
  confirms `knowledge_hits` is NOT dead (legacy `PhaseEvaluator.evaluateResolveFaq*`
  paths pass non-null hits). Deferred to S4 or a future M5+ milestone
  to address the canonical-path question.

## 8. OQ (open questions)

- **OQ-S52.1 / OQ-S52.2 / OQ-S52.3 / OQ-S52.4** — see §7 above.
- **OQ-S51.1** — closed at S2 close (live trace eyeball PASS).
- **OQ-S51.2** — disposition (a) at S3 close per human directive
  (folded into OQ-S52.3 above).
- Java + Python baselines: see §6 above.

## 9. Backend / dev-sandbox context

- Backend at `http://localhost:8080/actuator/health` was UP at S3
  close, running the PRE-S3 build (HEAD `cf9c120` per `git log`; S3
  dev work uncommitted in the working tree at handoff time per
  commit-at-end pattern).
- LLM keys (DeepSeek + Moonshot + others) are present in env. The
  real-LLM rerun would need: (a) stop current backend, (b) `mvn
  install -DskipTests` to rebuild with C2 #4 + #3, (c) `make backend`
  to restart, (d) `cd eval_interactive && uv run eval-interactive run
  --path case_specs/bad_cases/` to rerun. Deferred to deliver-agent
  at S3 close to avoid disrupting the user's running backend session.

## 10. Self-check vs sprint-contract closing checklist

- [x] Read the obs proposal §2.C / §4.C / §8 + OQ-S51.2 handoff note?
  YES (per §1 read-order; cited in C1 matrix §7 cross-references).
- [x] C1 matrix delivered to `docs/diagnostics/` AND reviewed by
  deliver-agent + human BEFORE any C2 field change? YES — `AskUserQuestion`
  at C1 review, 2026-05-25; human chose "Land #3 + #4 only".
- [x] Every C2 change (#2-#5) gated by its matrix row (no consumer
  breaks)? YES. #3 + #4 landed only because the matrix confirmed
  consumer-safety; #2 + #5 STOP-surfaced because matrix flagged HIGH
  semantic risk + real-LLM-rerun-required.
- [x] Convergence is registry/Skill-driven — zero per-UC if-else /
  keyword / enum added? YES (§3 self-walk Q1 + Q5).
- [x] `moderation_context` resolved in the direction C1 justifies (#3)?
  YES — option (b) strip the declaration (no consumer; declaration was
  a M2-era artifact).
- [x] `tool_schemas` discarded base removed only if C1 confirms no
  consumer (#4)? YES — the C1 matrix established the base IS consumed
  by legacy paths, so #4 REPLACES it with Skill-driven (not removes).
  Run-loop primary path end-state is byte-identical.
- [x] OQ-S51.2 overlay mapped (#1a) and NOT auto-changed (human decides)?
  YES — matrix §6 mapped; human chose option (a) leave-as-is + note.
- [x] Java: no NEW regression; new wiring/rendering tests PASS? YES —
  `1172 / 1-inherited / 0 / 2` (+7 new Sprint52 tests; lone failure is
  OQ-S41.5).
- [STOP-SURFACED] **Real-LLM bad-case rerun holds the M4-close
  distribution** (PASS×5 / IMPROVING×4 / FAIL×3 / OOSR×0)? Per
  OQ-S51.1 established pattern + human directive at C1 review,
  deliver-agent dispatches at S3 close. Risk profile: byte-identical
  for run-loop primary path; cold-edge for legacy paths.
- [x] §7 stanza filled + §4.1 self-walk written for the S3-close Codex
  review? YES — sprint_objective.md §7 stanza already binding; §3 of
  this handoff is the per-sub-sprint self-walk.
- [x] Handoff §1-§11 complete; §12 reserved; staged only S3-scope files?
  YES — §12 reserved for deliver-agent at close.

## 11. Commit / bundling note

S3 dev work is UNCOMMITTED in the working tree at handoff (commit-at-end
per `feedback_commit_at_end_bundles_deliver_artefacts.md`). The dev
staged-scope ONLY per `sprint_objective.md` "Commit discipline":

- `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java`
- `server/src/main/resources/skills/resolve_faq_grounded_answer.yaml`
- `server/src/test/java/com/gumtree/csagent/service/runtime/Sprint52ProjectionSkillDrivenTest.java`
- `server/src/test/java/com/gumtree/csagent/service/runtime/PhaseEvaluatorResolveSkillIntegrationTest.java`
- `docs/diagnostics/m5-s3-projection-consumption-map.md`
- `docs/sprints/sprint-052-handoff.md`

No `git add -A`. Deliver-agent close-bundle files (objective archive,
`10-handoff.md` §0 + §1 refresh, `action_bank.md` §6 Sprint 52 row,
the per-sub-sprint `compact/sprint-052-codex-review-prompt.md`, and
the real-LLM rerun results) are bundled by the human at close per the
established pattern.

## 12. Sub-sprint close verdict (deliver-agent + human)

**Classification: A — Clean PASS — PENDING the per-sub-sprint Codex review.**
The two deliver-agent-side gates (independent verification + the real-LLM
bad-case regression-safety rerun) are complete and PASS; the FINAL S3 close
verdict is gated on the per-sub-sprint Codex review (`compact/sprint-052-codex-review-prompt.md`
→ `docs/codex-findings.md`, per `milestone_objective.md` §8). On Codex
`pass / 0` the S3 close is **A — Clean PASS**. Recorded by the deliver-agent
2026-05-25; human concurred the rerun gate.

### 12.1 Deliver-agent independent verification (not dev self-report)

- **Footprint** matches §5 exactly (`git diff --numstat` vs `cf9c120`):
  `ContextProjectionBuilder.java` +59/-13, `resolve_faq_grounded_answer.yaml`
  -1, `PhaseEvaluatorResolveSkillIntegrationTest.java` golden +1/-2, NEW
  `Sprint52ProjectionSkillDrivenTest.java`, NEW
  `docs/diagnostics/m5-s3-projection-consumption-map.md`, NEW this handoff.
- **Fences verified**: zero `eval_interactive/` / `eval/` / `data/` / `ui/`
  touch; no `system_prompt.txt` / other-Skill-YAML edit; S2 surface
  (`bot_turn_llm_calls`, `/trace`, `AgentRunLoopImpl`, `TraceWriter`,
  `mergeFaqGroundingIntoProjection`) UNTOUCHED; C2 #2 + #5 NOT landed
  (STOP-surfaced).
- **C2 #4 §1.7-clean (diff-verified)**: `resolveProjectedToolNames(session, uc)`
  → `skillRegistry.select(phase, uc).map(Skill::toolsRequired)` with a SINGLE
  defensive fallback to `toolPolicyEnforcer.getVisibleToolsForUc(uc)` for
  unmapped tuples — no per-UC if-else / keyword / enum. `SkillRegistry.select`
  (`:65`) + the legacy `buildProjection(...)` callers (`PhaseEvaluator` ×6 +
  `ControlKernel.recordTurn`) confirmed (replace-not-delete justified). The
  run-loop `build(...)` `:845-863` plan-filter overwrite leaves the run-loop
  `tool_schemas` byte-identical; the golden `allowedTools()` is unchanged.
- **C2 #3 safe**: the `moderation_context` code references are the SEPARATE
  `get_message_moderation_context` tool + `BotSession.moderationContext` session
  field, NOT the stripped `required_context_keys` projection declaration (which
  has no emission consumer).
- **Java reproduced**: `mvn test -B` → `Tests run: 1172, Failures: 1, Errors: 0,
  Skipped: 2` (aggregate surefire); the lone failure is the inherited
  `SystemPromptUserRequestedTiebreakerTest` (OQ-S41.5 STATUS QUO); `+7` =
  `Sprint52ProjectionSkillDrivenTest` (7/7 pass). No NEW regression.
- **Backend = S3 build**: the rerun ran against a backend restarted on the S3
  diff; `javap -p` confirmed `resolveProjectedToolNames` compiled into
  `target/classes` (stale-backend trap ruled out).

### 12.2 Real-LLM bad-case rerun — regression-safe PASS (the C2 evidence gate)

Run by the deliver-agent 2026-05-25 against the S3 build, Moonshot
`moonshot-v1-32k`, `parallel=1`. Full per-case evidence + reasoning in
`eval_interactive/case_specs/bad_cases/_manifest.md` "M5 / S3 (Sprint 52)
close" section. **Distribution reproduces the M4-close baseline**: PASS×5
(cs001, cs014, cs029, cs066, fg5q) + IMPROVING×4 (alice, cs011, cs012, wmkb)
+ FAIL×3 (cs015, cs095, iwzx) + OOSR×0. The decisive signal — **outcome
class — is stable** (every establishing case escalates; zero resolve↔escalate
flip). fg5q + iwzx hit the intermittent `CONTRACT_VIOLATION` session-
establishment flake on the main run; both cleared on isolated rerun (iwzx 1×,
fg5q 2×) — the documented `R-bad-case-parallel-session-establishment-flakiness`
shape, categorically upstream of `tool_schemas` projection. UC/Tier-2 jitter
is documented run-to-run LLM variance. **No C2-attributable degradation** —
the run-loop path is byte-identical and the mapped-Skill tool set is unchanged.
Human concurred regression-safe 2026-05-25.

### 12.3 Pending at close (settled with the human after Codex)

- **Per-sub-sprint Codex review** — `compact/sprint-052-codex-review-prompt.md`
  is ready; the human commits the S3 dev scope then dispatches Codex over the
  S3 commit range. On `pass / 0` → A — Clean PASS finalized here + archived to
  `docs/sprints/sprint-052-codex-review.md`.
- **S4 decision** (`milestone_objective.md` §3: taken at S3 close) — C1 flagged
  #2 (Skill-declared context-key gating) as HIGH-risk (needs a Skill-declaration
  audit) and #5 (`soft_signal_via_projection` gating) as needing a real-LLM
  rerun; both STOP-surfaced to S4. Deliver-agent recommendation: **run S4** for
  #2 (with the prerequisite Skill-declaration audit) + #5, batching their
  real-LLM rerun.
- **R-item dispositions** (OQ-S52.1 #2 / OQ-S52.2 #5 → S4 scope, not standalone
  R-items if S4 runs; OQ-S52.3 OQ-S51.2 overlay → leave-as-is + matrix §6 future
  dispositions; OQ-S52.4 `knowledge_hits` dual-path → S4 or M5+). **Flake
  observation**: `R-bad-case-parallel-session-establishment-flakiness` has now
  recurred (cs029 M4 + fg5q + iwzx S3) — recommend a priority bump.
