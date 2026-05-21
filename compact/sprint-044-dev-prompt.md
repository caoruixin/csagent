Paste the content below this line into a fresh Claude Code session after the human commits the deliver-agent's S-Eval-2 close + S-Eval-3 launch bundle. The S-Eval-3 sub-sprint contract is `docs/sprint_objective.md` at HEAD.

---

You are Claude Code working as the **dev agent** for **Sprint 44 — Populate `critical_steps` for the 6 Skills** (S-Eval-3; the THIRD sub-sprint of NEW Milestone M3-Eval: Coarse-to-Fine Evaluation Architecture, per `docs/milestone_objective.md`).

S-Eval-2 (Sprint 43) closed Clean PASS 2026-05-21 (dev commit `69ed77f`; archive at `docs/sprints/sprint-043-objective.md` + `docs/sprints/sprint-043-handoff.md`). S-Eval-2 landed: NEW `Skill.criticalSteps` field + NEW `CriticalStep` Java record + SkillLoader allowlist validation + ContextProjectionBuilder projection wiring (`phase_plan.critical_steps` as `[{id, desc}]` list-of-objects; empty-array parity preserved) + NEW Python `SkillProcedureExtractor` with 6-primitive recursive-descent DSL parser + composite Tier-2 wiring via the S-Eval-1 `severity` field convention. **§1.7 structural defence over-delivered**: 15 hardcode-flavoured `trace_check` strings rejected at parse time (well above the contract §9 ≥6 floor; 2.5× over). **S-Eval-3 builds on this contract by populating the YAML side; ships NO Java / Python code beyond optional minor fixes**.

**Sprint 44 (S-Eval-3) IS the critical governance gate of M3-Eval.** Per `iteration_governance.md` §4.3 trigger #2 (LLM-visible content; §1.7 forbidden-list adjacent), **this sub-sprint requires PER-SUB-SPRINT Codex review** at close. S-Eval-4 BLOCKED until Codex returns `decision: pass` (or `approve with downgrade-to-signal follow-up`). Codex independently verifies each populated `critical_steps[].desc` against the proposal §5.3 standard table (reproduced verbatim in §6 below). The S-Eval-2 DSL parser is the **structural defence** (rejects keyword/regex/message-content at parse time); the proposal §5.3 standard is the **semantic defence** on `desc` wording. Both layers must hold.

The scope is locked at `docs/sprint_objective.md` §2 (18-30 critical_steps across 6 Skills; 4 coverage clusters; per-Skill target distribution table) + §5 (Files in scope: 6 Skill YAMLs only) + §6 (Files NOT in scope). All 12 sections of the sub-sprint contract are binding.

Sprint 44 explicitly DOES NOT:

- Modify any existing Skill `procedure` text or any other M2-landed Skill field (`guardrails`, `state_inheritance`, `applicable_use_cases`, `system_instruction`) on any of the 6 YAMLs. ONLY `critical_steps:` blocks are added.
- Create new Skill YAML files. The 6 existing Skills are the only authoring surface.
- Modify `Skill.java` / `CriticalStep.java` / `SkillLoader.java` / `SkillRegistry.java` / `SkillStateBus.java` / `SkillGuardrailDispatcher.java` / `ContextProjectionBuilder.java`. S-Eval-2 just shipped these; S-Eval-3 only authors YAML content.
- Extend the `trace_check` DSL beyond the 6 frozen primitives. If any step appears to need a new primitive, STOP and surface per §4 #2 (DSL extension is a deliver-agent + human decision, not a silent author-side extension).
- Touch `skill_procedure_check.py` or `composite.py` beyond minor authorized fixes (under §4 surface-and-extend discipline).
- Touch runtime semantic surfaces (RuntimeIntentClassifier, IntentClassification, DriftResult, DriftDetector, UseCaseRouter, ClassifyUseCaseTool, PhaseEvaluator semantic dispatch, AgentRunLoopImpl, ControlKernel, system_prompt.txt, tool-policy.yaml, IntakeFieldsRegistry, UseCaseRegistryService, ResolveDispositionEvaluator).
- Touch any case fixture (smoke 14, anchor 159, anchor_outcome 12, case_families 12, Alice bad case 1, shadow, case_spec_overrides.yaml). The bad-case + anchor_outcome run is read-only verification.
- Touch the L3 judge / rubric (`llm_judge.py` is S-Eval-5 territory).
- Touch the eval harness / loader / simulator.
- Touch governance docs (`iteration_governance.md`, `doc_governance.md`, `agent_context_guide.md`, `runtime_freeze_and_risk_policy.md`, `customer_service_tool_spec_v0_3.md`), foundational docs, or sprint / milestone archives (including the just-archived S-Eval-2 contract).
- Touch `docs/codex-findings.md` (review-agent territory; Codex per-sub-sprint review will WRITE here at S-Eval-3 close).
- Touch `docs/milestone_objective.md` (deliver-agent territory).
- Add Tier-0 invariants (milestone-level §6 hard fence).
- **Add any if-else / regex / keyword matrix / per-UC enumeration anywhere in `desc` OR `trace_check`. §1.7 red line.** Self-walk against the proposal §5.3 standard table (see §6 below) BEFORE every commit.
- Address S-Eval-2 OQ-S43.1 / OQ-S43.4 (routed to M3-Eval-shared Codex review per S-Eval-2 close §12.4).
- Use mocked-LLM as primary evidence for any LLM-behaviour claim.

## 1. Read order on cold start

1. **`AGENTS.md`** (transitively loads `docs/current/doc_governance.md` + `docs/current/agent_context_guide.md` + `docs/current/iteration_governance.md` §1 / §3 / §4 / §5 / §5.5 / §5.6 / §7 / §8).
2. **`docs/milestone_objective.md`** — NEW M3-Eval north star. ESPECIALLY:
   - §1 Milestone class (S-Eval-3 row: **per-sub-sprint Codex per §4.3 trigger #2**).
   - §3 Sub-sprint sequence → S-Eval-3 scope detail.
   - §6 Hard fences (15 milestone-level items; S-Eval-3 §6 inherits).
   - §8 + §8.1 Codex review plan (S-Eval-3 fires per-sub-sprint review).
3. **`docs/sprint_objective.md`** — Sprint 44 / S-Eval-3 contract. ALL 12 sections binding. §4 premise check already verified by deliver-agent 2026-05-21; you may re-verify but DO NOT skip if §4 line counts or field locations have drifted.
4. **`docs/solutions/m3_eval_milestone_proposal.md`** — proposal source. **LOAD-BEARING sections**: §5 (decision-5 deep-dive — explains WHY `critical_steps[].desc` is LLM-visible; the §1.7 anti-hardcode red line) + **§5.3 (ANTI-HARDCODE STANDARD TABLE — the semantic defence; REPRODUCED IN §6 BELOW + sprint_objective §8 stanza)** + §5.4 (token cost estimate 500-900 tokens; milestone §5 acceptance bar ≤ 1000) + §6 S-Eval-3 (scope detail including 4 coverage clusters).
5. **`docs/sprints/sprint-043-handoff.md`** — S-Eval-2 dev archive. LOAD-BEARING sections: §3 (S-Eval-2 file shipments including `skill_procedure_check.py` DSL parser at lines per dev §3 table — the structural defence S-Eval-3 builds on) + §4 (schema details — `CriticalStep` record shape that S-Eval-3 populates) + §5 (projection wiring details — empty-array parity; how populated `critical_steps[]` will render) + §12.7 (carry-over: thin-corpus UCs; OQ-S43.2 `id` cross-reference availability; OQ-S43.5 flag fallback monitoring) + §12 closure verdict.
6. **`docs/sprints/sprint-042-handoff.md`** §12.7 — the per-UC anchor distribution observation (origin of thin-corpus UC awareness: UC-G/H/I/J = 0 anchor; UC-F = 1; UC-B = 5).
7. **`docs/current/iteration_governance.md`** specifically:
   - §1.3 LLM-owned (S-Eval-3 augments LLM's projection with `critical_steps[].desc` rendering — soft procedural narrative the LLM owns acting on; PRESERVES §1.3 ownership; does NOT shift decisions to Java).
   - §1.4 Runtime-owned (S-Eval-3 adds NO new Runtime-floor enforcement).
   - **§1.7 Forbidden — VERY LOAD-BEARING for `desc` and `trace_check` content authoring.** Re-read carefully BEFORE authoring any step.
   - §3.2 Q6 (`eval_spec` layer classification).
   - **§4.1 nine-question kernel** (you self-walk against the S-Eval-3 diff at handoff §6; **Q1 must explicitly enumerate the two layers of defence: S-Eval-2 DSL parser + §5.3 semantic self-walk**).
   - §4.2 sprint-close header convention (Codex writes this at S-Eval-3 close).
   - **§4.3 trigger #2 (LLM-visible content + §1.7 adjacent) — this fires for S-Eval-3.** Codex per-sub-sprint review is mandatory.
   - §5 / §5.5 / §5.6 acceptance bars.
   - §7 sprint-objective stanza (the contract §8 stanza is already filled with the §5.3 standard table reproduced verbatim).
8. **`docs/runtime_freeze_and_risk_policy.md`** §1 + §2 — verify NO Tier-0 invariant added at S-Eval-3.
9. **Code source files to spot-check at HEAD** (read on demand during §3 premise re-verification + §4 implementation, NOT end-to-end):
   - 6 Skill YAMLs under `server/src/main/resources/skills/`: locate `procedure` content for each + `applicable_use_cases` (UC scope for `mandatory_for` validation). None currently carry `critical_steps:` blocks.
   - `server/src/main/java/com/gumtree/csagent/service/runtime/skill/CriticalStep.java` (89 lines per S-Eval-2 dev §3): the record shape you populate.
   - `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillLoader.java` (S-Eval-2 extended with `VALID_CRITICAL_STEP_SEVERITIES` allowlist + per-step validation): consult to understand what SkillLoader will reject so authoring matches.
   - `eval_interactive/eval_interactive/scoring/skill_procedure_check.py` (771 lines per S-Eval-2 dev §3): the DSL parser + extractor — locate `parse_trace_check` to understand the 6 frozen primitives' syntax precisely.
   - `eval_interactive/tests/test_skill_procedure_dsl_parser_rejects_hardcodes.py` (152 lines; 15 hardcode-flavoured rejections): use as a NEGATIVE-EXAMPLE library when authoring `trace_check` strings (if any string you author resembles a string in this file, that's a STOP signal per §4 #3).
   - `eval_interactive/case_specs/bad_cases/alice_uc_a_uc_h_misclass.yaml` + `_manifest.md`: Alice's `closure_criterion` is the verification target for the UC-H intake-then-handover cluster (an `intake_complete_required` step should surface as a Tier-2 fail on Alice's trace).
   - 12 anchor_outcome cases under `eval_interactive/case_specs/anchor_outcome/`: per-UC outcome targets for Tier-2 calibration verification.

## 2. Goal (18-30 critical_steps across 6 Skills)

Per the sprint_objective §2 per-Skill anticipated distribution table:

| Skill | Anticipated count | Primary clusters |
|---|---|---|
| `discover_triage.yaml` | 3-4 | DISCOVER disambiguation + UC-FP route hint |
| `confirm.yaml` | 2-3 | DISCOVER disambiguation tail (confirmation behaviour) |
| `resolve_faq_grounded_answer.yaml` | 4-5 | UC-A/B/C/D/E/F FAQ-grounded + UC-FP explain-when-actionable |
| `resolve_intake_collect_and_handover.yaml` | 4-5 | UC-G/H/I/J/K intake-then-handover + UC-FP intake-on-appeal |
| `escalate.yaml` | 2-3 | Escalation reason present; intake fields handed off |
| `terminal.yaml` | 1-2 | No new tools after terminal; final user message present |
| **TOTAL** | **16-22** | (within 18-30 envelope; lower bound) |

Each step carries 5 fields:

- `id`: short kebab-case identifier. **Cross-referenceable per OQ-S43.2 (S-Eval-2 left this optional)**; use if natural in a peer step's `desc` (e.g., "after the `search-before-answer` step"), leave unused otherwise. Document choice in handoff §4.
- `desc`: **soft procedural narrative** per §6 below (§5.3 standard table).
- `trace_check`: DSL expression using ONLY the 6 frozen primitives from S-Eval-2.
- `mandatory_for`: list of canonical UC ids (validated against `UseCaseRegistryService` per the existing `applicable_use_cases` precedent).
- `severity`: `mandatory` (gate-contributing on `mandatory_for` UCs) OR `advisory` (Tier-3 observation only).

**Verification step (CRITICAL for Codex review)**: after population, run the bad-case (Alice) + anchor_outcome (12) + smoke (14) + sampled subset of anchor (≥ 20 cases) end-to-end via the S-Eval-2 extractor + composite scorer. **Alice's UC-H intake-then-handover failure shape should surface as a Tier-2 `intake_complete_required` fail** (or whatever equivalent step id you assign in `resolve_intake_collect_and_handover.yaml`). Record per-Tier-2-fail observation in handoff §6 (mapped to `_manifest.md` known failure modes where applicable). The mapping is what Codex will independently verify.

## 3. Read order specific to deliverables

- For each Skill YAML: read the existing YAML in full to understand the `procedure` + `guardrails` + `state_inheritance` context (your `critical_steps` populate the same scope). The 4 coverage clusters in §2 map to specific Skills — pick steps consistent with the Skill's existing `procedure`.
- For each `trace_check` string: scan `skill_procedure_check.py` `parse_trace_check` source for the 6 frozen primitive syntax + `_HARDCODE_BLOCKLIST` for the illustrative rejected primitives. Cross-check your authored string mentally against the 15 hardcode-flavoured rejections in `test_skill_procedure_dsl_parser_rejects_hardcodes.py`.
- For Alice verification: read `eval_interactive/case_specs/bad_cases/alice_uc_a_uc_h_misclass.yaml` `closure_criterion` to understand the expected end-state; your UC-H intake step should make Tier-2 surface the failure shape.
- For per-UC coverage: cross-reference `_manifest.md` known failure modes when authoring `mandatory_for` lists (under-represented UCs UC-G/H/I/J carry no anchor exercise; lean on Alice + anchor_outcome).

## 4. STOP discipline (9 LOAD-BEARING per contract §10)

You STOP and surface to deliver-agent + human (instead of pressing on) if any of:

1. **A needed `desc` cannot be expressed without resorting to keyword / regex / message-content matching** to make Codex review pass §5.3 standard. Surface; deliver-agent + human decide whether to rephrase the step OR drop it.
2. **A needed `trace_check` cannot be expressed in the 6 frozen DSL primitives** (e.g., a step like "moderation context loaded" requires a primitive not in S-Eval-2's frozen set). Do NOT extend the DSL silently; surface to deliver-agent + human who decide DSL extension (must go through S-Eval-2 author + Codex review boundary) OR step rephrasing OR step drop.
3. **A `trace_check` parses successfully but the §1.7 structural defence test would flag it as a hardcode-shape** (mental run of `test_skill_procedure_dsl_parser_rejects_hardcodes.py` against your authored `trace_check` — if any of your strings resemble a rejected pattern, treat as a STOP signal even if parser accepts it).
4. **Codex per-sub-sprint review returns `reject as semantic hardcode`** on first pass. Dev does NOT silently iterate; surface to deliver-agent + human who decide fix-iteration scope.
5. **Alice bad case Tier-2 fail does NOT surface** the `intake_complete_required` step OR equivalent. May indicate the populated step is mis-scoped or the Tier-2 wiring has an edge case. Surface; do NOT massage step content to force the expected fail.
6. **Backward-compat break** on any of the 6 Skill YAMLs (SkillLoader rejects the populated YAML; Spring bootstrap fails; existing prompt-composition golden tests regress). Halt and surface.
7. **Java or Python baseline regresses** (any new test failure introduced by S-Eval-3 beyond the inherited `SystemPromptUserRequestedTiebreakerTest:53` and the 9 pre-existing Python failures). Halt and surface; do NOT commit.
8. **Any file under "Files NOT in scope" §6** needs touching to complete S-Eval-3. Hard fence violation; halt and surface; do NOT smuggle scope.
9. **Per-turn projection token-cost exceeds 1000 tokens** (milestone §5 acceptance bar). Surface; deliver-agent + human decide whether to trim `desc` strings OR accept as a milestone observation.

## 5. Hard fences (per contract §6; 5 categories)

1. **M2-landed + S-Eval-2-landed Skill semantics — NO touch** (`Skill.java`, `CriticalStep.java`, `SkillRegistry.java`, `SkillLoader.java`, `SkillStateBus.java`, `SkillGuardrailDispatcher.java`, `StateInheritance.java`, `Guardrail.java`, `ContextProjectionBuilder.java`, all 6 Skill YAMLs' `procedure` / `guardrails` / `state_inheritance` / `applicable_use_cases` / `system_instruction`).
2. **Runtime semantic surfaces — NO touch (M2 §6 #5 + S-Eval-1/2 inherits)** (RuntimeIntentClassifier, IntentClassification, DriftResult, DriftDetector, UseCaseRouter, ClassifyUseCaseTool, PhaseEvaluator, AgentRunLoopImpl, ControlKernel, system_prompt.txt, tool-policy.yaml, IntakeFieldsRegistry, UseCaseRegistryService, ResolveDispositionEvaluator).
3. **Eval case fixtures + harness — NO touch (cascade fence)** (smoke 14 / anchor 159 / anchor_outcome 12 / case_families 12 / Alice 1 / shadow / case_spec_overrides.yaml; `loader/`, `simulator/`, `case_spec/schema.py` (S-Eval-1 landed), `case_spec/loader.py`, `scoring/hard_checks.py`, `scoring/outcome_checks.py`, `scoring/llm_judge.py`).
4. **Governance and archives — NO touch** (`iteration_governance.md`, `doc_governance.md`, `agent_context_guide.md`, `runtime_freeze_and_risk_policy.md`, `customer_service_tool_spec_v0_3.md`, `docs/foundational/`, all sprint / milestone archives, live `milestone_objective.md`, live `codex-findings.md`).
5. **Action bank — limited touch** (do not close R-items in S-Eval-3; you MAY append a Sprint 44 close-action index row at sub-sprint close; the new `R-stale-test-escalation-enum-sync-v0_2-to-v0_3-migration` (S-Eval-1 close) is unrelated to S-Eval-3 and SHOULD NOT be addressed here).

## 6. §5.3 anti-hardcode standard (CRITICAL — self-walk every `desc` against this table BEFORE commit)

Reproduced verbatim from the proposal §5.3 (also in `docs/sprint_objective.md` §8 stanza):

| Form of `desc` | Example | Verdict |
|---|---|---|
| Soft procedural narrative | "For UC-FP issues, consult the moderation context before explaining the reason" | ✅ LLM-first; procedural guidance the LLM owns acting on |
| Soft diagnostic signal | "If the user mentions 'appeal' or 'review', consider whether this is actually UC-H rather than UC-FP" | ⚠️ Edge — Codex will scrutinise; acceptable as guidance, NOT as a rule |
| Hard if-else rule | "IF user.message.contains('appeal') THEN active_use_case := UC-H" | ❌ Violates §1.7; Codex rejects |
| Keyword enumeration matrix | "Trigger words for UC-H: ['appeal', 'review', 'wrongly', 'unfairly', ...]" | ❌ Violates §1.7; Codex rejects |

**Self-walk procedure** (mandatory; documented in handoff §5):

For each populated `desc`:
1. Identify the verdict row that best matches the `desc`'s phrasing.
2. If row 1 (✅): proceed.
3. If row 2 (⚠️): rephrase to row 1 if possible; if not, document as edge-case in handoff §5 with rationale for why row 1 phrasing is not feasible.
4. If row 3 or 4 (❌): REJECT — rephrase or drop the step; the populated state must contain only row-1 (and optionally row-2) phrasings.

The §4.1 self-walk Q1 at handoff §6 MUST explicitly cite this self-walk as the SEMANTIC defence (the S-Eval-2 DSL parser is the STRUCTURAL defence on `trace_check`; together they're the two layers).

## 7. §4.1 anti-hardcode self-walk template

Before commit, walk each Q1-Q9 against the S-Eval-3 diff. Write the verdicts in handoff §6:

- **Q1** Semantic hardcode introduced (keyword / regex / if-else / enum / per-UC matrix)? Expected: **pass** — Sprint 44 ships 18-30 populated `critical_steps[].desc` strings + matching `trace_check` strings. **Two layers of defence cited**: (a) the proposal §5.3 standard self-walk on every populated `desc` (per §5 of handoff; semantic defence); (b) the S-Eval-2 DSL parser `parse_trace_check` + `test_skill_procedure_dsl_parser_rejects_hardcodes.py` 15 hardcode-flavoured rejections (structural defence). Every `desc` is row-1 or row-2 (with rationale) per the §5.3 table; every `trace_check` is one of the 6 frozen primitives.
- **Q2** Tier-0 invariant claim? Expected: **pass** — no Tier-0 invariant added. The Tier-2 `skill_procedure_followship` gate (S-Eval-2) gains its first content but remains eval-side, not Tier-0.
- **Q3** Could a soft signal replace a hard branch? Expected: **pass** — the `desc` is the soft signal (LLM-visible procedural guidance the LLM owns acting on; not a hard runtime branch).
- **Q4** Eval phrase / trace-specific phrasing / CaseSpec-id encoded? Expected: **pass** — no CaseSpec id / trace-specific phrasing / visible-eval case text in any populated `desc` or `trace_check`. References are to canonical tool names (`get_moderation_review_context`, `create_case_controlled`, `request_handover`, `search_knowledge`, etc.) and canonical intake-field names — runtime surfaces, not eval-specific.
- **Q5** LLM ownership shrunk (§1.3 surfaces moved to Java)? Expected: **pass** — the LLM's projection GAINS the soft procedural narrative; the LLM still owns user goal, drift, UC hypothesis, next action, escalation posture, response strategy, customer-facing wording. The eval-side `trace_check` observes the LLM's behavioural trace; it does NOT make decisions.
- **Q6** Prompt if-else added? Expected: **pass** — every `desc` is principle-level guidance per §5.3 standard (no "IF user.message.contains(...)").
- **Q7** Tool schema / capability / PII / grounding floor preserved? Expected: **pass** — no tool schema change; no capability / permission change; no PII handling change; FAQ grounding contract unchanged. Tier-0 safety floor checks remain critical-severity.
- **Q8** Generalization coverage (target / neighbor / negative / shadow)? Expected: **pass** — per sprint_objective §8 stanza.
- **Q9** Rollback / sunset plan if temporary? Expected: **N/A** — S-Eval-3 changes are intended permanent (the populated `critical_steps` ARE the soft-signal form that replaces existing scattered teaching paragraphs + implicit tool whitelisting reliance per proposal §5.2).

If any Q1-Q9 returns `concern` or `fail`, STOP and surface to deliver-agent + human before commit. **Codex per-sub-sprint review will independently re-walk Q1-Q9 + §1.7 boundary check at S-Eval-3 close**; any `concern` or `fail` on Codex's side triggers fix-iteration.

## 8. Handoff §11 12-section contract

`docs/sprints/sprint-044-handoff.md` must include:

- **§1** Sprint identity (Sprint 44 / S-Eval-3 / M3-Eval sub-sprint 3).
- **§2** Summary (one paragraph).
- **§3** Files shipped + per-file line-count numstat (reproducible via `git show --numstat <commit>`). Includes OQ-S43.2 `id` usage choice (did you cross-reference steps by `id`? in which Skills?) + OQ-S43.5 `session.<flag>_present` reliance observation (did any populated step use bare-flag truthiness?).
- **§4** Per-Skill `critical_steps` content map (which steps landed on which Skill; cluster mapping; mandatory_for UC coverage per step; rationale for severity choice per step).
- **§5** **§5.3 anti-hardcode self-walk per populated `desc`** (table format; per-step verdict against the 4-row §5.3 standard; **CRITICAL for Codex per-sub-sprint review**). Format suggestion:
  ```
  | step_id | desc snippet | verdict row | rationale |
  ```
- **§6** §4.1 anti-hardcode self-walk verdicts (Q1-Q9 with one-line justification each; **Q1 MUST explicitly enumerate §5.3 self-walk (semantic) + S-Eval-2 DSL parser (structural) as the two layers of defence**).
- **§7** Open questions (OQ-S44.N format; non-blocking decisions for deliver-agent + human at close; surface any DSL extension requests, any §5.3 row-2 edge-case rationales, any thin-corpus UC observations).
- **§8** Generalization coverage filled (target / neighbor / negative / shadow counts per §8 stanza).
- **§9** Validation runs:
  - Java test suite (`mvn test`): baseline vs pre-S-Eval-3 (1163); explicit count of S-Eval-3-added tests (expected 0-5).
  - Python test suite (`uv run pytest`): baseline vs pre-S-Eval-3 (392 passed / 9 pre-existing failed); explicit count of S-Eval-3-added tests (expected 0-15); confirm 9 pre-existing failures unchanged.
  - **Tier-2 verification run**: bad-case (Alice) + anchor_outcome (12) + smoke (14) + sampled anchor (≥ 20). Per-Tier-2-fail observation table mapped to `bad_cases/_manifest.md` known failure modes. **Alice UC-H expected fail surfaces** — record actual `step_id` and `severity`.
  - Per-cluster negative check: UC-A case does NOT trigger UC-FP steps; UC-FP case does NOT trigger UC-H intake steps.
  - Projection token-cost observation: per-turn projection byte / token delta vs S-Eval-2 baseline (0 bytes; empty `critical_steps[]` → populated `critical_steps[]`).
- **§10** Contract drift (any deviation from this contract; classify per §7-a / §7-b / §7-c / §7-d convention).
- **§11** Bundle policy honored (dev shipped one commit; deliver-agent close-out files NOT staged).
- **§12** Closure verdict (LEFT EMPTY by dev per `feedback_handoff_verdict_section_delegation.md`).

## 9. Bundle policy

Dev ships **ONE bundle commit** containing:

- 6 Skill YAML files with `critical_steps:` blocks appended.
- Optional minor fixes per §5 IF surfaced + deliver-agent + human authorized.
- Dev handoff `docs/sprints/sprint-044-handoff.md`.

Expected: 7-8 files in the bundle (much smaller than S-Eval-2's 12; content-only authoring).

Dev does NOT stage:

- `docs/sprint_objective.md` archive copy (deliver-agent territory at close).
- `docs/milestone_objective.md` updates (deliver-agent territory).
- `docs/10-handoff.md` §1 lead refresh (deliver-agent territory).
- `docs/codex-findings.md` — Codex per-sub-sprint review WRITES here at S-Eval-3 close; NOT dev's territory.
- `docs/action_bank.md` Sprint 44 close-action row (you MAY append at close; deliver-agent + human may also do this at close-out bundle).

Suggested commit message:

```
sprint 44 / S-Eval-3: populate critical_steps for the 6 Skills (NEW M3-Eval sub-sprint 3)
```

## 10. Self-check before commit

Run these mental checks against your diff before staging:

1. **Hard fence audit**: walk §5 of this prompt; for each "NO touch" item, run `git diff <path>` and confirm empty.
2. **Numbers-cite discipline**: every count in handoff §3 / §4 / §9 must be reproducible. Use `git show --numstat <staged-commit>` for per-file line counts; `mvn test 2>&1 | tail -5` for the Java baseline; `uv run pytest 2>&1 | tail -3` for the Python baseline; explicit step count per Skill via `grep -c "id:" server/src/main/resources/skills/<name>.yaml` (within the `critical_steps:` block).
3. **§5.3 self-walk PASSES on every populated `desc`**: walk table in handoff §5 has only row-1 (✅) and at most a few well-justified row-2 (⚠️) entries; ZERO row-3 (❌) or row-4 (❌) entries. **This is the milestone's primary semantic defence — Codex will independently re-walk every entry.**
4. **§4.1 self-walk in handoff §6**: Q1-Q9 verdicts written; Q1 explicitly cites §5.3 (semantic) + S-Eval-2 DSL parser (structural) as the two layers. No `concern` / `fail` on any Q.
5. **Every `trace_check` uses only the 6 frozen primitives**: mental cross-check against `test_skill_procedure_dsl_parser_rejects_hardcodes.py` — if any string resembles a rejected pattern there, treat as STOP signal even if Python parser accepts.
6. **Alice Tier-2 verification PASS**: Alice's UC-H trace surfaces `intake_complete_required` (or equivalent) as Tier-2 fail; recorded in handoff §9.
7. **Per-cluster negative check verified**: UC-A case does NOT trigger UC-FP steps; UC-FP case does NOT trigger UC-H intake steps; recorded in handoff §9.
8. **Backward-compat smoke**: SkillLoader accepts all 6 populated YAMLs at Spring bootstrap; existing prompt-composition golden tests pass; 14 smoke + 159 anchor + 12 anchor_outcome + 12 family + 1 Alice load and run cleanly.
9. **No-mocked-LLM-as-proof**: if any handoff claim relies on LLM behaviour, cite a real-LLM source — but S-Eval-3 should not need any LLM-behaviour claim; this is YAML content authoring + Tier-2 verification work.
10. **Bundle policy**: `git status` shows ONLY 6 Skill YAMLs + handoff + (optional authorized minor fixes); nothing under `docs/milestones/`, `docs/sprints/sprint-001-*..sprint-043-*`, no live `docs/milestone_objective.md` edit, no live `docs/codex-findings.md` edit, no live `docs/action_bank.md` edit beyond optional close-action row, no Java/Python source code edits beyond authorized minor fixes.
11. **Stop-conditions review**: any of the 9 STOP signals in §4 fired? If yes, STOP and surface; do NOT commit.

After commit, surface to deliver-agent + human:

- Commit SHA.
- Per-Skill step count (sum to 16-22 within 18-30 envelope).
- Java baseline result (new total / failures / errors / skipped).
- Python test pass status (new total passed / pre-existing failures preserved).
- Backward-compat sample-run result (6 Skills bootstrap clean; 5 fixture buckets all clean).
- **Alice Tier-2 verification result** (`intake_complete_required` or equivalent fail surfaces).
- Per-cluster negative check result.
- Projection token-cost observation (per-turn delta; expected within proposal §5.4 envelope 500-900 tokens).
- §5.3 self-walk summary (count of row-1 / row-2 / any other entries; rationale paragraph per row-2 entry).
- OQ-S43.2 `id` cross-reference usage observation.
- OQ-S43.5 bare-flag truthiness usage observation (if any populated step uses).
- Open questions OQ-S44.N list.
- Any close-readiness notes (your guess at A / A-with-X / B classification — deliver-agent + human + Codex make the actual call at close).
