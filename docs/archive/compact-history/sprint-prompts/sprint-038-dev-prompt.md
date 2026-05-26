Paste the content below this line into a fresh Claude Code session on branch `refactor/remove-the-shackles`.

---

# Sprint 38 dev prompt (Claude Code session) — NEW M2 sub-sprint 2 = SkillRegistry core + 4 simpler phase Skills migration

**Branch:** `refactor/remove-the-shackles`
**Repo:** `/Users/caoruixin/projects/csagent-latest`
**Sub-sprint type:** implementation (Java + YAML); first implementation sub-sprint after Sprint 37 design freeze.
**Codex review:** per-sub-sprint at Sprint 38 close per §4.3 trigger #3 (new architectural surface).

You are dev-agent (Claude Code) for **Sprint 38**, the SECOND sub-sprint of NEW Milestone M2 (Skill Registry Abstraction + Wholesale Retroactive Externalization). Your task is to land the SkillRegistry core abstraction + the 4 simpler phase Skills (DISCOVER + CONFIRM + ESCALATE + TERMINAL/CLOSE) as externalized YAML + PhaseEvaluator integration for those 4 phases, per Sprint 37 freeze decisions (a)/(b)/(c)/(e §6.2.1-§6.2.4).

> **Important orientation:** Sprint 37 (NEW M2 sub-sprint 1) shipped a design freeze at `docs/proposals/skill_registry_design.md` (3633 lines, 13 sections, 10 decisions a-j locked). Sprint 37 closed PASS A 2026-05-17 (commit `51c327c`; Codex per-sub-sprint review `pass / 0` at `docs/sprints/sprint-037-codex-review.md`). The Sprint 37 freeze is **immutable** per `doc_governance.md` — do NOT edit it. Sprint 38's job is to **implement** the freeze decisions (a)/(b)/(c)/(e §6.2.1-§6.2.4) verbatim. The other decisions (d) procedure-vs-guardrails split + (e §6.2.5-§6.2.6) RESOLVE migration + (f) Sprint 23/31/33 teaching extraction + (g) Sprint 6/7/11 + S1+S2 predicate migration + (h) unified dispatcher + (i) state model + state_inheritance + (j) §4.1 walk are CONTEXTUAL ONLY for Sprint 38; downstream sub-sprints (Sprint 39 / Sprint 40 / Sprint 41) implement them.

---

## 1. Read order on session start

Read these in order before doing anything else. Cite each file in your handoff §1 Context Pack as you read it.

1. `AGENTS.md` (auto-loaded transitively: `docs/current/doc_governance.md` + `docs/current/agent_context_guide.md` + `docs/current/iteration_governance.md`).
2. `docs/sprint_objective.md` — Sprint 38 contract (the binding scope for this sub-sprint). Pay attention to §2 Goal (decisions (a)/(b)/(c)/(e §6.2.1-§6.2.4) implementation; 4 deliverables D-a/D-b/D-c/D-e), §3 Non-goals, §6 Files NOT in scope (31 hard fences), §7 Bundle policy, §10 Stop conditions (20 items).
3. `docs/milestone_objective.md` — NEW M2 contract (context). Pay attention to §2 Goal, §3 Sub-sprint sequence (your sub-sprint is the second row), §5 acceptance bar recalibration (Alice + interactive eval = OBSERVATION, NOT gate), §6 hard fences (22 items at the milestone level).
4. `docs/proposals/skill_registry_design.md` — **the Sprint 37 design freeze** (the architectural authority for Sprint 38). Read these sections carefully:
   - §1 Background + relation to upstream (esp. §1.1 what this freeze decides; §1.5 research-agent proposal verbatim; §1.6 architectural diagram; §1.7 what this freeze does NOT decide).
   - **§2 Decision (a) — Skill data model** (the 12-field record + JSON Schema validation contract) — Sprint 38 implements verbatim as Java record in `Skill.java`.
   - **§3 Decision (b) — SkillRegistry shape** (Java class location + `select(phase, useCase)` semantics + SkillLoader fail-fast at Spring boot) — Sprint 38 implements verbatim as `SkillRegistry.java` + `SkillLoader.java`.
   - **§4 Decision (c) — PhaseEvaluator integration** (composeSkillPhasePlan helper + backward-compatible PhasePlan + mixed Sprint 38/39 migration window) — Sprint 38 implements verbatim by editing `PhaseEvaluator.java`.
   - **§6.2.1 DISCOVER → `discover_triage.yaml` template** — Sprint 38 creates the file verbatim from the YAML template at §6.2.1.
   - **§6.2.2 CONFIRM → `confirm.yaml` template** — Sprint 38 creates the file verbatim.
   - **§6.2.3 ESCALATE → `escalate.yaml` template** — Sprint 38 creates the file verbatim (note: `create_case_controlled` INTENTIONALLY excluded per §6.2.3 note + Codex 1.8 / `customer_service_tool_spec_v0_2.yaml` runtime-only visibility rule per PhaseEvaluator inline comment at lines 514-521).
   - **§6.2.4 CLOSE → `terminal.yaml` template** — Sprint 38 creates the file verbatim (`applicable_phases: [CLOSE]` per OQ-7.1 default; keep enum unchanged at Sprint 38; file named `terminal.yaml` carries architectural intent for the M3+ possible rename).
   - §6.4 behavioural-equivalence test pattern — Sprint 38 implements as `PhaseEvaluatorSkillIntegrationTest`.
5. `docs/proposals/skill_registry_design.md` — CONTEXTUAL ONLY for Sprint 38 (other sections); read briefly:
   - §5 (decision (d) procedure-vs-guardrails split): for the 4 simpler phase Skills, `guardrails: []` (empty) per §6.2.1-§6.2.4. Predicates land at Sprint 39 in §6.2.5-§6.2.6 Skills.
   - §6.2.5 + §6.2.6 (RESOLVE-INTAKE + RESOLVE-FAQ Skill templates): **NOT shipped in Sprint 38** (Sprint 39 scope). The legacy PhaseEvaluator branches at lines 552-665 stay UNCHANGED for Sprint 38.
   - §7 (Sprint 23/31/33 teaching paragraph migration): **NOT shipped in Sprint 38** (Sprint 40 scope). `system_prompt.txt` UNTOUCHED.
   - §8 (Sprint 6/7/11 + NEW S1 + S2 predicate migration): **NOT shipped in Sprint 38** (Sprint 39 scope). `AgentRunLoopImpl.java` UNCHANGED.
   - §9 (unified Skill terminal-predicate dispatcher): **NOT shipped in Sprint 38** (Sprint 39 scope; depends on Skill `guardrails` declarations being populated, which Sprint 39 ships).
   - §10 (session-level state model + state_inheritance + `prior_use_case_carry`): **NOT shipped in Sprint 38** (Sprint 41 scope). `SkillStateBus.java` NOT created; `ContextProjectionBuilder.java` UNTOUCHED. Per-Skill `state_inheritance` block IS populated in the 4 Skill YAMLs (schema-defined per §10.2), but NOT yet enforced at session-state-bus boundary.
   - §11 (§4.1 nine-question walk on PROPOSED design): contextual; informs your Sprint 38 §8 stanza self-walk on the implementation diff.
   - §12 (Tier-0 candidate enumeration; C2 + C3 QUALIFIED-DEFER opened as R-items in `docs/action_bank.md` §5.2 for M3+ revisit): contextual; do NOT elevate any candidate in Sprint 38 dev (per Sprint 37 close pre-decision: DEFER both C2 + C3).
   - §13 (downstream sub-sprint reference index): contextual; cross-reference to confirm Sprint 38 scope matches the freeze.
6. `docs/sprints/sprint-037-handoff.md` — Sprint 37 dev's handoff (historical context for what was decided + what premise refinements surfaced). Pay attention to:
   - §1.2 code shape table verified at HEAD `51c327c` — provides line numbers for `PhaseEvaluator.java`, `AgentRunLoopImpl.java`, `IntakeFieldsRegistry.java`, `UseCaseRegistryService.java`, `ContextProjectionBuilder.java`, `system_prompt.txt`, `PhasePlan.java`, `RecordOutcomeTool.java`, `EscalationReasonResolver.java` — Sprint 38 dev SHALL re-verify these at session start at HEAD (which is now post-Sprint-37-close).
   - §3 premise re-verification table — the 10 premises from Sprint 37 contract §4 plus the §13.2 premise refinements (premise #7 `system_prompt.txt` 101 lines; Sprint 33 cue/slot split). Sprint 38 inherits these; verify any drift.
   - §6 Tier-0 candidate outcome — 5 candidates surfaced; C2 + C3 QUALIFIED-DEFER (R-items opened); C1 REJECTED; C4 + C5 NOT-A-CANDIDATE. No Sprint 38 action on Tier-0.
   - §7 11 OQs disposition — OQ-7.1 (CLOSE→TERMINAL rename) default kept enum unchanged at Sprint 38 planning; OQ-7.11 (decision tree §4.1 Q6) AGREE WITH DEV; other OQs deferred to downstream sub-sprint planning rounds.
7. `docs/sprints/sprint-037-codex-review.md` — Codex per-sub-sprint review of Sprint 37 (`decision: pass / blocking_count: 0`); read for: Codex's independent verification of Sprint 37 freeze quality (load-bearing for Sprint 38's trust in the freeze); Codex's §10 deferred / non-blocking notes (MATERIAL FINDING shapes Sprint 39, NOT Sprint 38; premise #7 shapes Sprint 40, NOT Sprint 38; Sprint 33 cue/slot split must be preserved exactly through Sprint 38 + Sprint 40; C2 + C3 R-items for M3+ revisit).
8. Code shape re-verification at session start (one read each; cite line numbers in handoff §3):
   - `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java` — total line count; phase dispatch switch at ~355-360; per-phase branches at DISCOVER ~397-457 / CONFIRM ~462-487 / CLOSE ~492-509 / ESCALATE ~513-542 / RESOLVE-INTAKE ~552-596 / RESOLVE-FAQ ~598-665; INTAKE_UCS at ~30; escalation_reason enum at ~33-63; UC_TEAM_NAME map at ~112; buildIntakeSystemInstruction at ~207-208.
   - `server/src/main/java/com/gumtree/csagent/model/PhasePlan.java` — total line count; existing record fields (per Sprint 37 freeze §4.2: backward-compatible 11-field record).
   - `server/src/main/java/com/gumtree/csagent/service/runtime/AgentRunLoopImpl.java` — total line count; Sprint 6/7/11 predicates at ~628-651 / 690-734 / 745-759; dispatch sites at ~317 / 356 / 413; FAQ_PATH_UCS at ~106-108; INTAKE_COMPLETE_GUARD_REJECT_REASON at ~120. **PRESERVED UNCHANGED in Sprint 38; Sprint 39 migrates.**
   - `server/src/main/java/com/gumtree/csagent/service/runtime/IntakeFieldsRegistry.java` — total line count; REQUIRED_FIELDS_BY_UC at ~53; canonicalFieldName at ~127; requiredFieldsFor at ~118; intakeComplete at ~184. **UNCHANGED in Sprint 38.**
   - `server/src/main/java/com/gumtree/csagent/service/runtime/UseCaseRegistryService.java` — total line count; UseCaseDefinition record at ~166-173 (6 fields). **UNCHANGED in Sprint 38; Sprint 38 reads `ucDef.name()` for `{uc_name}` placeholder substitution if any (not needed for Sprint 38 since 4 simpler phase Skills are non-parameterized).**
   - `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java` — total line count; intake_state slot at ~346-377; alternate_candidate_use_cases at ~396-416; discover_disambiguation_signals at ~418-432; already_called at ~704. **UNCHANGED in Sprint 38; Sprint 41 adds NEW `prior_use_case_carry` slot.**
   - `server/src/main/resources/prompts/system_prompt.txt` — total line count 101 per Sprint 37 premise #7; Sprint 23 23-28; Sprint 31 30-34; Sprint 33 36-43; DISCOVER 45-52; request_handover decision tree 54-101. **UNCHANGED in Sprint 38; Sprint 40 extracts teaching paragraphs.**
   - `server/src/main/resources/skills/` — verify directory does NOT exist at session start. Sprint 38 creates it.
9. Memory feedbacks (load relevant files from `.claude/agent-memory/sprint-deliver-orchestrator/`):
   - `feedback_constitution_discipline_vs_planning_anticipation.md` — do NOT silently edit governance docs even if implementation "logically suggests" an edit. Especially: do NOT elevate C2 or C3 to Tier-0 at Sprint 38 dev — DEFER is the pre-decision; R-items in action_bank §5.2 carry forward to M3+.
   - `feedback_commit_at_end_bundles_deliver_artefacts.md` — dev does NOT stage deliver-agent-owned files (`docs/sprint_objective.md` / `docs/milestone_objective.md` / `docs/10-handoff.md` / `docs/action_bank.md` / `docs/codex-findings.md` / `compact/sprint-038-*.md`). Sprint 38 dev commit bundles only the §5 Sprint 38 contract files.
   - `feedback_deliver_agent_cited_numbers_must_be_reproducible.md` — every code-citation in handoff cites file path + line numbers (extraction recipe).
   - `feedback_mocked_llm_cannot_prove_prompt_causal_change.md` — Sprint 38 ships no real-LLM eval rerun as primary evidence; Java composition logic tests + behavioural-equivalence tests MAY mock LLM (those are deterministic Java logic, not LLM behaviour). Behavioural equivalence on PhasePlan composition is testable in Java; that's the gate.
   - `feedback_handoff_verdict_section_delegation.md` — leave handoff §12 closure verdict as PLACEHOLDER; deliver-agent + human + Codex fill at Sprint 38 close.

---

## 2. Task — implement Sprint 37 freeze decisions (a)/(b)/(c)/(e §6.2.1-§6.2.4)

Sprint 38 §2 Goal in the contract enumerates 4 deliverables D-a / D-b / D-c / D-e. Read the contract §2 verbatim; you implement that. Concrete implementation walkthrough:

### 2.1 D-a — Skill data class (`Skill.java`)

Create `server/src/main/java/com/gumtree/csagent/service/runtime/skill/Skill.java` as a Java record with 12 fields per design doc §2.1:

```java
package com.gumtree.csagent.service.runtime.skill;

import java.util.List;

public record Skill(
    String name,
    String description,
    List<String> applicablePhases,           // enum subset: DISCOVER / CONFIRM / RESOLVE / ESCALATE / CLOSE
    List<String> applicableUseCases,         // enum subset OR ["*"] wildcard
    List<String> toolsRequired,              // canonical tool names; PhasePlan.allowedTools composed from this
    List<String> requiredContextKeys,        // context keys required for this Skill
    int maxToolSteps,
    boolean allowInterimMessage,
    List<String> validTerminalOutcomes,      // enum subset: CLARIFICATION_NEEDED / FINAL_ANSWER / ESCALATE
    String objective,
    String procedure,
    String groundingInstruction,
    String escalationPolicy,
    List<Guardrail> guardrails,              // empty list for Sprint 38's 4 simpler phase Skills; populated in Sprint 39
    StateInheritance stateInheritance        // populated per design doc §6.2.1-§6.2.4 templates
) {}
```

Define supporting types:

- `Guardrail` record (placeholder for Sprint 38; populated in Sprint 39). Sprint 38 ships an empty record OR a minimal `record Guardrail(String type, String onFail, Map<String, Object> parameters)` per design doc §5.2 schema; the field is present in the YAML schema but Sprint 38's 4 Skills all have `guardrails: []`.
- `StateInheritance` record per design doc §10.2 schema (placeholder for full Sprint 41 enforcement): `record StateInheritance(List<String> inherit, List<String> reset, List<String> softSignalViaProjection) {}`. Sprint 38 populates the field per the 4 Skill YAML templates but does NOT yet enforce at session-state-bus boundary (Sprint 41 scope).

JSON Schema validation contract per design doc §2.2: a `Skill.json` schema file at `server/src/main/resources/skills/_schema/Skill.json` (or equivalent path; dev picks per repo convention). The SkillLoader fails fast at boot if any Skill YAML doesn't validate per the schema. Sprint 38 ships the schema file IF Jackson YAML schema validation is the loader's approach; otherwise (e.g., manual Java validation in SkillLoader) the schema is encoded in Java assertion code. Dev picks the simpler path consistent with the existing repo (likely manual Java validation if repo doesn't already pull in JSON Schema validator).

### 2.2 D-b — SkillRegistry + SkillLoader (`SkillRegistry.java` + `SkillLoader.java`)

Create `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillRegistry.java`:

```java
package com.gumtree.csagent.service.runtime.skill;

import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;

@Component
public class SkillRegistry {
    private final List<Skill> skills;

    public SkillRegistry(SkillLoader loader) {
        this.skills = loader.loadAll();
    }

    public Optional<Skill> select(String phase, String useCaseId) {
        // Per design doc §3.2: exact-match (applicablePhases contains phase AND
        // applicableUseCases contains useCaseId) → wildcard match (applicablePhases
        // contains phase AND applicableUseCases contains "*") → Optional.empty().
        return skills.stream()
            .filter(s -> s.applicablePhases().contains(phase))
            .filter(s -> s.applicableUseCases().contains(useCaseId) || s.applicableUseCases().contains("*"))
            .findFirst();
    }

    public List<Skill> allSkills() {
        return List.copyOf(skills);
    }
}
```

Create `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillLoader.java`:

```java
package com.gumtree.csagent.service.runtime.skill;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

@Component
public class SkillLoader {
    private static final ObjectMapper YAML = new YAMLMapper();

    public List<Skill> loadAll() {
        // Per design doc §3.3: read all *.yaml from server/src/main/resources/skills/
        // at @PostConstruct or constructor; parse + validate; fail-fast on schema error.
        var resolver = new PathMatchingResourcePatternResolver();
        try {
            var resources = resolver.getResources("classpath:skills/*.yaml");
            var loaded = new ArrayList<Skill>();
            for (Resource r : resources) {
                Skill skill = YAML.readValue(r.getInputStream(), Skill.java);
                validate(skill, r.getFilename());
                loaded.add(skill);
            }
            return loaded;
        } catch (Exception e) {
            throw new IllegalStateException("SkillLoader failed: " + e.getMessage(), e);
        }
    }

    private void validate(Skill skill, String filename) {
        // Per design doc §2.2 + §3.3: validate required fields present,
        // applicable_phases enum subset, applicable_use_cases enum subset
        // (or ["*"]), valid_terminal_outcomes enum subset, tools_required
        // non-empty, etc. Fail fast on any violation.
        if (skill.name() == null || skill.name().isBlank()) {
            throw new IllegalStateException("Skill in " + filename + " missing required field: name");
        }
        // ... full validation per design doc §2.2 schema
    }
}
```

(The above is illustrative; adapt to actual repo Spring patterns + existing Jackson YAML usage if any.)

### 2.3 D-c — PhaseEvaluator integration

Modify `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java`:

1. **Add `composeSkillPhasePlan(Skill, Phase, String, BotSession) → PhasePlan` private helper** per design doc §4.1. The helper composes the selected Skill with session state into a PhasePlan with the same field set as the legacy per-phase branches:

```java
private PhasePlan composeSkillPhasePlan(Skill skill, Phase phase, String useCaseId, BotSession session) {
    return PhasePlan.builder()
        .phase(phase)
        .useCase(useCaseId)
        .objective(skill.objective())
        .allowedTools(skill.toolsRequired())
        .requiredContextKeys(skill.requiredContextKeys())
        .maxToolSteps(skill.maxToolSteps())
        .allowInterimMessage(skill.allowInterimMessage())
        .validTerminalOutcomes(skill.validTerminalOutcomes())
        .systemInstruction(skill.procedure())
        .groundingInstruction(skill.groundingInstruction())
        .escalationPolicy(skill.escalationPolicy())
        .build();
}
```

(The above is illustrative; adapt to actual PhasePlan record / builder shape.)

2. **Modify the `evaluate(...)` dispatch switch at lines 355-360** to query `skillRegistry.select(phase, useCase)` FIRST; if a Skill is returned, use `composeSkillPhasePlan(...)`; otherwise fall through to the existing per-phase branch:

```java
public PhasePlan evaluate(BotSession session, ...) {
    Phase phase = session.getPhase();
    String useCaseId = session.getActiveUseCase();

    // NEW: SkillRegistry-driven composition for migrated phases
    Optional<Skill> skillOpt = skillRegistry.select(phase.name(), useCaseId);
    if (skillOpt.isPresent()) {
        return composeSkillPhasePlan(skillOpt.get(), phase, useCaseId, session);
    }

    // LEGACY: existing per-phase branches for unmigrated phases (RESOLVE-INTAKE, RESOLVE-FAQ)
    return switch (phase) {
        case RESOLVE -> /* existing RESOLVE-INTAKE branch at lines 552-596 OR RESOLVE-FAQ branch at 598-665 */;
        // ...
    };
}
```

3. **DELETE the per-phase branch code for the 4 migrated phases** (DISCOVER 397-457 + CONFIRM 462-487 + CLOSE 492-509 + ESCALATE 513-542). Per Sprint 38 contract §5, the deletion is part of D-c; behavioural-equivalence tests cover the migration. Alternative: keep both paths with a feature-flag-style guard for safer rollback (dev picks; recommend deletion since the migration is the whole point + Java tests cover equivalence).

4. **PRESERVE the legacy RESOLVE-INTAKE branch at lines 552-596 and RESOLVE-FAQ branch at 598-665 UNCHANGED.** Sprint 39 migrates these.

### 2.4 D-e — 4 Skill YAML files

Create 4 YAML files at `server/src/main/resources/skills/`:

- `discover_triage.yaml` — verbatim from design doc §6.2.1 template
- `confirm.yaml` — verbatim from design doc §6.2.2 template
- `escalate.yaml` — verbatim from design doc §6.2.3 template (note: `create_case_controlled` INTENTIONALLY excluded per design doc §6.2.3 note)
- `terminal.yaml` — verbatim from design doc §6.2.4 template (`applicable_phases: [CLOSE]`; file named `terminal.yaml` carries architectural intent)

For each YAML, the body should be **bit-for-bit equivalent** (modulo whitespace normalization) to the design doc template. The migration is mechanical; do NOT add or remove fields beyond what the template specifies. If you find any inconsistency between the design doc template and the legacy `PhaseEvaluator.java` content for that phase (e.g., template has slightly different wording than legacy), STOP and surface in handoff §7 OQ — the design doc is the freeze authority, but if the legacy content has shifted since Sprint 37 close (premise drift), the deliver-agent + human should adjudicate.

### 2.5 Tests

Create 4 test files:

- `server/src/test/java/com/gumtree/csagent/service/runtime/skill/SkillTest.java` — unit tests for Skill record (field accessors; default `guardrails: []` + `state_inheritance: { inherit: [], reset: [], soft_signal_via_projection: [] }` defaults); ~5-8 tests.
- `server/src/test/java/com/gumtree/csagent/service/runtime/skill/SkillRegistryTest.java` — unit tests for `select(phase, useCase)` semantics (exact-match per phase + wildcard `["*"]` + null fallback / Optional.empty); ~6-8 tests.
- `server/src/test/java/com/gumtree/csagent/service/runtime/skill/SkillLoaderTest.java` — unit tests for SkillLoader (loads 4 Skills from `skills/`; fail-fast on schema error: missing field / invalid `applicable_phases` enum / invalid `applicable_use_cases` enum / invalid `valid_terminal_outcomes` enum / malformed YAML); ~10-12 tests. Use temp directories OR test resources for negative cases.
- `server/src/test/java/com/gumtree/csagent/service/runtime/PhaseEvaluatorSkillIntegrationTest.java` — behavioural-equivalence integration test per design doc §6.4. For each of the 4 migrated phases (DISCOVER + CONFIRM + ESCALATE + CLOSE), assert that the post-migration PhasePlan is field-by-field equal to a golden pre-migration PhasePlan for representative UCs. ~12-16 tests (3-4 UCs × 4 phases).

For the behavioural-equivalence test, the **golden pre-migration PhasePlan strings** must be captured BEFORE the legacy branches are deleted. Two approaches:

- **(A) Hardcoded golden strings** — extract the expected systemInstruction / groundingInstruction / escalationPolicy / etc. strings from the legacy branches at session start (read PhaseEvaluator.java lines 397-457 + 462-487 + 492-509 + 513-542) + inline them as String constants in the test file. This is fragile but explicit.
- **(B) Capture-via-checkout** — `git show 51c327c:server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java > /tmp/PhaseEvaluator-pre.java` + read the pre-migration content from the temp file at test time. More robust but adds test setup complexity.

Recommend (A) for simplicity; the golden strings are immutable post-deletion + the test is the contract.

---

## 3. Walking §4.1 nine-question anti-hardcode kernel on the Sprint 38 diff

Per Sprint 38 contract §8 + handoff §8: walk the §4.1 9 questions against the Sprint 38 diff. Expected verdict `approve` — architectural refactoring; behavioural equivalence preserved; tool-whitelist semantics unchanged. Key questions to walk explicitly:

- **Q1 keyword / regex / if-else / enum / per-UC matrix for semantic decision?** NO. SkillRegistry `select(phase, useCase)` is a lookup, NOT a branch table. The 4 simpler phase Skills all have `applicable_use_cases: ["*"]` so no per-UC routing. The DISCOVER Skill's `procedure` text at design doc §6.2.1 includes the Sprint 7 §I0 weak-candidate cue + Sprint 33 ad-status disambiguation cue verbatim from `PhaseEvaluator.java:418-431` + `:432-448`; these are CLASSIFICATION GUIDANCE (naming UCs as targets of inference like UC-F / UC-B / UC-C / UC-D), NOT branching behaviour by active UC. Codex M1 / NEW Sprint 37 reviewed this same content and verified NOT a per-UC-branch if-else.
- **Q2 Tier-0 justification?** N/A. Sprint 38 adds no Tier-0 invariant; Sprint 37 close pre-decision DEFER C2 + C3 preserved.
- **Q3 soft signal achievable?** N/A. Sprint 38 is architectural refactoring; the migrated content was already in PhaseEvaluator.java — the Skill abstraction just moves the source. The state_inheritance block carries `soft_signal_via_projection: [alternate_candidate_use_cases, discover_disambiguation_signals]` for DISCOVER Skill (per design doc §6.2.1) — these are existing M1 Sprint 32/33 projection slots; Sprint 38 declares them at the Skill level for Sprint 41's eventual state-bus enforcement (Sprint 38 itself does not yet enforce).
- **Q4 visible-eval / trace phrasing / CaseSpec id encoded into runtime/prompt/judge?** NO. The 4 Skill YAML bodies are migrated content from PhaseEvaluator.java + existing teaching text; no CaseSpec id, no trace phrasing.
- **Q5 semantic ownership shift LLM → Java?** NO. The LLM still owns UC hypothesis, drift, escalation posture, response strategy, customer-facing language, per-step argument choice per §1.3. The Skill `procedure` is LLM-soft teaching; the Skill `tools_required` composes the same `PhasePlan.allowedTools` as before (tool whitelist enforced by `ToolDispatcher.validateAgainstPlan`); the Skill `guardrails: []` for Sprint 38's 4 Skills means no Java predicate added.
- **Q6 if-else in prompt?** NO. The 4 Skill `procedure` texts are principle-level (CONFIRM is interpret-satisfaction; ESCALATE is finalize-handover; CLOSE is polite-closing-+-record-outcome; DISCOVER carries the Sprint 7 §I0 + Sprint 33 cues which Codex M1/Sprint 37 verified are classification guidance, not per-UC-pair branches).
- **Q7 tool / capability / PII / grounding floor preserved?** YES. Tool schemas unchanged; capability/permission boundary unchanged via existing `PhasePlan.allowedTools` + `ToolDispatcher.validateAgainstPlan`; PII / safety floor / grounding floor unchanged (the grounding-floor narrow extension via S1 `must_cite_source` predicate is Sprint 39 scope, not Sprint 38).
- **Q8 generalization coverage?** YES per Sprint 38 §8 stanza: target = 4 simpler phase Skills behavioural equivalence on representative UCs; neighbor = 2 unmigrated RESOLVE phases stay green (Sprint71PartialIntakePersistenceTest 14/14 + IntakeFieldExtractor + Sprint 32/33 projection-slot tests); negative = SkillLoader fail-fast on malformed YAML / missing field / invalid enum; shadow = N/A for Sprint 38 (behavioural equivalence on Java composition is fully testable; LLM behaviour observation OBSERVATION ONLY per M2 §5).
- **Q9 rollback / sunset?** N/A. Skill Registry abstraction is permanent. If Sprint 38 evidence shows the abstraction is wrong, deliver-agent + human STOP per Sprint 38 contract §10 #2 and re-frame; the legacy branches can be temporarily restored via git revert if needed.

---

## 4. Hard fences reminder (from Sprint 38 contract §6)

31 hard fences in Sprint 38 §6. Key reminders:

- §6 #1 **No edit to PhaseEvaluator.java RESOLVE-INTAKE (552-596) or RESOLVE-FAQ (598-665) branches.** Sprint 39 scope.
- §6 #2 **No edit to AgentRunLoopImpl.java.** Sprint 6/7/11 predicates PRESERVED. Sprint 39 migrates.
- §6 #3 **No edit to system_prompt.txt.** Sprint 40 extracts teaching paragraphs.
- §6 #4 **No NEW S1 / S2 predicates.** Sprint 39 scope.
- §6 #5 **No unified Skill terminal-predicate dispatcher.** Sprint 39 scope (depends on Skill guardrails populated in Sprint 39).
- §6 #6 **No SkillStateBus.java / prior_use_case_carry projection slot / ContextProjectionBuilder edit.** Sprint 41 scope.
- §6 #7-#10 **No touch to classifier / drift detector / INTAKE_UCS / escalation_reason enum / Tier-0 invariant.** M2 §6 fences.
- §6 #11-#15 **No constitution / governance / foundational doc / sprint archive / milestone archive edit.**
- §6 #16-#18 **No deliver-agent-owned file edit** (sprint_objective, milestone_objective, 10-handoff, action_bank, codex-findings).
- §6 #19-#23 **No eval surface edit** (case families, shadow, harness, Alice bad case, overrides).
- §6 #24 **No mocked-LLM as primary evidence** for LLM-behaviour claims.
- §6 #25 **No per-UC-branch if-else in any Skill YAML body** — Codex verifies this is load-bearing.
- §6 #26 **No Skill that prescribes LLM customer-facing language** per §1.3.
- §6 #27 **No Skill that hard-encodes per-step argument values** per §1.3.
- §6 #28-#29 **No regression on Sprint71PartialIntakePersistenceTest 14/14 / M1 functional surfaces.**
- §6 #30 **No CLOSE → TERMINAL phase enum rename** (OQ-7.1 default: keep enum unchanged).
- §6 #31 **No pre-decision of Sprint 39 / 40 / 41 implementation specifics beyond freeze.**

---

## 5. Stop conditions (per Sprint 38 contract §10)

20 stop conditions. Key triggers:

- §10 #1 premise drift on §4 items → STOP + surface.
- §10 #2 behavioural equivalence test fails on any of 4 migrated phases → STOP + surface (do NOT widen the equivalence assertion).
- §10 #3 Sprint 6/7/11 predicate test regression (those tests should NOT touch since predicates UNCHANGED) → STOP + surface (likely accidental touch).
- §10 #4 Sprint71PartialIntakePersistenceTest 14/14 fails → STOP + diagnose (M1 functional-surface regression).
- §10 #5 §1.7 forbidden-list violation cannot be cleanly resolved in a Skill YAML body → STOP + surface.
- §10 #6 §1.3 boundary violation (Skill prescribes customer language or per-step args) → STOP + surface.
- §10 #7 §1.4 boundary mismatch → STOP + surface.
- §10 #8-#14 tempted to migrate RESOLVE / Sprint 6/7/11 predicates / S1+S2 / dispatcher / state-bus / system_prompt.txt / rename CLOSE enum → STOP per fence reminders above.
- §10 #15 Tier-0 candidate surfaced from Sprint 38 implementation → STOP + surface in handoff §7 OQ (do NOT edit `runtime_freeze_and_risk_policy.md` in Sprint 38 dev commit).
- §10 #16-#17 tempted to edit constitution / Sprint 37 freeze → STOP per constitution-discipline.
- §10 #18 tempted to draft Sprint 39 / 40 / 41 contracts → STOP per deliver-agent-owned at planning round.
- §10 #19 tempted to modify Alice bad case → STOP per Constitution §1.7.
- §10 #20 tempted to use mocked-LLM as primary evidence → STOP per `feedback_mocked_llm_cannot_prove_prompt_causal_change.md`.

---

## 6. Bundle policy

- **Single dev commit** with all §5 Sprint 38 contract files: 3 new Java classes (Skill, SkillRegistry, SkillLoader); 4 new Skill YAML files; PhaseEvaluator.java edit; 4 new test files (~30-40 tests total); `docs/sprints/sprint-038-handoff.md`.
- Per `feedback_commit_at_end_bundles_deliver_artefacts.md`: dev does NOT stage deliver-agent-owned files (`docs/sprint_objective.md` / `docs/milestone_objective.md` / `docs/10-handoff.md` / `docs/action_bank.md` / `docs/codex-findings.md` / `compact/sprint-038-*.md`). Deliver-agent + human bundle those at sub-sprint close.
- **Tests are part of the dev commit**, NOT a separate fix-iteration.

Commit message format (per recent precedent like `51c327c` Sprint 37 commit):

```
sprint 38: SkillRegistry core + 4 simpler phase Skills migration (NEW M2 sub-sprint 2)
```

(With or without a longer body; deliver-agent doesn't require a specific body shape.)

---

## 7. Java baseline note

Post-Sprint-37-close baseline: `983 / 1-inherited / 0 / 2` per `mvn test -q`. The 1 inherited failure is `SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly:53` (dirty working-tree `system_prompt.txt` baseline carrying from Sprint 24-era; documented unchanged through Sprint 37 close).

Sprint 38 expected baseline post-commit: `983 + ~30-40 new Sprint 38 tests` = `~1013-1023` total tests; 0 new failures; 1 inherited failure persists; 0 new errors; 2 skipped. Re-run `cd server && mvn test -q` BEFORE committing; verify counts.

The inherited `SystemPromptUserRequestedTiebreakerTest` failure stays exactly as it is at HEAD `51c327c`. Sprint 40 may resolve it when extracting Sprint 23/31/33 teaching paragraphs (the test was breaking because of the dirty `system_prompt.txt` working-tree mod from Sprint 24-era); Sprint 38 does NOT touch `system_prompt.txt` so the failure carries forward unchanged.

---

## 8. Handoff document structure (12 sections per Sprint 31-37 shape; per Sprint 38 contract §11)

Author `docs/sprints/sprint-038-handoff.md` with 12 sections matching Sprint 31-37 shape, adapted for implementation sub-sprint:

1. **Context Pack** — list of files read at session start with tier / status / one-line relevance; code shape verified at HEAD `51c327c` table with cited line numbers; doc-status warnings if any; source-of-truth decision; implementation status `implemented`; risks before implementation (assessed at session start).
2. **Sub-sprint-objective recap** — Sprint 38 SkillRegistry core + 4 simpler phase Skills migration; layer prompt_projection + skill_state + Runtime PhasePlan composition; semantic-touching multi-layer.
3. **Premise re-verification** — §4 spot-check; 12 premises; one short paragraph per premise citing source.
4. **Implementation walkthrough** — high-level summary of 3 Java classes + PhaseEvaluator integration + 4 Skill YAMLs; cross-reference to Sprint 37 freeze decisions (a)/(b)/(c)/(e §6.2.1-§6.2.4).
5. **Sprint 37 freeze fidelity** — for each freeze decision, cite the implementation file + line range that honors the decision; note any departures from freeze template with rationale.
6. **§4.1 anti-hardcode self-walk** — 9 questions on Sprint 38 diff; expected verdict `approve`.
7. **Open questions for deliver-agent + human** — any §1.7 boundary case requiring deliver-agent + human + Codex review; any premise drift; any tension between freeze template and actual implementation (e.g., SkillRegistry.select null-fallback semantics chosen per design doc §3.2 alternatives — dev picks + surfaces); any decision that surfaced unexpected tension.
8. **Anti-hardcode self-walk (§4.1 nine questions) on Sprint 38 itself** — expected verdict `approve`.
9. **Files changed** — table with paths; 3 new Java classes + 4 new Skill YAML files + 1 PhaseEvaluator edit + 4 new test files + handoff = ~13 files.
10. **Layer-classification self-walk** per Sprint 38 §8 stanza — prompt_projection + skill_state + Runtime PhasePlan composition; behavioural equivalence preserved; tool-whitelist semantics unchanged.
11. **§5 Eval Acceptance bars (adapted for architectural-refactoring sub-sprint + M2 §5 recalibration)** — Sprint 38 hard gates: 3 new Java classes compile + pass tests; 4 Skill YAMLs load + validate at boot; PhaseEvaluator integration; behavioural equivalence on 4 phases; Java baseline preservation 983 + ~30-40 new tests; Sprint71PartialIntakePersistenceTest 14/14; M1 functional-surface preservation; Codex per-sub-sprint review verdict `approve` expected. Interactive eval smoke + bad-case suite are OBSERVATION ONLY per M2 §5 recalibration; record at close for tracking, MAY regress, does NOT block.
12. **Closure verdict placeholder** — leave for deliver-agent + human + Codex per-sub-sprint review per `feedback_handoff_verdict_section_delegation.md`.

---

## 9. M2 milestone context recap (quick reference)

| Sub-sprint | Status | Scope |
|---|---|---|
| Sprint 37 | **CLOSED PASS A 2026-05-17 (commit `51c327c`)** | Skill Registry design freeze (3633-line design doc; 10 decisions a-j; 5 Tier-0 candidates; 11 OQs) |
| **Sprint 38** | **Current sub-sprint** | SkillRegistry core + 4 simpler phase Skills migration (DISCOVER + CONFIRM + ESCALATE + TERMINAL/CLOSE) |
| Sprint 39 | Future | RESOLVE_FAQ + RESOLVE_INTAKE Skill migration + Sprint 6/7/11 predicate migration + NEW S1 `must_cite_source` + S2 `intake_complete_required` + unified Skill terminal-predicate dispatcher |
| Sprint 40 | Future | Sprint 23/31/33 teaching paragraph extraction from `system_prompt.txt` + orchestration shell cleanup |
| Sprint 41 | Future | UC switch + state preservation (`SkillStateBus.java` + per-Skill `state_inheritance` enforcement + NEW `prior_use_case_carry` projection slot) |

M2 acceptance bar recalibration 2026-05-17: bad-case suite (Alice) + interactive eval composite_score are OBSERVATION ONLY for THIS milestone (NOT hard gate). Primary gate = functional review + Java tests + Sprint 37 freeze decisions honored across implementation sub-sprints + per-sub-sprint Codex review verdicts `approve`.

Sprint 38 success → Sprint 39 contract draft (deliver-agent + human planning round). Sprint 38 fix-iteration if Codex returns `fix_required / blocking_count ≥ 1`. Sprint 38 `out_of_scope_review` if Codex broadens scope beyond Sprint 38 contract.

---

## 10. Self-check before committing

- [ ] All §1 read order completed; cited in handoff §1 Context Pack.
- [ ] All §2 deliverables D-a / D-b / D-c / D-e implemented per Sprint 37 freeze.
- [ ] §3 §4.1 9-question self-walk completed; verdict `approve`.
- [ ] §4 31 hard fences honored (no scope creep into Sprint 39/40/41 surfaces).
- [ ] §5 20 stop conditions evaluated; none fired.
- [ ] §6 bundle policy honored; deliver-agent-owned files NOT staged.
- [ ] §7 Java baseline preserved (`mvn test -q`: 983 + ~30-40 new tests = ~1013-1023; 0 new failures; 1 inherited failure persists; 0 new errors).
- [ ] §8 handoff structure 12 sections complete; §12 closure verdict left as PLACEHOLDER.
- [ ] No edit to deliver-agent-owned files (`docs/sprint_objective.md` / `docs/milestone_objective.md` / `docs/10-handoff.md` / `docs/action_bank.md` / `docs/codex-findings.md` / `compact/sprint-038-*.md`).
- [ ] No edit to `docs/proposals/skill_registry_design.md` (Sprint 37 freeze immutable).
- [ ] No edit to sprint archives or milestone archives.
- [ ] No edit to RESOLVE_FAQ / RESOLVE_INTAKE branches of PhaseEvaluator.java.
- [ ] No edit to AgentRunLoopImpl.java.
- [ ] No edit to system_prompt.txt.
- [ ] No edit to ContextProjectionBuilder.java.
- [ ] No edit to RuntimeIntentClassifier.java / DriftDetector.java / UseCaseRouter.java / ClassifyUseCaseTool.java.
- [ ] No NEW Tier-0 invariant in runtime_freeze_and_risk_policy.md.
- [ ] No per-UC-branch if-else in any Skill YAML body.
- [ ] No CLOSE → TERMINAL phase enum rename.
- [ ] Commit message follows the format `sprint 38: SkillRegistry core + 4 simpler phase Skills migration (NEW M2 sub-sprint 2)`.

Once committed, surface the commit SHA + a brief summary (~3-5 sentences on what shipped + any §7 OQs surfaced) to the human. The deliver-agent will draft the Sprint 38 Codex review prompt + dispatch Codex per-sub-sprint review at Sprint 38 close.
