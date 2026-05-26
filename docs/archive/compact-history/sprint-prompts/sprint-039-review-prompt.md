Paste the content below this line into a fresh Codex session after the Sprint 39 dev commit lands. No PR will be opened; review the commit directly.

---

You are the Anti-Hardcode + Sprint-Close Review Agent for Sprint 39 — the THIRD sub-sprint of **NEW Milestone M2 (Skill Registry Abstraction + Wholesale Retroactive Externalization, LLM-led Policy-bounded)** per `docs/milestone_objective.md`. Sprint 37 (M2 sub-sprint 1 design freeze) closed PASS A on 2026-05-17 at commit `51c327c` per `docs/sprints/sprint-037-codex-review.md`. Sprint 38 (M2 sub-sprint 2 SkillRegistry-core + 4 simpler phase Skills migration) closed **B-fix-iterated** 2026-05-17 across main-dev commit `bd9d3f5` + fix iteration #1 commit `5787806` (Codex round-1 `fix_required / blocking_count: 1`; round-2 `pass / 0`); see `docs/sprints/sprint-038-codex-review.md`.

Sprint 39 is the **biggest M2 sub-sprint by scope** — multi-fence convergence at one sub-sprint per human direction (no silent scope split into Sprint 40+). The dev landed single commit `2f412b6` with:

- 2 NEW RESOLVE Skill YAMLs at `server/src/main/resources/skills/`:
  - `resolve_faq_grounded_answer.yaml` (52 lines; `applicable_use_cases: [UC-A..UC-F, UC-FP]`; `tools_required: [get_customer_context, search_knowledge, resolve_article, record_outcome, request_handover]`; `max_tool_steps: 4`; 3 guardrails declared in order: `faq_miss_handover_requires_resolve_attempt` (Sprint 6 §G2 migration) + `premature_resolve_outcome_guard` (Sprint 11 §M1 migration) + `must_cite_source` (NEW S1 per M2 §6 #4 verbatim); `objective` includes `{uc_name}` template placeholder).
  - `resolve_intake_collect_and_handover.yaml` (37 lines; `applicable_use_cases: [UC-G..UC-K]`; `tools_required: [request_handover]` — intentionally NO `create_case_controlled` per Codex 1.8 runtime-only visibility rule; `max_tool_steps: 3`; 1 guardrail: `intake_complete_required` (S2 — Sprint 7 §I2 predicate moved); 6 template-substitution placeholders `{uc_name}` / `{uc_id}` / `{team_name}` / `{intake_complete_trigger}` / `{intake_required_fields}` / `{case_creation_note}`).
- 1 NEW unified Java dispatcher at `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillGuardrailDispatcher.java` (416 lines; Spring `@Component`; injected with `SkillRegistry` + `ObjectMapper`).
- 2 NEW Java records: `RejectVerdict.java` (26 lines; `predicateName` / `hint` / `trace`) + `DispatchContext.java` (45 lines; `plan` / `session` / `accumulatedToolResults` / `lastLlmRawResponse` / `parsedUserMessage`).
- `PhaseEvaluator.java` integration extension: `composeSkillPhasePlan(...)` extended for RESOLVE Skills' template substitution via new private helper `substitutePlaceholders(text, activeUc)` (6 placeholders, each a single registry/map lookup keyed by `activeUc`; NO per-UC-pair branch). Legacy RESOLVE-INTAKE branch (parent commit `5787806` lines 417-460) + legacy RESOLVE-FAQ branch (parent lines 463-530) + legacy `buildIntakeSystemInstruction(uc, ucDef)` private helper DELETED. **1529 → 1427 lines (−102)**.
- `AgentRunLoopImpl.java` migration: 3 dispatch sites routed through `SkillGuardrailDispatcher` (HANDOVER_TOOL at parent line 317 covers `intake_complete_required` + `faq_miss_handover_requires_resolve_attempt`; HANDOVER_TOOL at line 356 covers the alternate dispatch path; RECORD_OUTCOME_TOOL at line 413 covers `premature_resolve_outcome_guard` + `must_cite_source`). 3 static predicate methods (`shouldRejectFaqMissHandover` + `shouldRejectIncompleteIntakeHandover` + `shouldRejectPrematureResolveOutcome`) + local `sessionCollected` helper + 7 dead constants REMOVED. NEW `@Autowired` 6-arg constructor with `SkillGuardrailDispatcher` parameter; legacy 5-arg constructor preserved for ~16 backward-compatible test sites (delegates with `null` dispatcher; guardrails disabled). **787 → 638 lines (−149)**.
- NEW S1 `must_cite_source` predicate declared on `resolve_faq_grounded_answer.yaml` + implemented in `SkillGuardrailDispatcher.handleMustCiteSource` bounded per M2 §6 #4 verbatim human authorization (3 boundaries: phase=RESOLVE_FAQ via Skill `applicable_use_cases` scope; tool=`record_outcome` with `class=resolve` via dispatcher invocation site; check=`source_id` citation presence in user-facing message — presence only, NO content-quality judgment).
- NEW S2 `intake_complete_required` predicate declared on `resolve_intake_collect_and_handover.yaml` + implemented in `SkillGuardrailDispatcher.handleIntakeCompleteRequired`; semantically identical to the existing Sprint 7 §I2 predicate (M2 contribution is moving from inline Java method to declarative Skill guardrail; delegates required-fields enforcement to UNCHANGED `IntakeFieldsRegistry`).
- Sprint 11 / 11.1 / 12 frozen surface preserved: `ResolveDispositionEvaluator.shouldRejectPrematureResolveOutcome` UNCHANGED; only the invocation site moves from `AgentRunLoopImpl` to `SkillGuardrailDispatcher.handlePrematureResolveOutcomeGuard` (verbatim delegation to the same static method).
- 60 new tests across 4 NEW test files: `PhaseEvaluatorResolveSkillIntegrationTest` (321 lines; 14 golden-string behavioural-equivalence tests for RESOLVE_FAQ × 7 UCs + RESOLVE_INTAKE × 5 UCs + 2 fallback paths) + `SkillGuardrailDispatcherTest` (445 lines; 24 unit tests covering 4 typed handlers × fire/no-fire scenarios + short-circuit + `RejectVerdict` trace immutability + `DispatchContext` null-safety) + `ResolveFaqGuardrailsTest` (201 lines; 11 target/neighbor/negative on 3 RESOLVE_FAQ guardrails incl. M2 §6 #4 bounded-scope verification) + `ResolveIntakeGuardrailsTest` (182 lines; 11 target/neighbor/negative for `intake_complete_required` across 5 INTAKE UCs).
- 8 EDIT test files for the constructor + dispatcher-helper sweep: `Sprint7IntakeStateTest` (6 tests via `rejectsIntakeIncomplete` helper) + `Sprint11ProgressiveResolveTest` (2 tests via `dispatcherRejectsPrematureResolve`) + `Sprint12RuntimeAlignmentValidationTest` (2 tests via same helper) + `Sprint34IntakePrefillProjectionAndGuardTest` (6 tests via `dispatcherRejectsIntakeIncomplete`) + `Sprint71PartialIntakePersistenceTest` (2 tests via same helper — M1 functional-surface preservation) + `AgentRunLoopS1FaqGroundedResolveGuardTest` (6 unit tests via `dispatcherRejectsFaqMiss` helper + integration tests on 6-arg constructor with `SkillTestFixtures.productionDispatcher()`) + `SkillLoaderTest` (production-Skills count test renamed/updated to expect 6 + Sprint 39 guardrails verified) + `SkillTestFixtures` (`productionDispatcher()` helper added) + `PhaseEvaluatorPlanTest` (removed dead Mockito stub no longer reachable).
- The 12-section handoff at `docs/sprints/sprint-039-handoff.md` (350 lines).

Per `iteration_governance.md` §4.3, Sprint 39 fires per-sub-sprint Codex review per **trigger #3** (Runtime grounding-floor surface — S1 `must_cite_source` is a NEW Runtime-floor enforcement bounded per M2 §6 #4 verbatim authorization; Runtime capability floor — unified dispatcher is a NEW architectural surface for Skill terminal-predicate enforcement; multi-fence convergence at one sub-sprint requires per-sub-sprint Codex review to verify each fence + cumulative anti-hardcode kernel).

**Sprint 37 freeze decisions Sprint 39 implements (the in-scope subset):**

- **(e §6.2.5) RESOLVE-INTAKE Skill mapping** — `resolve_intake_collect_and_handover.yaml`.
- **(e §6.2.6) RESOLVE-FAQ Skill mapping** — `resolve_faq_grounded_answer.yaml`.
- **(e §6.4) Behavioural-equivalence test pattern** — extended to the 2 RESOLVE Skills × 12 representative UCs via `PhaseEvaluatorResolveSkillIntegrationTest`.
- **(g §8.2.1) Sprint 6 §G2 → `faq_miss_handover_requires_resolve_attempt`** — handled by `SkillGuardrailDispatcher.handleFaqMissHandoverRequiresResolveAttempt`.
- **(g §8.2.2) Sprint 7 §I2 → `intake_complete_required`** — handled by `SkillGuardrailDispatcher.handleIntakeCompleteRequired`.
- **(g §8.2.3) Sprint 11 §M1 → `premature_resolve_outcome_guard`** — handled by `SkillGuardrailDispatcher.handlePrematureResolveOutcomeGuard` via verbatim delegation to UNCHANGED `ResolveDispositionEvaluator.shouldRejectPrematureResolveOutcome`.
- **(g §8.2.4) NEW S1 `must_cite_source`** — handled by `SkillGuardrailDispatcher.handleMustCiteSource` bounded per M2 §6 #4 verbatim authorization.
- **(g §8.2.5) S2 intake-completeness** — same as (g §8.2.2); the M2 contribution is the move from inline Java to declarative form.
- **(h §9.1-§9.4) unified dispatcher** — public surface `checkBeforeDispatch` + `checkBeforeOutcomePersist`; short-circuit-on-first-reject in declaration order; per-tool-call dispatch site routing; trace shape with `predicate_name` + `decision_outcome` + `skill_name` + `reject_reason_label` + handler-specific predicate-input fields.

**Sprint 37 freeze decisions Sprint 39 does NOT implement (out-of-scope; preserved for downstream sub-sprints):**

- **(f) §7 Sprint 23/31/33 teaching paragraph migration from `system_prompt.txt`** — Sprint 40 scope.
- **(i) §10 session-level state model + `state_inheritance` enforcement** — Sprint 41 scope. `state_inheritance.soft_signal_via_projection: [prior_use_case_carry]` IS declared on both Sprint 39 RESOLVE Skill YAMLs but NOT enforced (no `SkillStateBus.java`; `ContextProjectionBuilder.java` UNCHANGED; the `prior_use_case_carry` projection slot is in the Sprint 38-fix `SkillLoader.VALID_PROJECTION_SLOTS` allowlist as forward-compat — no Sprint 39 allowlist edit was required).
- **(h §9.9) C2 Tier-0 candidate elevation** — Sprint 39 ships the FIRST observed evidence surface (dispatcher short-circuit semantic); re-evaluation deferred to Sprint 39 close OR M2 close per `feedback_constitution_discipline_vs_planning_anticipation.md`.

**M2 §5 acceptance recalibration governs Sprint 39 close** per `docs/milestone_objective.md` §5 (2026-05-17, human-judgment recalibration of `iteration_governance.md` §5.6 primary-gate framing for THIS milestone): bad-case suite (Alice) + interactive eval composite_score are OBSERVATION ONLY (NOT hard gate) for M2; primary gate = functional review + Java tests pass + Sprint 37 freeze decisions honored across implementation sub-sprints + per-sub-sprint Codex review verdict `approve`. **Codex's review prompt MUST NOT re-import the §5.6 bad-case-suite-primary default for Sprint 39**; per-Q walk + behavioural-equivalence verification + Sprint 37 freeze fidelity + Sprint 11/11.1/12 frozen surface preservation + M1 functional-surface preservation + Java baseline preservation are the load-bearing surfaces.

**Sprint-39-close pre-decisions (deliver-agent + human, 2026-05-18 post-handoff):** Before Codex dispatch, deliver-agent + human pre-decided the seven OQs the dev surfaced in handoff §7. Codex INDEPENDENTLY verifies each pre-decision is sound where called out (see §9 below):

- **OQ-S39.1 (6th template-substitution placeholder `{intake_required_fields}` beyond the 5 named in contract §2.2 D-c):** **AGREE WITH DEV.** The contract §2.2 D-e clause ("the precise number of placeholders is dev judgement") explicitly anticipated this; byte-for-byte equivalence with the legacy `buildIntakeSystemInstruction(...)` output requires `IntakeFieldsRegistry.requiredFieldsFor(uc).toString()` interpolation which the 6th placeholder cleanly externalizes via a registry-driven single lookup. **Codex INFORMATIONAL** — surfaces if any placeholder slot is per-UC-pair (which would be a §1.7 violation); not a fix-required surface. If Codex believes the dev judgement should be re-formalized in the contract at 6 placeholders explicitly, raise as a Sprint 39 close housekeeping item for the deliver-agent (out-of-scope to fix-iterate).
- **OQ-S39.2 (dispatcher convenience overloads — `PhasePlan`-form + `Skill`-form):** **AGREE WITH DEV.** Sprint 39 exposes both the design doc §9.1 primary `Skill`-form (test-friendly) AND a convenience `PhasePlan`-form that looks up the active Skill via the injected `SkillRegistry`. The primary form is the design-doc fidelity surface; the convenience form is the production caller surface (avoids `AgentRunLoopImpl` re-injecting `SkillRegistry` separately). **Codex INFORMATIONAL** — surfaces if either form silently bypasses the short-circuit semantic, not blocking.
- **OQ-S39.3 (`RejectVerdict` + `DispatchContext` placement as separate files vs inner records on the dispatcher):** **AGREE WITH DEV.** Separate files chosen for testability + readability (the 24 dispatcher unit tests + 22 guardrail unit tests construct `DispatchContext` directly via the public record constructor — inner-class form would require visibility relaxation). **Codex INFORMATIONAL.**
- **OQ-S39.4 (`INTAKE_COMPLETE_GUARD_REJECT_REASON` constant relocation from `AgentRunLoopImpl` to `SkillGuardrailDispatcher.INTAKE_INCOMPLETE_REJECT_REASON`):** **AGREE WITH DEV.** The value is preserved bit-for-bit (`"intake_required_fields_missing_for_intake_complete"`); the constant moves alongside the predicate logic per the design doc §9 relocation pattern. Same for `S1_GUARD_REJECT_REASON` → `FAQ_MISS_REJECT_REASON` and `PROGRESSIVE_RESOLVE_GUARD_REJECT_REASON` → `PROGRESSIVE_RESOLVE_REJECT_REASON`. **Codex INFORMATIONAL** — verifies the value strings are byte-for-byte preserved.
- **OQ-S39.5 (C2 Tier-0 candidate — `R-skill-guardrail-non-overridability-tier-0`):** **DEFER** per `feedback_constitution_discipline_vs_planning_anticipation.md` + M2 §10 stop condition #1. Sprint 39 ships the FIRST observed evidence surface for C2 (the dispatcher with short-circuit-on-first-reject semantic); production traces have not yet been observed. Re-evaluation at Sprint 39 close OR M2 close. **Codex INDEPENDENT verdict REQUIRED** (this is the load-bearing OQ for Sprint 39 review): does Sprint 39's dispatcher landing + the 24 `SkillGuardrailDispatcherTest` short-circuit tests constitute SUFFICIENT structural evidence to elevate C2 to Tier-0 NOW, OR does the deferral hold pending production trace evidence? Expected verdict: **DEFER** (production evidence still missing; the structural Java tests are deterministic Java-logic tests, NOT LLM-behaviour evidence; the dispatcher's load-bearing claim is that LLM CANNOT override the rejection — that needs trace observation across the Sprint 39/40/41 milestone window before C2 elevation). **If Codex DISAGREES** (verdict: elevate NOW): route as `out_of_scope_review` per Sprint 37 §8 precedent + Sprint 38 OQ-S38.5 precedent — Tier-0 elevation / deferral is deliver-agent + human authority per M2 §10 stop condition #1 + `runtime_freeze_and_risk_policy.md` editing requires explicit human authorization per `feedback_constitution_discipline_vs_planning_anticipation.md`. **Do NOT classify as `fix_required`**.
- **OQ-S39.6 (`on_fail: downgrade_reason` not used in Sprint 39 RESOLVE Skills; all 4 guardrails use `reject_with_hint`):** **DEFER to Sprint 40+** per design doc §9.6 OQ. Sprint 39 finds no behavioural tension that would justify switching to `downgrade_reason`. The `SkillLoader.VALID_ON_FAIL_MODES` allowlist includes `downgrade_reason` for forward-compat. **Codex INFORMATIONAL** — confirms no tension; not blocking.
- **OQ-S39.7 (Sprint 38 test name `PhaseEvaluatorSkillIntegrationTest.resolve_intakeUc_stillFollowsLegacyBranchUntilSprint39` is now stale post-migration):** **DEFER to M2 close housekeeping** per `doc_governance.md` rename-at-fold-back cadence. The test continues to PASS post-Sprint-39 (the assertion `allowedTools == [request_handover]` still holds via the new Skill). **Codex INFORMATIONAL** — surfaces the stale name; no rename required at Sprint 39 close.

**Disagreement-routing for OQ-S39.5 (the load-bearing OQ):** Tier-0 elevation requires editing `docs/runtime_freeze_and_risk_policy.md` §1 / §2 with current-Tier-0-invariant language. Per `feedback_constitution_discipline_vs_planning_anticipation.md`, that edit requires explicit human-review escalation at planning round, not Codex's review verdict. Codex's INDEPENDENT verdict is informational input to the human's deliberation, NOT a Sprint 39 dev blocker. If Codex's verdict is "elevate NOW", deliver-agent + human pick up the decision at Sprint 39 close.

---

## 1. Loader

Read in this order on a fresh Codex session. Do NOT skip; the prompt's later sections assume each is loaded.

1. **`AGENTS.md`** (transitively loads `docs/current/doc_governance.md` + `docs/current/agent_context_guide.md` + `docs/current/iteration_governance.md` §1 / §3 / §4 / §4.1 / §4.2 / §4.3 / §5 / §5.5 / §5.6 / §7 / §8).
2. **`docs/milestone_objective.md`** — the NEW M2 milestone north star. Especially: §3 Sprint 39 row (Sprint 39 scope per the sub-sprint sequence); §5 acceptance recalibration (Alice + eval = OBSERVATION, NOT gate); §6 hard fences (22 items; esp. #1 no per-UC-branch if-else, #4 **verbatim S1 authorization quote** the dispatcher's `handleMustCiteSource` must honor with no expansion, #5 no `RuntimeIntentClassifier` / `DriftDetector` / etc touch, #20 no OLD design deletion, #21 M1 functional-surface preservation).
3. **`docs/sprint_objective.md`** — the Sprint 39 contract (~715 lines). Read: §1 sub-sprint class; §2 Goal — 6 execution phases (D-a through D-v); **§2.6 reproduces M2 §6 #4 verbatim S1 authorization quote — LOAD-BEARING**; §3 Non-goals (explicit list); §4 Premise check (15 items dev re-verified at session start per handoff §3); §5 Files in scope (single-commit list, 14-20 files); §6 Hard fences (37 items); §7 Bundle policy; §8 §7 stanza; §9 Success metrics (hard gates + observations); §10 Stop conditions (28 items; strong STOP discipline); §11 Handoff contract; §12 M2 milestone context.
4. **`docs/sprints/sprint-039-handoff.md`** — the dev's archive. Compare §9 (Files changed) against §5 of the objective; verify no scope creep. §1.2 code-shape table verified at HEAD `5787806` (pre-Sprint-39). §3 walks the 15 §4 premises (15/15 PASS reported within tolerance for the minor 1-3 line shifts on `PhaseEvaluator.java` landmarks). §4 implementation walkthrough across the 6 execution phases. §5 Sprint 37 freeze fidelity table. §6 §4.1 self-walk verdict `approve` per Q1-Q9. §7 7 OQs (OQ-S39.5 is the load-bearing one; OQ-S39.1-4/6/7 informational). §8 anti-hardcode duplicate (cross-references §6). §9 files changed table. §10 layer-classification self-walk. §11 §5 acceptance bars adapted per M2 §5 recalibration. §12 closure verdict placeholder.
5. **`docs/proposals/skill_registry_design.md`** — Sprint 37 freeze (immutable per `doc_governance.md`); architectural authority for Sprint 39. Read sections referenced by Sprint 39 in scope:
   - **§5.2** (4 typed predicate types — the `VALID_GUARDRAIL_TYPES` allowlist; the dispatcher's bounded contract; NOT a generic rule engine).
   - **§6.2.5** RESOLVE-INTAKE Skill template — compare to `server/src/main/resources/skills/resolve_intake_collect_and_handover.yaml` (note: dev chose LEGACY-verbatim per Sprint 38 OQ-S38.1 precedent + `doc_governance.md` code-ahead-of-docs; the §6.2.5 template carries editorial paraphrasing — same disagreement-routing as Sprint 38 OQ-S38.1 if Codex finds it material).
   - **§6.2.6** RESOLVE-FAQ Skill template — compare to `resolve_faq_grounded_answer.yaml`.
   - **§6.4** behavioural-equivalence test pattern — verify `PhaseEvaluatorResolveSkillIntegrationTest` extends this pattern (14 tests pinned to golden strings).
   - **§8.2.1-§8.2.5** the 5 Sprint 6/7/11/S1/S2 predicate semantics — verify each handler honors the freeze decision verbatim.
   - **§8.2.4** M2 §6 #4 verbatim S1 authorization quote — verify `handleMustCiteSource` 3-boundary check.
   - **§9.1-§9.4** dispatcher public surface + composition + per-tool-call dispatch site routing + trace shape — verify `SkillGuardrailDispatcher` honors each.
   - **§9.6** `downgrade_reason` OQ — verify Sprint 39 explicitly uses `reject_with_hint` only (OQ-S39.6 deferral).
   - **§9.9** C2 Tier-0 candidate context — verify NOT elevated by Sprint 39 dev (OQ-S39.5 deferral pending production evidence).
6. **`docs/sprints/sprint-038-objective.md`** + **`docs/sprints/sprint-038-handoff.md`** + **`docs/sprints/sprint-038-fix-handoff.md`** + **`docs/sprints/sprint-038-codex-review.md`** — Sprint 38 archives. Read for the SkillRegistry-core landing baseline (45 tests added Sprint 38; 1 more in fix iteration); `SkillLoader.VALID_GUARDRAIL_TYPES` allowlist (4 entries including all Sprint 39 types — the Sprint 38-fix shipped the allowlist with foresight for Sprint 39); `SkillLoader.VALID_PROJECTION_SLOTS` allowlist (3 entries including `prior_use_case_carry` forward-compat for Sprint 41); the post-Sprint-38-fix Java baseline 1034 / 1 inherited / 0 / 2 that Sprint 39 inherits.
7. **`docs/sprints/sprint-037-objective.md`** + **`docs/sprints/sprint-037-handoff.md`** + **`docs/sprints/sprint-037-codex-review.md`** — Sprint 37 design freeze archives. Read for the Sprint 37 freeze decision pre-text + Tier-0 candidate verdicts (C1 REJECTED preserved by construction; C2 + C3 QUALIFIED-DEFER; C4 + C5 NOT A CANDIDATE — Sprint 39 honors all).
8. **`docs/current/iteration_governance.md`** — §1.3 (LLM-owned — verify Sprint 39's 2 RESOLVE Skill `procedure` bodies are LLM-soft teaching at principle-level; verify NO Skill prescribes customer-facing language or per-step argument values beyond the registered guardrail's enforced argument scope per §1.3 line); §1.4 (Runtime-owned — verify the 4 Sprint 39 guardrail types are all Runtime-floor enforcement, NOT LLM-owned semantic decisions; verify `PhasePlan.allowedTools` continues to be enforced by `ToolDispatcher.validateAgainstPlan`); §1.7 (Forbidden — verify Sprint 39 has NO per-UC-branch if-else in any Skill YAML body or dispatcher Java code; no eval phrase encoding; no LLM-vs-Java boundary shift beyond M2 §6 #4 bounded authorization); §3.2 (`prompt_projection` Q3 + `skill_state` Q4 + Runtime-owned grounding-floor / capability-floor layer classification per Sprint 39 stanza); §4.1 nine-question kernel (Codex independently re-walks against the Sprint 39 diff); §4.2 sprint-close header convention; §4.3 trigger #3 (Sprint 39 fires); §5 / §5.5 / §5.6 acceptance bars (Sprint 39 + M2 §5 recalibration: bad-case suite OBSERVATION not gate for THIS milestone); §7 sprint-objective stanza; §8 milestone framework.
9. **`docs/runtime_freeze_and_risk_policy.md`** §1 + §2 — current Tier-0 invariant set. Verify Sprint 39 dev commit ADDS no Tier-0 invariant (M2 §6 #8 fence preserved; C2 + C3 R-items not elevated; the dispatcher landing provides FIRST observed evidence for C2 but the elevation requires explicit human-review escalation at Sprint 39 close — not a Sprint 39 dev surface). Especially verify §1.1 #3 frozen surface (`ResolveDispositionEvaluator.shouldRejectPrematureResolveOutcome` UNCHANGED in Sprint 39).
10. **`docs/current/faq_grounding_contract.md`** — grounding-floor contract. Verify Sprint 39's NEW S1 `must_cite_source` is the bounded inversion of `D-hard-citation-gate` per M2 §6 #4 verbatim authorization (NOT a generic citation gate; the original `D-hard-citation-gate` deferral preserved for all other phases / UCs / tools / outcome classes).
11. **`compact/sprint-039-dev-prompt.md`** — what the dev was authorized to do vs what landed (cross-check for in-scope vs out-of-scope edits). Especially: §5 strong STOP discipline (5 LOAD-BEARING STOPs); §7 M2 §6 #4 verbatim quote reproduced; §4.1 self-walk template the dev applied; bundle policy.
12. **`compact/context-handoff-sprint-039-pre-launch.md`** — the deliver-agent pre-launch handoff. Especially: §1 background (Sprint 38 closure + Sprint 39 contract drafting); §4 decision records (single-sub-sprint with 6 execution phases per human direction; strong STOP discipline; M2 §5 recalibration; constitution-discipline check on C2 elevation); §7.4 the per-sub-sprint Codex review trigger framing (this prompt is the deliver-agent's product for §7.4).
13. **Code source files for cited-line spot-checking** at HEAD `2f412b6` (read on demand during §3 + §4 + §5 + §6 verification, NOT end-to-end):
    - `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java` (post-Sprint-39 1427 lines per handoff §4.2; verify via `wc -l`).
    - `server/src/main/java/com/gumtree/csagent/service/runtime/AgentRunLoopImpl.java` (post-Sprint-39 638 lines per handoff §4.3; verify via `wc -l`).
    - `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillGuardrailDispatcher.java` (NEW; 416 lines; Spring `@Component`; 4 typed handlers).
    - `server/src/main/java/com/gumtree/csagent/service/runtime/skill/RejectVerdict.java` (NEW; 26 lines).
    - `server/src/main/java/com/gumtree/csagent/service/runtime/skill/DispatchContext.java` (NEW; 45 lines).
    - `server/src/main/resources/skills/resolve_faq_grounded_answer.yaml` (NEW; 52 lines).
    - `server/src/main/resources/skills/resolve_intake_collect_and_handover.yaml` (NEW; 37 lines).
    - `server/src/main/java/com/gumtree/csagent/service/runtime/ResolveDispositionEvaluator.java` (UNCHANGED; 205 lines; Sprint 11 / 11.1 / 12 frozen surface — verify via `git diff 5787806..2f412b6 -- server/src/main/java/com/gumtree/csagent/service/runtime/ResolveDispositionEvaluator.java` returns empty).
    - `server/src/main/java/com/gumtree/csagent/service/runtime/IntakeFieldsRegistry.java` (UNCHANGED; 222 lines; M1 functional surface — verify via `git diff` returns empty).
    - `server/src/main/java/com/gumtree/csagent/service/runtime/UseCaseRegistryService.java` (UNCHANGED; verify).
    - `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java` (UNCHANGED; 1161 lines; Sprint 41 scope — verify via `git diff` returns empty).
    - `server/src/main/resources/prompts/system_prompt.txt` (UNCHANGED; 101 lines; Sprint 40 scope — verify via `git diff` returns empty).
    - `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillLoader.java` (UNCHANGED from Sprint 38-fix `VALID_GUARDRAIL_TYPES` + `VALID_TOOL_NAMES` + `VALID_PROJECTION_SLOTS` allowlists; verify via `git diff` returns empty).
    - `server/src/main/java/com/gumtree/csagent/service/runtime/skill/Skill.java` + `Guardrail.java` + `StateInheritance.java` + `SkillRegistry.java` (UNCHANGED from Sprint 38-fix; verify).
    - `server/src/test/java/com/gumtree/csagent/service/runtime/PhaseEvaluatorResolveSkillIntegrationTest.java` (NEW; 321 lines; 14 tests).
    - `server/src/test/java/com/gumtree/csagent/service/runtime/skill/SkillGuardrailDispatcherTest.java` (NEW; 445 lines; 24 tests).
    - `server/src/test/java/com/gumtree/csagent/service/runtime/skill/ResolveFaqGuardrailsTest.java` (NEW; 201 lines; 11 tests incl. S1 bounded-scope verification).
    - `server/src/test/java/com/gumtree/csagent/service/runtime/skill/ResolveIntakeGuardrailsTest.java` (NEW; 182 lines; 11 tests).

## 2. Scope-discipline gate (BLOCKING)

Verify the dev commit at `2f412b6` touches ONLY the surfaces enumerated in `docs/sprint_objective.md` §5 (Sprint 39 contract files in scope). Run:

```bash
git -C /Users/caoruixin/projects/csagent-latest diff 5787806..2f412b6 --stat
```

Expected file list (any extra file outside this list is a **BLOCKING scope violation**):

- **NEW** under `server/src/main/resources/skills/`: `resolve_faq_grounded_answer.yaml` (52 lines), `resolve_intake_collect_and_handover.yaml` (37 lines).
- **NEW** under `server/src/main/java/com/gumtree/csagent/service/runtime/skill/`: `SkillGuardrailDispatcher.java` (416 lines), `RejectVerdict.java` (26 lines), `DispatchContext.java` (45 lines).
- **EDIT** `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java` (1529 → 1427 lines per handoff §4.2; +91 / −193 per `git show --numstat`).
- **EDIT** `server/src/main/java/com/gumtree/csagent/service/runtime/AgentRunLoopImpl.java` (787 → 638 lines per handoff §4.3; +141 / −290).
- **NEW** under `server/src/test/java/com/gumtree/csagent/service/runtime/`: `PhaseEvaluatorResolveSkillIntegrationTest.java` (321 lines; 14 tests).
- **NEW** under `server/src/test/java/com/gumtree/csagent/service/runtime/skill/`: `SkillGuardrailDispatcherTest.java` (445 lines; 24 tests), `ResolveFaqGuardrailsTest.java` (201 lines; 11 tests), `ResolveIntakeGuardrailsTest.java` (182 lines; 11 tests).
- **EDIT** test files (constructor + dispatcher-helper sweep): `Sprint7IntakeStateTest.java` (+52 / −14), `Sprint11ProgressiveResolveTest.java` (+39 / −4), `Sprint12RuntimeAlignmentValidationTest.java` (+37 / −3), `Sprint34IntakePrefillProjectionAndGuardTest.java` (+34 / −6), `Sprint71PartialIntakePersistenceTest.java` (+29 / −2), `AgentRunLoopS1FaqGroundedResolveGuardTest.java` (+44 / −10), `skill/SkillLoaderTest.java` (+35 / −6), `skill/SkillTestFixtures.java` (+39 / −7), `PhaseEvaluatorPlanTest.java` (+5 / −1).
- **NEW** `docs/sprints/sprint-039-handoff.md` (350 lines; 12-section dev archive).

**BLOCKING scope violations** (any presence in the dev commit at `2f412b6`):

- ANY edit to `server/src/main/java/com/gumtree/csagent/service/runtime/ResolveDispositionEvaluator.java` — Sprint 11 / 11.1 / 12 frozen surface per `runtime_freeze_and_risk_policy.md` §1.1 #3 + Sprint 39 §6 fence. Only the invocation site moves; the class itself must be UNCHANGED.
- ANY edit to `server/src/main/java/com/gumtree/csagent/service/runtime/IntakeFieldsRegistry.java` — M1 functional-surface preservation; Sprint 7 §I2 source-of-truth for per-UC intake fields.
- ANY edit to `server/src/main/java/com/gumtree/csagent/service/runtime/UseCaseRegistryService.java` — preserved through M2.
- ANY edit to `server/src/main/resources/prompts/system_prompt.txt` — Sprint 40 scope (teaching extraction).
- ANY edit to `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java` — Sprint 41 scope (no `prior_use_case_carry` projection slot impl; no `SkillStateBus` invocation).
- ANY presence of a NEW `SkillStateBus.java` — Sprint 41 scope.
- ANY edit to `RuntimeIntentClassifier.java` / `IntentClassification.java` / `DriftResult.java` / `DriftDetector.java` / `UseCaseRouter.java` / `ClassifyUseCaseTool.java` — M2 §6 #5 + M3-D Topic↔UC binding deferred.
- ANY edit to `INTAKE_UCS` set OR `escalation_reason` enum at `PhaseEvaluator.java` — M2 §6 #6 + #7 fences.
- ANY edit to `customer_service_tool_spec_v0_2.yaml` or `_v0_3.yaml` — tool schema unchanged (`RESOLVE_INTAKE` Skill's `tools_required: [request_handover]` intentionally lacks `create_case_controlled` per Codex 1.8).
- ANY edit to `RecordOutcomeTool.normalizeOutcomeClass` — UNCHANGED; the `handleMustCiteSource` mirrors the canonical/RESOLVED-alias normalization locally per handoff §4.5.
- ANY new entry to `SkillLoader.VALID_GUARDRAIL_TYPES` OR `VALID_PROJECTION_SLOTS` OR `VALID_TOOL_NAMES` allowlists — Sprint 38-fix shipped all required entries with foresight for Sprint 39 (verify via `git diff 5787806..2f412b6 -- server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillLoader.java` returns empty).
- ANY edit to `docs/runtime_freeze_and_risk_policy.md` — M2 §6 #8 fence; Tier-0 elevation requires explicit human-review escalation.
- ANY edit to `docs/current/iteration_governance.md` / `docs/current/doc_governance.md` / `docs/current/agent_context_guide.md` — constitution-discipline preserved.
- ANY edit to `docs/proposals/skill_registry_design.md` OR `docs/proposals/skill_foundation_design.md` — frozen / superseded.
- ANY edit to other docs under `docs/foundational/` or `docs/current/` (other than the Sprint 39 handoff).
- ANY edit to sprint archives under `docs/sprints/sprint-001-*` through `docs/sprints/sprint-038-*` — immutable.
- ANY edit to milestone archives under `docs/milestones/` — immutable.
- ANY edit to deliver-agent-owned files: `docs/milestone_objective.md`, `docs/sprint_objective.md`, `docs/10-handoff.md`, `docs/action_bank.md`, `docs/codex-findings.md`, `compact/sprint-039-*.md`, `compact/context-handoff-*.md` — per `feedback_commit_at_end_bundles_deliver_artefacts.md` dev does NOT stage these (the deliver-agent's batch 1 + batch 2 bundle is at a SEPARATE commit, not Sprint 39 dev commit).
- ANY edit to `eval_interactive/case_specs/case_families/<existing>/` or `eval_interactive/case_specs_shadow/case_families/<existing>/` or `eval_interactive/eval_interactive/` or `eval_interactive/case_specs/bad_cases/alice_uc_a_uc_h_misclass.yaml` or `eval_interactive/case_spec_overrides.yaml` — cascade fence from M1.

**Run** `git -C /Users/caoruixin/projects/csagent-latest show --stat 2f412b6` to enumerate the touched files; any scope violation = Finding #1 with the file path + line range quoted.

## 3. §4.1 Anti-Hardcode kernel walk (against the Sprint 39 diff)

Run the §4.1 9-question kernel verbatim as loaded from `iteration_governance.md` §4.1. **Sprint 39 is NOT exempt** (semantic-touching: introduces NEW Runtime-floor enforcement S1 + NEW unified architectural surface dispatcher per §4.3 trigger #3).

Walk each question against the **Sprint 39 IMPLEMENTATION DIFF** (not against the design doc). Per-Q load-bearing surfaces:

- **Q1 keyword / regex / if-else / enum / per-UC matrix for semantic decision?** Walk all Sprint 39 surfaces:
  - **`SkillRegistry.select(phase, useCase)`** continues to be exact-match-then-wildcard lookup, NOT a branch table. Verify Sprint 39's 2 RESOLVE Skills' `applicable_use_cases` lists (`[UC-A, UC-B, UC-C, UC-D, UC-E, UC-F, UC-FP]` for RESOLVE-FAQ; `[UC-G, UC-H, UC-I, UC-J, UC-K]` for RESOLVE-INTAKE) are SCOPE declarations matched by SkillRegistry, NOT branching tables. These lists encode the EXISTING FAQ-path-vs-INTAKE-path UC partition that pre-Sprint-39 `AgentRunLoopImpl` enforced via `FAQ_PATH_UCS` constant + `IntakeFieldsRegistry.isIntakeUseCase(...)` — Sprint 39 relocates the partition declaratively into Skill scope, NOT introduces a new partition.
  - **`PhaseEvaluator.composeSkillPhasePlan(...)` + the new `substitutePlaceholders(text, activeUc)` helper** — verify each placeholder (`{uc_name}` / `{uc_id}` / `{team_name}` / `{intake_complete_trigger}` / `{intake_required_fields}` / `{case_creation_note}`) is resolved by a single registry / map lookup keyed by `activeUc`; verify NO per-UC-pair if-else in the substitution path. Read `PhaseEvaluator.java` around the helper location post-Sprint-39 (~line 436 per handoff §4.2); confirm.
  - **`SkillGuardrailDispatcher` 4 typed predicate handlers** — each is a registry-driven single-condition check, NOT a per-UC-pair branch:
    - `handleFaqMissHandoverRequiresResolveAttempt`: checks `safeToolName(call) == "request_handover"` AND `args.escalation_reason == "faq_miss_threshold_exceeded"` AND `accumulated_tool_results.resolve_article` empty / errored AND `accumulated_tool_results.search_knowledge.faq_miss != true` AND `accumulated_tool_results.search_knowledge.hits` non-empty. The FAQ-path UC scope is encoded VIA Skill `applicable_use_cases`, NOT re-checked in the handler — verify via reading the handler at `SkillGuardrailDispatcher.java:217-257`.
    - `handleIntakeCompleteRequired`: checks `safeToolName(call) == "request_handover"` AND `reason.startsWith("intake_complete_for_uc_")` AND `IntakeFieldsRegistry.isIntakeUseCase(activeUc)` AND `!IntakeFieldsRegistry.intakeComplete(activeUc, collected)`. The INTAKE-path UC scope is encoded VIA Skill `applicable_use_cases`, AND also re-checked via `IntakeFieldsRegistry.isIntakeUseCase(activeUc)` for layered safety. Verify via reading the handler at `SkillGuardrailDispatcher.java:265-298`.
    - `handlePrematureResolveOutcomeGuard`: verbatim delegation to `ResolveDispositionEvaluator.shouldRejectPrematureResolveOutcome(plan, currentPhase, outcomeClass)` (the UNCHANGED Sprint 11 / 11.1 / 12 frozen surface). Verify via reading the handler at `SkillGuardrailDispatcher.java:307-327`.
    - `handleMustCiteSource`: 3-boundary check per M2 §6 #4 verbatim authorization:
      - (a) Skill scope (encoded VIA `resolve_faq_grounded_answer.yaml` `applicable_use_cases: [UC-A..UC-FP]`; not re-checked in handler).
      - (b) `outcomeClass` matches `"resolve"` (case-insensitive) OR legacy `"RESOLVED"` alias per `RecordOutcomeTool.normalizeOutcomeClass` (mirror'd at `SkillGuardrailDispatcher.normalizeOutcomeClass` static method).
      - (c) `parameters.cite_token_field` (default `source_id`) is PRESENT in the user-facing message text (`ctx.parsedUserMessage()` first; fallback to `ctx.lastLlmRawResponse()`).
      - The handler does NOT judge correctness, relevance, or content quality — PRESENCE ONLY. Verify via reading the handler at `SkillGuardrailDispatcher.java:340-384`.
  - **The 2 RESOLVE Skill YAML `procedure` texts** are LLM-soft teaching at principle-level. RESOLVE-FAQ: legacy `PhaseEvaluator.java` lines 498-529 verbatim with `{uc_name}` placeholder substitution at one location in `objective`. RESOLVE-INTAKE: legacy `buildIntakeSystemInstruction(...)` output verbatim with 6 template placeholders. Read each `procedure` text; cross-reference to legacy `PhaseEvaluator.java` content at parent commit `5787806`. Confirm content equivalence; confirm no per-UC-pair if-else introduced via the placeholder substitution.
  - **The 2 RESOLVE Skill YAML `tools_required` lists** are tool-set declarations per phase: FAQ = `[get_customer_context, search_knowledge, resolve_article, record_outcome, request_handover]` (5 tools); INTAKE = `[request_handover]` (1 tool; intentionally no `create_case_controlled` per Codex 1.8). Verify each tool name is in the existing tool registry (cross-reference `SkillLoader.VALID_TOOL_NAMES` at Sprint 38-fix commit).
  - **The 2 RESOLVE Skill YAML `valid_terminal_outcomes` lists** are terminal-state declarations: FAQ = `[FINAL_ANSWER, CLARIFICATION_NEEDED, ESCALATE]`; INTAKE = `[CLARIFICATION_NEEDED, ESCALATE]`. Verify each terminal name is in the existing `TerminalOutcome` enum.
  - **The 2 RESOLVE Skill YAML `state_inheritance` blocks** are STATE-DIMENSION-keyed: FAQ = `inherit: [customer_context, accumulated_tool_results]`, `reset: []`, `soft_signal_via_projection: [prior_use_case_carry]`; INTAKE = `inherit: [customer_context, intake_fields_partial]`, `reset: []`, `soft_signal_via_projection: [prior_use_case_carry]`. Verify NO per-UC-pair declaration; verify each state slot is in the existing `SkillLoader.VALID_STATE_KEYS` + `VALID_PROJECTION_SLOTS` allowlists (Sprint 38-fix shipped `prior_use_case_carry` as forward-compat — no Sprint 39 allowlist edit required; verify).
  - **The 2 RESOLVE Skill YAML `guardrails` blocks** declare guardrails in DECLARATION ORDER (the dispatcher walks `activeSkill.guardrails()` in declaration order per design doc §9.2; declaration order matters for short-circuit). FAQ: `[faq_miss_handover_requires_resolve_attempt, premature_resolve_outcome_guard, must_cite_source]` (3 in order). INTAKE: `[intake_complete_required]` (1).
  - **`AgentRunLoopImpl` 6-arg vs 5-arg constructor** — the new 6-arg constructor with `SkillGuardrailDispatcher` parameter is the production path (Spring DI); the 5-arg constructor preserved for backward-compat in ~16 tests delegates with `null` dispatcher (guardrails disabled). Verify the backward-compat path is appropriate for the ~16 tests that don't exercise predicate semantics; verify the 5 tests that DO exercise predicate semantics were migrated to the 6-arg constructor via the dispatcher-helper pattern (handoff §4.3 names them: `Sprint7IntakeStateTest`, `Sprint11ProgressiveResolveTest`, `Sprint12RuntimeAlignmentValidationTest`, `Sprint34IntakePrefillProjectionAndGuardTest`, `Sprint71PartialIntakePersistenceTest`, `AgentRunLoopS1FaqGroundedResolveGuardTest`).
  - Expected: NO per-UC-branch if-else in any Sprint 39 surface. Dev handoff §6 Q1 verdict `NO`. Codex independently confirms or surfaces dissent with the diff snippet quoted.
- **Q2 Tier-0 justification?** Sprint 39 adds no Tier-0 invariant. The dispatcher landing provides FIRST observed evidence for C2 (refusal non-overridability) but the elevation requires explicit human-review escalation per `feedback_constitution_discipline_vs_planning_anticipation.md` + M2 §10 stop condition #1. Verify: `docs/runtime_freeze_and_risk_policy.md` §1 + §2 unchanged at HEAD `2f412b6` (run `git diff 5787806..2f412b6 -- docs/runtime_freeze_and_risk_policy.md` returns empty). The Sprint 11/11.1/12 frozen surface (`runtime_freeze_and_risk_policy.md` §1.1 #3) is also preserved by construction (`ResolveDispositionEvaluator.shouldRejectPrematureResolveOutcome` UNCHANGED).
- **Q3 soft signal achievable?** N/A — Sprint 39 is architectural refactoring + bounded predicate landing. The 3 migrated predicates (Sprint 6/7/11) were already Runtime-floor enforcement pre-Sprint-39; relocation does not change the LLM-vs-Java boundary. The NEW S1 is the M2 §6 #4 verbatim authorized Runtime-floor extension (NOT a generic citation gate; the original `D-hard-citation-gate` deferral preserved for all other phases / UCs / tools per `action_bank.md`). The NEW S2 is the existing Sprint 7 §I2 semantics moved.
- **Q4 visible-eval / trace phrasing / CaseSpec id encoded into runtime / prompt / judge?** Read the 2 RESOLVE Skill YAML bodies + dispatcher Java source + handler bodies. Verify: NO `alice_*` CaseSpec id, NO `cs_*` CaseSpec id, NO trace phrasing or bad-case-suite reference text encoded anywhere. The 2 RESOLVE YAML bodies carry the legacy `PhaseEvaluator.java` Java-string content (Codex M1 / Sprint 37 / Sprint 38 already cleared of eval-text leakage). The dispatcher trace shape (`predicate_name` + `decision_outcome` + `skill_name` + `reject_reason_label` + handler-specific predicate-input fields) extends the existing Sprint 6/7/11 trace pattern with `skill_name` per design doc §9.4.
- **Q5 semantic ownership shift LLM → Java?** Verify: §1.3 LLM ownership preserved (each Skill `procedure` is LLM-soft teaching at principle-level; no Skill prescribes customer-facing language or per-step argument values beyond the registered guardrail's enforced argument scope per §1.3 line). §1.4 Runtime ownership preserved (tool schemas unchanged; `PhasePlan.allowedTools` continues to be enforced by `ToolDispatcher.validateAgainstPlan`; PII / safety floor untouched). The 3 migrated predicates were already Java pre-Sprint-39 (Sprint 6/7/11 ALREADY shipped them); the migration relocates the invocation surface without LLM ownership shift. The NEW S1 is bounded per M2 §6 #4 verbatim authorization (grounding-floor minimum-citation-presence is Runtime-owned per §1.4); NOT a shift of LLM-owned next-action / response strategy / customer-facing language.
- **Q6 if-else in prompt?** Read each of the 2 RESOLVE Skill `procedure` / `grounding_instruction` / `escalation_policy` text bodies. Verify: principle-level teaching with template-substitution placeholders for registry-driven values; NO per-UC-pair if-else in any text body. The `system_prompt.txt` `request_handover` decision tree at lines 54-101 is UNCHANGED in Sprint 39 (Sprint 39 §6 fence; the tree was Sprint 37 OQ-7.11 reviewed and AGREE WITH DEV).
- **Q7 tool schema / capability / PII / grounding floor preserved?** Verify: tool schema unchanged (no `customer_service_tool_spec_v0_2.yaml` / `v0_3.yaml` edit); `PhasePlan.allowedTools` composition unchanged (composed from `skill.toolsRequired`); PII / safety floor untouched. **Grounding floor EXTENDED for S1 bounded per M2 §6 #4 verbatim authorization (NOT a generic citation gate)** — verify the `handleMustCiteSource` handler's 3-boundary check + the `parameters.outcome_class=resolve` / `cite_token_field=source_id` declarations on `resolve_faq_grounded_answer.yaml` guardrail block + the test coverage in `ResolveFaqGuardrailsTest` 4 negative cases (`class=escalate` + `class=abandon` + `source_id` present + Skill scope mismatch).
- **Q8 generalization coverage?** Per handoff §10 (target / neighbor / negative / shadow). Verify against actual Sprint 39 test files:
  - **Target**: 2 RESOLVE phase Skills × 12 representative UCs (7 FAQ + 5 INTAKE) in `PhaseEvaluatorResolveSkillIntegrationTest.java` (14 tests; golden-string behavioural equivalence; read the file and spot-check at least 2 golden assertions).
  - **Neighbor**: 4 Sprint 38 phase Skills (UNCHANGED through Sprint 39) + existing Sprint 71 / Sprint 7 / Sprint 11 / Sprint 12 / Sprint 34 / Sprint 38 tests preserved via dispatcher-helper-pattern updates. Run targeted regression spot-checks (§7 below).
  - **Negative**: 4 negatives on NEW S1 in `ResolveFaqGuardrailsTest` (`class=escalate`, `class=abandon`, `source_id` present, Skill scope mismatch); 3 negatives on S2 in `ResolveIntakeGuardrailsTest` (`user_requested`, `incomplete_intake` reason variant, faq-path UC). Plus the existing 12 negative `SkillLoaderTest` cases (UNCHANGED).
  - **Shadow**: N/A per M2 §5 acceptance recalibration.
- **Q9 rollback / sunset?** N/A — SkillRegistry abstraction is permanent. If behavioural equivalence regresses on RESOLVE × representative UCs, `git revert 2f412b6` restores the 2 legacy RESOLVE branches + the 3 static `shouldRejectXxx` methods + the legacy `buildIntakeSystemInstruction` helper. The deletion is bounded; recovery is exact.

**Expected verdict on the §4.1 kernel walked against the Sprint 39 diff: `approve`** (architectural refactoring + bounded predicate landing; behavioural equivalence preserved; no per-UC-branch hardcode introduced; tool-whitelist semantics unchanged; LLM-vs-Java boundary preserved beyond M2 §6 #4 bounded authorization for S1). Codex may surface per-Q informational dissent; raise as BLOCKING only if Q1 / Q5 / Q6 / Q7 yields a substantive boundary violation OR if Q2 surfaces evidence the dev silently elevated C2 to Tier-0 (which would be a §6 #8 fence violation).

## 4. §1.7 boundary check on the Sprint 39 implementation (BLOCKING if a violation is found)

§1.7 forbidden list applies to the Sprint 39 IMPLEMENTATION DIFF. Specifically verify:

- **The 2 RESOLVE Skill YAML `procedure` / `grounding_instruction` / `escalation_policy` text bodies** do NOT encode "raw eval phrases into Java or prompt" — read each verbatim; confirm no CaseSpec id, no trace phrasing, no `alice_*` reference, no bad-case-suite text. The text bodies are CONTENT migrations from legacy `PhaseEvaluator.java`; verify content equivalence + content boundary.
- **The 2 RESOLVE Skill YAML `applicable_use_cases` lists** are explicit UC enumerations (7 + 5 = 12 UCs total) per design doc §6.2.5 + §6.2.6. These are SCOPE declarations, NOT per-UC-branch tables — verify by reading each YAML; verify the lists match the EXISTING FAQ-path / INTAKE-path UC partition that pre-Sprint-39 `AgentRunLoopImpl` enforced (`FAQ_PATH_UCS` constant + `IntakeFieldsRegistry.isIntakeUseCase(...)`). Sprint 39 relocates the partition declaratively into Skill scope, NOT introduces a new partition. Any new UC additions / removals relative to the pre-Sprint-39 partition would require Sprint 37 freeze re-approval — flag as BLOCKING if present.
- **The 2 RESOLVE Skill YAML `state_inheritance` blocks** are STATE-DIMENSION-keyed, NOT per-UC-pair-keyed. Verify.
- **The 2 RESOLVE Skill YAML `guardrails` blocks** declare guardrails in DECLARATION ORDER (the dispatcher walks `activeSkill.guardrails()` in declaration order per §9.2). FAQ declaration order: `[faq_miss_handover_requires_resolve_attempt, premature_resolve_outcome_guard, must_cite_source]`. INTAKE: `[intake_complete_required]`. Verify each guardrail `type` is in `SkillLoader.VALID_GUARDRAIL_TYPES` (4 entries shipped at Sprint 38-fix); each `on_fail` is `reject_with_hint` (no `downgrade_reason` used per OQ-S39.6 deferral); the FAQ `must_cite_source` block carries `parameters.outcome_class: resolve` + `parameters.cite_token_field: source_id` (the M2 §6 #4 bounded scope encoded declaratively); the INTAKE `intake_complete_required` block carries `parameters.escalation_reason_pattern: "intake_complete_for_uc_*"` + `parameters.required_fields_source: IntakeFieldsRegistry`.
- **`SkillGuardrailDispatcher`** is bounded to the 4 typed predicate types per `SkillLoader.VALID_GUARDRAIL_TYPES` allowlist + the 4 private handler methods + the 2 public switch statements (`checkBeforeDispatch` + `checkBeforeOutcomePersist`). It is NOT a generic rule engine accepting arbitrary external predicate definitions per design doc §9.1 + Sprint 39 contract §6 fences. Verify by reading the `SkillGuardrailDispatcher.java` switch bodies + handler method signatures; verify NO `Map<String, Function<...>>` lookup pattern that would allow runtime injection of arbitrary predicate handlers; verify the 4 handlers are the ONLY methods invoked from the switch.
- **The `handleMustCiteSource` 3-boundary check** matches M2 §6 #4 verbatim authorization with NO expansion. Read the handler at `SkillGuardrailDispatcher.java:340-384`; verify:
  - (a) `outcomeClass` normalized to lowercase + checked against `"resolve"` OR legacy `"resolved"` alias (mirrors `RecordOutcomeTool.normalizeOutcomeClass`). If `outcomeClass != "resolve"` → `Optional.empty()` (no fire on `class=escalate` / `class=abandon` / other classes).
  - (b) `parameters.outcome_class` declared on the YAML guardrail block agrees (must be `"resolve"`; configured-mismatch → handler bows out gracefully).
  - (c) `userMessage.contains(citeToken)` where `citeToken = parameters.cite_token_field` (default `source_id`). Presence check ONLY — no quality / correctness / relevance judgment.
  - Skill scope (phase=RESOLVE_FAQ) is encoded VIA `resolve_faq_grounded_answer.yaml` `applicable_use_cases: [UC-A..UC-FP]`; not re-checked in handler.
- **The `handleIntakeCompleteRequired` semantic** matches Sprint 7 §I2 byte-for-byte (no new semantics introduced; this IS S2 = (g §8.2.2)). Read the handler at `SkillGuardrailDispatcher.java:265-298`; verify:
  - `safeToolName(call) == "request_handover"`.
  - `escalation_reason.startsWith("intake_complete_for_uc_")` (matches the `parameters.escalation_reason_pattern` declared on the YAML).
  - `IntakeFieldsRegistry.isIntakeUseCase(activeUc)` (M1 functional surface; UNCHANGED).
  - `!IntakeFieldsRegistry.intakeComplete(activeUc, collected)` → reject (Sprint 7 §I2 byte-for-byte).
  - Trace reason label = `"intake_required_fields_missing_for_intake_complete"` (preserved bit-for-bit from `AgentRunLoopImpl.INTAKE_COMPLETE_GUARD_REJECT_REASON`).
- **The `handlePrematureResolveOutcomeGuard` delegation** is verbatim to `ResolveDispositionEvaluator.shouldRejectPrematureResolveOutcome(plan, currentPhase, outcomeClass)`. Read the handler at `SkillGuardrailDispatcher.java:307-327`; verify the static call signature matches the UNCHANGED `ResolveDispositionEvaluator` surface; verify no transformation of `plan` / `currentPhase` / `outcomeClass` before the delegation call.
- **`SkillLoader` allowlists UNCHANGED** — verify `VALID_GUARDRAIL_TYPES` still has exactly 4 entries (Sprint 38-fix shipped: `faq_miss_handover_requires_resolve_attempt` + `intake_complete_required` + `premature_resolve_outcome_guard` + `must_cite_source`); verify `VALID_PROJECTION_SLOTS` still has the Sprint 38-fix 3 entries (`alternate_candidate_use_cases` + `discover_disambiguation_signals` + `prior_use_case_carry`); verify `VALID_TOOL_NAMES` still has the Sprint 38-fix 11 canonical tool names. ANY allowlist extension in Sprint 39 commit = BLOCKING scope violation (the Sprint 38-fix shipped them with foresight; no extension required).
- **No semantic ownership shift LLM → Java** beyond M2 §6 #4 bounded authorization (the 3 migrated predicates were already Java pre-Sprint-39; only relocation; the NEW S1 is the bounded Runtime-floor extension; the NEW S2 is the existing Sprint 7 §I2 moved). Verify.

If ANY Sprint 39 implementation surface encodes a §1.7 violation (per-UC-branch if-else; new Tier-0 invariant added; LLM-ownership shift; eval-phrase encoding; generic rule engine instead of bounded 4-type dispatcher; S1 expansion beyond 3 boundaries; semantic hardcode without justification), raise as a **BLOCKING finding** and recommend `fix_required`.

## 5. Hard-fence verification

- **Sprint 39 contract §6 hard fences (37 items):** verify each is honored at HEAD `2f412b6`. Walk in order; especially:
  - **#1** No per-UC-branch if-else in any Skill body OR dispatcher Java OR `composeSkillPhasePlan` template substitution path — verified by §3 Q1 + §4 above.
  - **#4** **M2 §6 #4 verbatim S1 authorization quote — LOAD-BEARING**: S1 `must_cite_source` bounded to phase=RESOLVE_FAQ + tool=record_outcome with class=resolve + check=source_id citation presence. No expansion. Verified by `handleMustCiteSource` 3-boundary check + `ResolveFaqGuardrailsTest` 4 negative cases.
  - **#5** No `RuntimeIntentClassifier.java` / `IntentClassification.java` / `DriftResult.java` / `DriftDetector.java` / `UseCaseRouter.java` / `ClassifyUseCaseTool.java` touch — verify via `git diff 5787806..2f412b6 -- server/src/main/java/com/gumtree/csagent/service/runtime/RuntimeIntentClassifier.java [...]` (all return empty).
  - **#7** No `escalation_reason` enum widening — verify by reading the post-Sprint-39 `PhaseEvaluator.java` enum; unchanged.
  - **#8** No Tier-0 invariant added — verify via `git diff` on `docs/runtime_freeze_and_risk_policy.md` (returns empty).
  - **#9-#11** No edits to existing case families / shadow / harness — verify via `git diff 5787806..2f412b6 -- eval_interactive/` (returns empty).
  - **#12** No constitution-discipline doc edits — verify via `git diff` on `docs/current/iteration_governance.md` + `doc_governance.md` + `agent_context_guide.md` + `runtime_freeze_and_risk_policy.md` (all return empty).
  - **No Sprint 11 / 11.1 / 12 frozen surface edit** — verify `git diff 5787806..2f412b6 -- server/src/main/java/com/gumtree/csagent/service/runtime/ResolveDispositionEvaluator.java` returns empty.
  - **No M1 functional surface edit** — verify `git diff` on `IntakeFieldsRegistry.java` + `IntakeFieldExtractor.java` returns empty.
  - **No Sprint 40/41 scope creep** — verify `git diff` on `system_prompt.txt` + `ContextProjectionBuilder.java` returns empty; verify NO `SkillStateBus.java` file present at `server/src/main/java/com/gumtree/csagent/service/runtime/`.
  - **No NEW outcome class enum** (no `resolve_no_citation` added; rejection happens via `reject_with_hint` per design doc §9.5) — verify by reading `RecordOutcomeTool` outcome class set + Skill YAML `valid_terminal_outcomes`; no new enum entry.
  - **No `on_fail: observe_only` or `on_fail: downgrade_reason`** in Sprint 39 RESOLVE Skill YAMLs — verify by reading the 2 YAMLs (all 4 guardrails declare `on_fail: reject_with_hint`).
  - **No deletion of OLD `docs/proposals/skill_foundation_design.md`** — verify the file still exists at HEAD `2f412b6` with `status: superseded` frontmatter preserved.

- **M2 milestone-level hard fences (22 items per `docs/milestone_objective.md` §6):** verify each is honored at HEAD `2f412b6`. Especially:
  - **#1** No per-UC-branch if-else — verified by §3 + §4.
  - **#2** Skill terminal predicate is Java guard ONLY for Runtime-owned floor per §1.4 — verify the 4 Sprint 39 guardrail types are all Runtime-floor enforcement (FAQ-miss handover guard, intake-completeness guard, premature-resolve-outcome guard, S1 source citation presence) — NOT LLM-owned semantic decisions. The dispatcher trace surfaces predicate-input fields so the LLM observes the rejection but cannot override.
  - **#3** Skill recommended order is soft prompt guidance, NOT hard enforcement — verify each `procedure` text is principle-level + LLM-soft (FAQ-path = "search_knowledge → resolve_article → record_outcome (with source_id citation) → terminal"; INTAKE-path = "ask missing fields → request_handover when complete"). No hard step enforcement in the prompt; the guardrails enforce the cross-step contracts post-LLM-decision at the dispatch site.
  - **#4** HARD FENCE INVERSION on `D-hard-citation-gate` (verbatim S1 authorization) — verified by §5 contract #4 + §4 handler check.
  - **#8** No Tier-0 invariant added without Sprint 37 freeze pre-authorization + explicit human-review escalation — verified by §8 below.
  - **#21** No reset / migration of M1-shipped FUNCTIONAL surfaces — verified by §5 above (`IntakeFieldExtractor`, `Sprint71PartialIntakePersistenceTest`, `IntakeFieldsRegistry`, Sprint 32/33 projections in `ContextProjectionBuilder.java` all UNCHANGED).

- **Behavioural equivalence verification (load-bearing per Sprint 37 freeze §6.4 extended to RESOLVE phases):** the §6.4 hard gate is "Skill-composed PhasePlan field-by-field equal to pre-migration golden". Codex independently verifies:
  1. The `PhaseEvaluatorResolveSkillIntegrationTest.java` test file contains 14 behavioural-equivalence cases (RESOLVE-FAQ × 7 UCs + RESOLVE-INTAKE × 5 UCs + 2 fallback paths). Read the test file (~321 lines).
  2. Each test pins the Skill-composed PhasePlan to GOLDEN pre-migration legacy string captured inline from `PhaseEvaluator.java` legacy RESOLVE-FAQ branch (498-529) + legacy `buildIntakeSystemInstruction(...)` output at parent commit `5787806`. Spot-check at least one RESOLVE-FAQ assertion + one RESOLVE-INTAKE assertion:
     - `git show 5787806:server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java | sed -n '498,529p'` (RESOLVE-FAQ legacy branch).
     - Read the corresponding test assertion in `PhaseEvaluatorResolveSkillIntegrationTest.java`.
     - Confirm character-by-character equivalence (modulo whitespace normalization Java string concatenation performed + 6 placeholder substitutions for INTAKE).
  3. The test passes via `mvn test -Dtest=PhaseEvaluatorResolveSkillIntegrationTest -q` from a clean checkout of `2f412b6`.

- **Sprint 37 freeze fidelity verification (per Sprint 39 handoff §5 table):**
  - Decision (e §6.2.5) RESOLVE-INTAKE → `resolve_intake_collect_and_handover.yaml`. Verify content vs legacy `buildIntakeSystemInstruction` output (Sprint 38 OQ-S38.1 precedent: legacy-verbatim acceptable).
  - Decision (e §6.2.6) RESOLVE-FAQ → `resolve_faq_grounded_answer.yaml`. Verify content vs legacy `PhaseEvaluator.java:498-529`.
  - Decision (e §6.4) behavioural-equivalence pattern → `PhaseEvaluatorResolveSkillIntegrationTest` 14 tests.
  - Decision (g §8.2.1) Sprint 6 §G2 → `handleFaqMissHandoverRequiresResolveAttempt`. Semantic equivalence verified by `Sprint6GuardSpec` historical references + dispatcher behaviour.
  - Decision (g §8.2.2) Sprint 7 §I2 → `handleIntakeCompleteRequired`. Verified by the 6 migrated `Sprint7IntakeStateTest` tests + 6 `Sprint34IntakePrefillProjectionAndGuardTest` tests + 2 `Sprint71PartialIntakePersistenceTest` tests + 2 `Sprint12RuntimeAlignmentValidationTest` tests.
  - Decision (g §8.2.3) Sprint 11 §M1 → `handlePrematureResolveOutcomeGuard` (verbatim delegation to UNCHANGED `ResolveDispositionEvaluator.shouldRejectPrematureResolveOutcome`). Verified by the 2 migrated `Sprint11ProgressiveResolveTest` tests + `ResolveDispositionEvaluator.java` UNCHANGED.
  - Decision (g §8.2.4) NEW S1 → `handleMustCiteSource` 3-boundary check bounded per M2 §6 #4 verbatim authorization. Verified by `ResolveFaqGuardrailsTest` 11 tests (target/neighbor/negative) incl. 4 negatives on bounded-scope.
  - Decision (g §8.2.5) S2 intake-completeness = decision (g §8.2.2) (same predicate moved).
  - Decision (h §9.1) public surface `checkBeforeDispatch` + `checkBeforeOutcomePersist` exposed in primary `Skill`-form + convenience `PhasePlan`-form.
  - Decision (h §9.2) short-circuit-on-first-reject in declaration order — verified by `SkillGuardrailDispatcherTest.shortCircuit_*` tests.
  - Decision (h §9.3) per-tool-call dispatch site routing — verified by `AgentRunLoopImpl` HANDOVER_TOOL + RECORD_OUTCOME_TOOL block migration.
  - Decision (h §9.4) trace shape with `predicate_name` + `decision_outcome` + `predicate_input_data` + `reject_reason_label` + `skill_name` — verified by reading dispatcher handler trace assembly + `RejectVerdict` record.
  - Decision (h §9.9) C2 Tier-0 candidate NOT pre-elevated — verified by `runtime_freeze_and_risk_policy.md` UNCHANGED.

- **MATERIAL FINDING preservation through Sprint 39** (cross-reference Sprint 37 close + Sprint 38 close):
  - Sprint 11 §M1 predicate semantics preserved via `ResolveDispositionEvaluator` UNCHANGED + verbatim delegation — verify.
  - Sprint 7 §I2 predicate semantics preserved via `IntakeFieldsRegistry.intakeComplete(...)` UNCHANGED — verify.
  - Sprint 6 §G2 predicate semantics preserved bit-for-bit in `handleFaqMissHandoverRequiresResolveAttempt` (verify the 6 condition checks match the legacy `shouldRejectFaqMissHandover` body at parent `5787806:server/src/main/java/com/gumtree/csagent/service/runtime/AgentRunLoopImpl.java:690-734`).

- **Sprint 33 cue/slot split preservation:**
  - Cue at Sprint 38's `discover_triage.yaml` `procedure` (Sprint 33 ad-status disambiguation cue text) — UNCHANGED in Sprint 39 (Sprint 38 already migrated; Sprint 39 does not touch DISCOVER Skill). Verify.
  - Slot at `system_prompt.txt:36-43` (the Sprint 33 slot description teaching block) — UNCHANGED in Sprint 39 (Sprint 40 migrates). Verify `git diff` returns empty on `system_prompt.txt`.

- **Premise re-verification (Sprint 38 close + Sprint 39 dev) preservation:**
  - `system_prompt.txt` is 101 lines — verify at HEAD `2f412b6` via `wc -l`.
  - `IntakeFieldsRegistry.java` is 222 lines — verify; UNCHANGED.
  - `UseCaseRegistryService.java` is 174 lines — verify; UNCHANGED.
  - `ContextProjectionBuilder.java` is 1161 lines — verify; UNCHANGED.
  - `ResolveDispositionEvaluator.java` is 205 lines — verify; UNCHANGED.
  - `PhaseEvaluator.java` post-Sprint-39 is 1427 lines per handoff (1529 → 1427 = −102) — verify via `wc -l`.
  - `AgentRunLoopImpl.java` post-Sprint-39 is 638 lines per handoff (787 → 638 = −149) — verify via `wc -l`.

## 6. Schema and reproducibility checks

- **RESOLVE Skill YAML schemas (design doc §6.2.5 + §6.2.6):**
  - `resolve_faq_grounded_answer.yaml`: required fields `name` / `description` / `applicable_phases: [RESOLVE]` / `applicable_use_cases: [UC-A..UC-FP, 7 UCs]` / `tools_required: [5 tools]` / `required_context_keys: [4 keys]` / `max_tool_steps: 4` / `allow_interim_message: false` / `valid_terminal_outcomes: [FINAL_ANSWER, CLARIFICATION_NEEDED, ESCALATE]` / `objective` (with `{uc_name}` placeholder) / `procedure` / `grounding_instruction` / `escalation_policy` / `guardrails: [3 entries in declaration order]` / `state_inheritance: [inherit 2, reset [], soft_signal_via_projection [prior_use_case_carry]]`.
  - `resolve_intake_collect_and_handover.yaml`: required fields `name` / `description` / `applicable_phases: [RESOLVE]` / `applicable_use_cases: [UC-G..UC-K, 5 UCs]` / `tools_required: [request_handover]` (intentionally 1 tool; NO `create_case_controlled`) / `required_context_keys: [2 keys]` / `max_tool_steps: 3` / `allow_interim_message: false` / `valid_terminal_outcomes: [CLARIFICATION_NEEDED, ESCALATE]` / `objective` (with `{uc_name}` + `{team_name}` placeholders) / `procedure` (with 6 placeholders) / `grounding_instruction` (with `{case_creation_note}` placeholder) / `escalation_policy` (with `{intake_complete_trigger}` placeholder) / `guardrails: [1 entry: intake_complete_required with parameters]` / `state_inheritance: [inherit 2, reset [], soft_signal_via_projection [prior_use_case_carry]]`.
  - Both YAMLs validate at boot via `SkillLoader.loadAll()` against the existing `VALID_GUARDRAIL_TYPES` + `VALID_TOOL_NAMES` + `VALID_PROJECTION_SLOTS` + `VALID_STATE_KEYS` + `VALID_ON_FAIL_MODES` allowlists — NO allowlist extension required.
- **`SkillGuardrailDispatcher` data shape:**
  - Spring `@Component` annotation.
  - Constructor injection: `SkillRegistry` + `ObjectMapper`.
  - 4 typed predicate type constants (mirror `SkillLoader.VALID_GUARDRAIL_TYPES`).
  - 4 reject-reason label constants (3 preserved bit-for-bit from `AgentRunLoopImpl` + 1 NEW S1 = `s1_citation_presence_required`).
  - 4 public entry points (2 `checkBeforeDispatch` overloads — primary Skill-form + convenience PhasePlan-form; 2 `checkBeforeOutcomePersist` overloads — same pattern).
  - 4 private handler methods.
- **`RejectVerdict` data shape:** record with `predicateName` (String) + `hint` (String) + `trace` (Map<String, Object>; copied on construction for immutability).
- **`DispatchContext` data shape:** record with `plan` (PhasePlan) + `session` (BotSession) + `accumulatedToolResults` (Map<String, Object>; copied) + `lastLlmRawResponse` (String) + `parsedUserMessage` (Optional<String>).
- **`PhaseEvaluator` integration:**
  - Constructor still 11-arg (Sprint 38-fix shape; no new arg added at Sprint 39).
  - `composeSkillPhasePlan(Skill, String, String)` extended; private helper `substitutePlaceholders(text, activeUc)` added; legacy RESOLVE-INTAKE branch + legacy RESOLVE-FAQ branch + legacy `buildIntakeSystemInstruction(...)` private helper DELETED.
  - Backward-compatible PhasePlan data shape (`PhasePlan.java` UNCHANGED at 146 lines).
- **`AgentRunLoopImpl` migration:**
  - NEW 6-arg constructor with `SkillGuardrailDispatcher` parameter.
  - Legacy 5-arg constructor preserved (delegates with `null` dispatcher; guardrails disabled).
  - 3 dispatch sites replaced with single dispatcher call each.
  - 3 static predicate methods + 1 local helper + 7 dead constants REMOVED.
- **Test file structure:**
  - `PhaseEvaluatorResolveSkillIntegrationTest` (321 lines; 14 tests per handoff §4.6).
  - `SkillGuardrailDispatcherTest` (445 lines; 24 tests).
  - `ResolveFaqGuardrailsTest` (201 lines; 11 tests).
  - `ResolveIntakeGuardrailsTest` (182 lines; 11 tests).
  - 8 EDIT test files (constructor + helper sweep; verify each EDIT is ≤ ~50 lines added).
- **Line-count verifications:**
  - `PhaseEvaluator.java` post-Sprint-39: handoff claims 1427 (1529 → 1427 = −102). Codex verifies via `wc -l server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java` at HEAD `2f412b6`. Surface material discrepancy as REPRODUCIBILITY observation per `feedback_deliver_agent_cited_numbers_must_be_reproducible.md`; NOT a BLOCKING finding (the substantive deletion of legacy RESOLVE branches is the behaviour-bearing change).
  - `AgentRunLoopImpl.java` post-Sprint-39: handoff claims 638 (787 → 638 = −149). Codex verifies via `wc -l`.
  - `SkillGuardrailDispatcher.java`: handoff claims 416 lines. Verify.
- **Reproducibility (per `feedback_deliver_agent_cited_numbers_must_be_reproducible.md`):** every claim in the dev handoff about current code shape cites file path + line range. Codex spot-checks 5-6 cited ranges; confirms they exist and match the handoff claim:
  - `SkillGuardrailDispatcher.java:217-257` `handleFaqMissHandoverRequiresResolveAttempt` body — verify.
  - `SkillGuardrailDispatcher.java:265-298` `handleIntakeCompleteRequired` body — verify.
  - `SkillGuardrailDispatcher.java:307-327` `handlePrematureResolveOutcomeGuard` body (with delegation to `ResolveDispositionEvaluator`) — verify.
  - `SkillGuardrailDispatcher.java:340-384` `handleMustCiteSource` body — verify 3-boundary check + presence-only behaviour.
  - `resolve_faq_grounded_answer.yaml` guardrails block — verify 3 entries in declaration order.
  - `resolve_intake_collect_and_handover.yaml` guardrails block — verify 1 entry with `parameters`.
- **Handoff structure:** verify 12-section shape per Sprint 31-38 + Sprint 39 §11 contract. §12 closure verdict placeholder per `feedback_handoff_verdict_section_delegation.md` (NOT filled by dev).

## 7. Validation runs (you re-execute)

From a clean checkout of the dev commit `2f412b6`:

- **Java test baseline preservation:**

  ```bash
  cd server && mvn test -q
  ```

  Expected: `Tests run: 1094, Failures: 1, Errors: 0, Skipped: 2` (post-Sprint-38-fix baseline 1034 + 60 new Sprint 39 tests = 1094; the 1 inherited failure is `SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly:53` attributed to the dirty working-tree `system_prompt.txt` baseline carrying since Sprint 24-era; persists UNCHANGED through Sprint 39 since `system_prompt.txt` is UNTOUCHED). Any new failure delta is a **BLOCKING finding** (Sprint 39's behavioural equivalence must preserve).

  Spot-check the 60 new Sprint 39 tests pass:
  - `PhaseEvaluatorResolveSkillIntegrationTest` — 14 tests pass.
  - `SkillGuardrailDispatcherTest` — 24 tests pass.
  - `ResolveFaqGuardrailsTest` — 11 tests pass.
  - `ResolveIntakeGuardrailsTest` — 11 tests pass.
  
  Verify via `mvn test -Dtest=PhaseEvaluatorResolveSkillIntegrationTest,SkillGuardrailDispatcherTest,ResolveFaqGuardrailsTest,ResolveIntakeGuardrailsTest -q`.

- **Targeted regression spot-checks** (per Sprint 39 §6 fences + M2 §6 #21 M1 functional-surface preservation):
  - `Sprint71PartialIntakePersistenceTest` — verify all tests pass (M1 functional-surface preservation; the dispatcher-helper update at handoff §4.3 preserved all 14 assertions). Run `mvn test -Dtest=Sprint71PartialIntakePersistenceTest -q`.
  - `Sprint11ProgressiveResolveTest` — verify (Sprint 11 §M1 semantic preservation via dispatcher delegation to UNCHANGED `ResolveDispositionEvaluator`).
  - `Sprint12RuntimeAlignmentValidationTest` — verify.
  - `Sprint7IntakeStateTest` + `Sprint34IntakePrefillProjectionAndGuardTest` — verify (Sprint 7 §I2 semantic preservation; the 12 tests across these 2 files all pass via the dispatcher-helper pattern).
  - `AgentRunLoopS1FaqGroundedResolveGuardTest` — verify (Sprint 6 §G2 + NEW S1 coverage).
  - `PhaseEvaluatorSkillIntegrationTest` — verify (Sprint 38 13 tests still pass; note the test name `resolve_intakeUc_stillFollowsLegacyBranchUntilSprint39` is now stale post-migration per OQ-S39.7 but the assertion still holds).
  - `SkillTest` + `SkillRegistryTest` + `SkillLoaderTest` — verify (Sprint 38 + Sprint 38-fix tests still pass; `SkillLoaderTest` updated to expect 6 production Skills with Sprint 39 guardrails verified).
  - `PhaseEvaluatorPlanTest` + `PhaseEvaluatorFaqMissFallbackTest` + `PhaseEvaluatorMaxStepsResolverTest` + `PhaseEvaluatorQueryEnrichmentTest` — verify.
  - `IntakeFieldExtractor` tests — verify (M1 functional surface).

- **No interactive-eval rerun required** at Sprint 39 close. Per M2 §5 acceptance recalibration, interactive eval + bad-case suite are OBSERVATION ONLY for THIS milestone; not a hard gate. If Codex chooses to spot-check OPTIONALLY, results are informational and MAY regress; do NOT block on regression.

- **No real-LLM behavioural rerun required** at Sprint 39 close. Sprint 39 is architectural refactoring + bounded predicate landing; behavioural equivalence is testable in Java (the `PhaseEvaluatorResolveSkillIntegrationTest` 14-test golden-string suite is the gate). The §6.4 contract is "Skill-composed PhasePlan field-by-field equal to pre-migration golden" — Java-deterministic, not LLM-stochastic. The `SkillGuardrailDispatcher` predicate-handler tests mock `LlmInvocationService` ONLY for deterministic Java-logic dependency wiring per `feedback_mocked_llm_cannot_prove_prompt_causal_change.md`.

## 8. Tier-0 candidate continuation (Codex-substantive verification)

Sprint 37 close + Sprint 38 close + Sprint 38-fix close all pre-decided C2 + C3 QUALIFIED-DEFER. Sprint 39 honors the DEFER pre-decision per `feedback_constitution_discipline_vs_planning_anticipation.md`:

| Candidate | Statement | Sprint 39 implementation evidence | Recommendation |
|---|---|---|---|
| **C1** | Skill tool-whitelist enforcement unconditional | Sprint 39 `composeSkillPhasePlan(...)` composes `skill.toolsRequired` into `PhasePlan.allowedTools`; existing `ToolDispatcher.validateAgainstPlan` continues to enforce the whitelist post-migration. C1 REJECTED preserved by construction. | Codex confirms — no action |
| **C2** | Skill terminal predicate refusal non-overridable by LLM | **Sprint 39 ships the FIRST observed evidence surface** — the `SkillGuardrailDispatcher` with short-circuit-on-first-reject semantic + the 24 `SkillGuardrailDispatcherTest` tests covering 4 handler types × fire/no-fire scenarios + short-circuit verification. **HOWEVER**, the load-bearing claim for C2 is "LLM cannot override the rejection at runtime" — this requires production trace observation across the Sprint 39/40/41 window, NOT structural Java-logic tests alone. **DEFER preserved per `feedback_constitution_discipline_vs_planning_anticipation.md`**; R-item `R-skill-guardrail-non-overridability-tier-0` in `action_bank.md` §5.2 ready for M3+ revisit after Sprint 39 production evidence accumulates. | **Codex INDEPENDENT verdict REQUIRED** per §9 OQ-S39.5. Expected: DEFER (production evidence pending). If Codex DISAGREES (verdict: elevate NOW), route as `out_of_scope_review` per Sprint 37 §8 + Sprint 38 OQ-S38.5 precedent |
| **C3** | Skill `state_inheritance` enforced at session-state-bus boundary | Sprint 39 declares `state_inheritance.soft_signal_via_projection: [prior_use_case_carry]` on both RESOLVE Skill YAMLs but NOT enforced (no `SkillStateBus.java`; `ContextProjectionBuilder.java` UNCHANGED). Sprint 41 scope. DEFER preserved; R-item `R-skill-state-bus-boundary-enforcement-tier-0` ready for M3+ revisit after Sprint 41 evidence. | Codex independently re-evaluates: DEFER is sound (Sprint 41 hasn't shipped). If Codex DISAGREES, route as `out_of_scope_review` |
| **C4** | S1 `must_cite_source` predicate semantics | **Sprint 39 ships the implementation** — `handleMustCiteSource` 3-boundary check bounded per M2 §6 #4 verbatim authorization. The implementation IS the freeze-decision-bounded scope; C4 was NOT A CANDIDATE per Sprint 37 close (the freeze decided the predicate ships per M2 §6 #4 verbatim; the elevation question is whether the predicate refusal non-overridability becomes Tier-0, which is C2's question, not C4's). | Codex confirms — no action |
| **C5** | S2 `intake_complete_required` predicate semantics | Sprint 39 ships the implementation — `handleIntakeCompleteRequired` (Sprint 7 §I2 semantics moved). NOT A CANDIDATE per Sprint 37 close (S2 IS the existing predicate moved; no new Tier-0 question). | Codex confirms — no action |

**Critical: Sprint 39 dev surfaced NO new Tier-0 candidate from implementation.** Handoff §7 OQ-S39.5 explicitly defers C2 re-evaluation. If Codex SURFACES a new Tier-0 candidate from the Sprint 39 implementation diff (e.g., the `SkillGuardrailDispatcher` short-circuit semantic should be Tier-0 territory NOW; the `handleMustCiteSource` 3-boundary check should be Tier-0 territory; the `AgentRunLoopImpl` 5-arg vs 6-arg backward-compat constructor seam should be Tier-0 territory), raise as a NEW Finding for deliver-agent + human Tier-0 evaluation at Sprint 39 close. **Do NOT block Sprint 39 close on a NEW Tier-0 candidate elevation** — per `feedback_constitution_discipline_vs_planning_anticipation.md` + M2 §10 stop condition #1, Tier-0 elevation requires explicit human-review escalation; Codex's finding is informational input to the human's deliberation, NOT a Sprint 39 dev blocker.

## 9. Open question independent verification

Sprint 39 dev surfaced 7 OQs in handoff §7:

| OQ | Subject | Deliver-agent + human pre-decision | Codex action |
|---|---|---|---|
| **OQ-S39.1** | 6th template-substitution placeholder `{intake_required_fields}` beyond contract §2.2 D-c 5 | **AGREE WITH DEV** (anticipated by contract §2.2 D-e dev judgement clause; byte-for-byte equivalence requires it) | **Codex INFORMATIONAL** — surface dissent only if any placeholder slot is per-UC-pair (which would be a §1.7 violation); not blocking |
| **OQ-S39.2** | Dispatcher convenience overloads (`PhasePlan`-form + `Skill`-form) | **AGREE WITH DEV** | Codex INFORMATIONAL |
| **OQ-S39.3** | `RejectVerdict` + `DispatchContext` placement as separate files | **AGREE WITH DEV** (testability + readability) | Codex INFORMATIONAL |
| **OQ-S39.4** | `INTAKE_COMPLETE_GUARD_REJECT_REASON` constant relocation to dispatcher | **AGREE WITH DEV** (value preserved bit-for-bit) | Codex INFORMATIONAL — verify value strings preserved byte-for-byte |
| **OQ-S39.5** | **C2 Tier-0 candidate elevation given dispatcher landing** | **DEFER** per `feedback_constitution_discipline_vs_planning_anticipation.md` (production evidence pending) | **Codex INDEPENDENT verdict REQUIRED** — load-bearing OQ for Sprint 39 review. Expected: DEFER. If DISAGREE: route as `out_of_scope_review` per Sprint 37 §8 + Sprint 38 OQ-S38.5 precedent. Do NOT route as `fix_required` (Tier-0 elevation is human authority, not dev surface) |
| **OQ-S39.6** | `on_fail: downgrade_reason` not used in Sprint 39 | **DEFER to Sprint 40+** per design doc §9.6 OQ | Codex INFORMATIONAL — confirm no behavioural tension found that would justify switching |
| **OQ-S39.7** | Sprint 38 test name `resolve_intakeUc_stillFollowsLegacyBranchUntilSprint39` stale post-migration | **DEFER to M2 close housekeeping** per `doc_governance.md` rename-at-fold-back cadence | Codex INFORMATIONAL — surface the stale name; no rename required at Sprint 39 close |

OQ-S39.5 is the **load-bearing OQ for Sprint 39 Codex review** — Codex's verdict adjudicates whether the dispatcher landing's structural short-circuit semantic + 24 dispatcher unit tests constitute SUFFICIENT evidence to elevate C2 to Tier-0 NOW, OR whether the deferral holds pending production trace observation. The other 6 OQs are informational.

## 10. Deferred / non-blocking observations

- **PhaseEvaluator + AgentRunLoopImpl post-Sprint-39 line counts** — handoff cites `PhaseEvaluator.java` 1427 lines (−102) + `AgentRunLoopImpl.java` 638 lines (−149). Codex re-runs `wc -l` at HEAD `2f412b6`. If actuals materially differ, note as REPRODUCIBILITY observation per `feedback_deliver_agent_cited_numbers_must_be_reproducible.md` — informational; not blocking.
- **Behavioural-equivalence test scope** — 14 tests pinned to GOLDEN pre-migration legacy strings cover RESOLVE-FAQ × 7 UCs + RESOLVE-INTAKE × 5 UCs + 2 fallback paths. If Codex SURFACES a gap (e.g., a RESOLVE-FAQ UC not pinned; a placeholder substitution edge case not tested), note as informational + open R-item for M2 close revisit; not blocking for Sprint 39 (the §6.4 gate is "field-by-field equal for representative UCs"; 12 UCs is the dev-judgement minimum that the freeze permits).
- **`SkillTestFixtures.productionDispatcher()` coupling to production YAMLs** — same shape as Sprint 38 `productionRegistry()` coupling (OQ-S38.4 verdict AGREE WITH DEV). Codex confirms.
- **C2 + C3 R-items in `action_bank.md` §5.2** stay open through Sprint 39 close. Codex may note any disagreement with the deferral framing as informational input to the human's M2 close / M3+ planning round; not blocking for Sprint 39.
- **Sprint 39 contract §11 row "Wrong-containment rate unchanged or down" + "Over-escalation rate unchanged or down"** marked N/A per `feedback_mocked_llm_cannot_prove_prompt_causal_change.md`. Sprint 39 is architectural + bounded predicate landing; behavioural equivalence on Java composition is the gate; LLM behaviour observation OBSERVATION ONLY per M2 §5 recalibration. Codex confirms N/A is correct.
- **Sprint 39 contract §11 row "Architecture-health metrics not regressed"** YES per `new_semantic_hardcode_count` = 0; `soft_signal_conversion_count` += 6 (2 RESOLVE phase Skills externalized + 3 predicate migrations + 1 NEW S1; cumulative M2 count = Sprint 38's 4 + Sprint 39's 6 = 10 architectural conversions post-Sprint-39); `planner_ownership_ratio` unchanged; `shadow_disagreement_rate` not measured.
- **Editorial fold-back deferred to M2 close**: any divergence Codex finds between Sprint 37 freeze design doc §6.2.5 / §6.2.6 templates' editorial paraphrasing and the legacy-verbatim YAML content shipped by Sprint 39 is M2-close fold-back per `doc_governance.md` cadence + Sprint 38 OQ-S38.1 precedent. Route as `out_of_scope_review` (NOT `fix_required`) per the same disagreement-routing the Sprint 38 review prompt encoded. Open R-item `R-skill-design-doc-template-fold-back` if not already opened from Sprint 38 close.

## 11. Output format (write to `docs/codex-findings.md`)

Replace the file content with the standard §4.2 sprint-close header followed by the structured sections below. Per `feedback_packaging_codex_findings_supersession.md`: delete-and-add the prior scaffold to ensure no editorial drift across sprints. Per the deliver-agent close convention: the deliver-agent will archive this file to `docs/sprints/sprint-039-codex-review.md` at Sprint 39 close.

```
## Sprint Review Decision
decision: pass | fix_required | out_of_scope_review
blocking_count: <number>
summary: <one paragraph>

## Review Evidence
<bullet list — review scope (Sprint 39 commit 2f412b6 against pre-dev HEAD 5787806; ~2191 insertions / 539 deletions across 21 files; 2 NEW RESOLVE Skill YAMLs + 1 NEW dispatcher Java class + 2 NEW Java records + PhaseEvaluator integration extension + AgentRunLoopImpl migration + 4 NEW test files + 8 EDIT test files + dev handoff); what you re-ran (Java baseline `mvn test -q`; targeted regression spot-checks per §7; cited file:line ranges spot-checked); what passed; what was independently verified (Sprint 37 freeze fidelity per §5 + §6; behavioural-equivalence golden-string preservation per §5; Sprint 6/7/11 predicate semantic preservation in dispatcher handlers per §4 + §5; S1 3-boundary bounded scope per §4 + §5; dispatcher bounded to 4 typed predicate types per §4; Sprint 11/11.1/12 frozen surface preservation via UNCHANGED ResolveDispositionEvaluator per §4 + §5; M1 functional-surface preservation per §5; no scope creep into Sprint 40/41 surfaces per §2 + §5; premise re-verification per §5)>

## Blocking Findings (if any)
<numbered list; each entry quotes the diff snippet OR Java file:line / Skill YAML body + cited reference; references back to §2 scope-discipline / §4 §1.7 boundary / §5 hard-fence / §8 Tier-0 / §9 OQ-S39.5>

## Anti-Hardcode Kernel (§3)
<nine-question walk against the Sprint 39 IMPLEMENTATION DIFF; each Q with one-line verdict; cross-reference dev handoff §6 self-walk; Codex agreement or dissent per Q with diff snippet quoted where dissent>

## §1.7 Boundary Check (§4)
<the §4 implementation boundary checks; pass/fail per surface: 2 RESOLVE Skill YAML procedure/grounding/escalation bodies legacy-verbatim + no eval-phrase encoding; explicit applicable_use_cases lists match pre-Sprint-39 FAQ/INTAKE partition; state_inheritance state-dimension-keyed; guardrails in declaration order with correct types/on_fail/parameters; dispatcher bounded to 4 typed types not generic rule engine; handleMustCiteSource 3-boundary check matches M2 §6 #4 verbatim; handleIntakeCompleteRequired matches Sprint 7 §I2 byte-for-byte; handlePrematureResolveOutcomeGuard verbatim delegation to UNCHANGED ResolveDispositionEvaluator; SkillLoader allowlists UNCHANGED; no semantic ownership shift>

## Hard-Fence Verification (§5)
<Sprint 39 contract §6 37 fences + M2 §6 22 fences + behavioural equivalence + Sprint 37 freeze fidelity table + MATERIAL FINDING preservation + Sprint 33 cue/slot split preservation + premise re-verification; pass/fail per fence>

## Schema And Reproducibility Checks (§6)
<RESOLVE Skill YAML schemas per design doc §6.2.5 + §6.2.6; SkillGuardrailDispatcher data shape per design doc §9.1-§9.4; RejectVerdict + DispatchContext record shapes; PhaseEvaluator integration; AgentRunLoopImpl migration; test file structure (14 + 24 + 11 + 11 = 60 new tests); line-count verifications (PhaseEvaluator 1427 + AgentRunLoopImpl 638 + SkillGuardrailDispatcher 416); cited file:line spot-checks (5-6 ranges) reproduced>

## Validation Runs (§7)
<the §7 results; `mvn test -q` output; baseline preservation 1094/1-inherited/0/2; 60 new Sprint 39 tests pass; targeted regression spot-checks Sprint71PartialIntakePersistenceTest + Sprint11ProgressiveResolveTest + Sprint7IntakeStateTest + Sprint34IntakePrefillProjectionAndGuardTest + Sprint12RuntimeAlignmentValidationTest + AgentRunLoopS1FaqGroundedResolveGuardTest + Sprint 38 tests preserved>

## Tier-0 Candidate Continuation (§8)
<the §8 walk; CONFIRM or DISAGREE per candidate (C1 REJECTED preserved by construction; C2 + C3 QUALIFIED-DEFER continuation per Sprint 37 close + Sprint 38 close pre-decision; C4 + C5 N/A — implementation is the freeze-decision-bounded scope); flag any NEW Tier-0 candidate Codex surfaces from Sprint 39 implementation diff as informational input to deliver-agent + human for Sprint 39 close / M2 close revisit>

## OQ Independent Verification (§9)
<the §9 walk; OQ-S39.5 INDEPENDENT verdict on C2 dispatcher Tier-0 elevation timing (DEFER vs ELEVATE NOW; if DISAGREE → route to `out_of_scope_review`); OQ-S39.1-4/6/7 informational notes if any>

## Deferred / Non-Blocking Notes (§10)
<the §10 items; line-count reproducibility + behavioural-equivalence test scope + SkillTestFixtures coupling + C2/C3 R-items + N/A acceptance bar rows + architecture-health metric counts + editorial fold-back routing>
```

## 12. Expected verdict shape

If all gates pass + Sprint 39 honors Sprint 37 freeze decisions (e §6.2.5 + §6.2.6) + (g §8.2.1-§8.2.5) + (h §9) verbatim + behavioural equivalence preserved per §6.4 + no scope creep + Java baseline 1094/1-inherited/0/2 holds + Codex's OQ-S39.5 verdict aligns with dev (C2 DEFER): **`decision: pass / blocking_count: 0`**. The cleanest outcome for a multi-fence-converging implementation sub-sprint where the dev shipped per spec + behavioural equivalence verified + Sprint 11/11.1/12 frozen surface preserved + M1 functional surfaces preserved + S1 bounded per M2 §6 #4 verbatim authorization.

If §2 scope-discipline fails (e.g., dev commit touched `ResolveDispositionEvaluator.java`, `IntakeFieldsRegistry.java`, `system_prompt.txt`, `ContextProjectionBuilder.java`, a deliver-agent-owned doc, or added a NEW `SkillStateBus.java`): **`decision: fix_required`** with the violating file path + line range quoted as Finding #1.

If §4 §1.7 boundary check fails (e.g., a per-UC-branch if-else in a Skill YAML body OR dispatcher Java; `handleMustCiteSource` expansion beyond 3 boundaries; `handleIntakeCompleteRequired` deviates from Sprint 7 §I2 semantics; `handlePrematureResolveOutcomeGuard` deviates from `ResolveDispositionEvaluator` delegation; new entry to `SkillLoader.VALID_GUARDRAIL_TYPES` / `VALID_PROJECTION_SLOTS` / `VALID_TOOL_NAMES` in Sprint 39 commit; generic rule engine instead of bounded 4-type dispatcher): **`decision: fix_required`** with the violating diff snippet quoted; cite the relevant §1.7 forbidden-list line + M2 §6 #1 / #4 / #5 / #25 / #26 / #27 fences.

If §5 behavioural-equivalence verification fails (e.g., `PhaseEvaluatorResolveSkillIntegrationTest` golden-string assertion fails on any of 14 cases OR the post-Sprint-39 RESOLVE composition shows divergence from parent `5787806` legacy branches at the placeholder-substitution surface): **`decision: fix_required`** with the failing test name + diff snippet quoted.

If §5 hard-fence verification fails (e.g., `ResolveDispositionEvaluator.java` modified; `system_prompt.txt` modified; `ContextProjectionBuilder.java` modified; Sprint 33 cue migration drops content; M1 `Sprint71PartialIntakePersistenceTest` regressed): **`decision: fix_required`** with the violating fence + file path quoted.

If §7 Java baseline regression (e.g., `mvn test -q` returns `Tests run: ≠1094 OR Failures: >1` OR a new failure delta beyond the 1 inherited): **`decision: fix_required`** with the failing test names quoted.

If §6 schema check fails (e.g., a RESOLVE Skill YAML lacks a required field; a guardrail block declares an unknown type; a `tools_required` list names a non-existent tool; a state slot is not in the existing allowlist): **`decision: fix_required`** with the violating YAML body quoted.

If Codex's OQ-S39.5 INDEPENDENT verdict comes back DISAGREEING with the dev (Codex believes C2 should be elevated NOW given dispatcher landing): **`decision: out_of_scope_review`** with the C2 elevation argument + production-evidence-pending argument both quoted; recommend deliver-agent + human evaluate at Sprint 39 close per M2 §10 stop condition #1; **Do NOT classify as `fix_required`** (Tier-0 elevation is human authority).

If Codex DISAGREES with the Tier-0 candidate continuation per §8 (e.g., believes Sprint 39's dispatcher short-circuit semantic + 24 unit tests already constitute structural Tier-0 territory that should be elevated NOW): **`decision: out_of_scope_review`** — same rationale as OQ-S39.5.

If Codex SURFACES a NEW Tier-0 candidate from the Sprint 39 implementation diff (not C1-C5): **`decision: out_of_scope_review`** with the candidate + surface quoted; deliver-agent + human evaluate at Sprint 39 close. Do NOT block Sprint 39 close on a NEW Tier-0 candidate elevation.

If Codex DISAGREES with the legacy-verbatim YAML content vs Sprint 37 freeze §6.2.5 / §6.2.6 templates' editorial paraphrasing (Sprint 38 OQ-S38.1 precedent reincarnated for RESOLVE Skills): **`decision: out_of_scope_review`** with the template-vs-legacy divergence quoted; recommend M2 close editorial fold-back per `doc_governance.md` code-ahead-of-docs. Open R-item `R-skill-design-doc-template-fold-back` if not already opened.

If a substance concern is found that is NOT in scope for Sprint 39 (e.g., a critique of the Sprint 37 design freeze itself; a critique of the M2 milestone scope; a critique of `system_prompt.txt` Sprint 23/31/33 teaching paragraphs — Sprint 40 scope; a critique of `ContextProjectionBuilder.java` `prior_use_case_carry` slot enforcement — Sprint 41 scope): **`decision: out_of_scope_review`** with the concern named and the milestone-contract / freeze / Sprint-40/41-scope reference cited.

## 13. Self-check before submitting

- [ ] §2 scope-discipline gate walked; every disallowed surface checked against `git diff 5787806..2f412b6 --stat`. Especially: `ResolveDispositionEvaluator.java` + `IntakeFieldsRegistry.java` + `UseCaseRegistryService.java` + `system_prompt.txt` + `ContextProjectionBuilder.java` + `RuntimeIntentClassifier.java` + `INTAKE_UCS` + `escalation_reason` enum + `SkillLoader` allowlists all UNCHANGED.
- [ ] §3 §4.1 nine-question kernel walked against the Sprint 39 IMPLEMENTATION DIFF per the canonical kernel loaded from `iteration_governance.md` §4.1; per-Q verdict aligned with or independently dissenting from dev handoff §6 self-walk. Especially: Q1 anti-per-UC-branch verified across SkillRegistry select + composeSkillPhasePlan + substitutePlaceholders + 4 dispatcher handlers + 2 RESOLVE Skill YAML bodies.
- [ ] §4 §1.7 boundary check walked against 2 RESOLVE Skill YAML bodies + dispatcher Java + 4 handler bodies + `handleMustCiteSource` 3-boundary M2 §6 #4 verbatim authorization + `handleIntakeCompleteRequired` Sprint 7 §I2 byte-for-byte + `handlePrematureResolveOutcomeGuard` verbatim delegation + `SkillLoader` allowlists UNCHANGED + no generic rule engine + 4-type dispatcher boundedness.
- [ ] §5 hard-fence verification (Sprint 39 contract §6 37 items + M2 §6 22 items + behavioural equivalence golden-string preservation across 14 cases + Sprint 37 freeze fidelity + MATERIAL FINDING preservation + Sprint 33 cue/slot split preservation + Sprint 11/11.1/12 frozen surface preservation + M1 functional-surface preservation + premise re-verification).
- [ ] §6 schema + reproducibility (RESOLVE Skill YAML schemas + dispatcher data shape + record shapes + PhaseEvaluator integration + AgentRunLoopImpl migration + test file structure + line-count verifications + cited file:line spot-checks).
- [ ] §7 Java baseline re-run from clean checkout; 1094/1-inherited/0/2 byte-identical; targeted regression spot-checks pass.
- [ ] §8 Tier-0 candidate continuation (C1 REJECTED by construction + C2 + C3 QUALIFIED-DEFER continuation pending production evidence + C4 + C5 N/A); no NEW Tier-0 candidate surfaced (or surfaced + flagged for human escalation).
- [ ] §9 OQ independent verification (OQ-S39.5 INDEPENDENT verdict on C2 dispatcher elevation timing; OQ-S39.1-4/6/7 informational).
- [ ] §10 deferred items noted as non-blocking.
- [ ] `docs/codex-findings.md` written per §11 format.
- [ ] Verdict per §12 expected shape.
