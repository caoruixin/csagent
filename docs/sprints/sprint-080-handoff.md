---
title: Sprint 080 / S-Auto-25 / M-Auto-6 Sub-sprint C-1 dev handoff — R7 update_intake_fields tool + R2.a#5-ext RESOLVE-intake clarification-budget re-map
doc_tier: sprint-archive
status: current
implementation_status: implemented
source_of_truth: code (server/.../service/runtime/IntakeFieldsMerger.java, service/tools/UpdateIntakeFieldsTool.java, service/runtime/ContextProjectionBuilder.java, service/runtime/ControlKernel.java, service/runtime/skill/SkillLoader.java, config/tool-policy.yaml, skills/resolve_intake_collect_and_handover.yaml)
last_reviewed: 2026-06-06
review_cadence: ad hoc
supersedes: []
superseded_by: null
notes: >
  M-Auto-6 Sub-sprint C-1. R7: a new no-side-effect update_intake_fields(fields={...})
  tool lets the LLM accumulate session.intakeFields across turns WITHOUT triggering
  the handover validator (c14 root cause #1 — state loss). R2.a#5-ext: the phase-aware
  max-repeated-same-action re-map now also fires on RESOLVE + intake UC + free-text
  action, so a c14-style RESOLVE-INTAKE clarification-budget hit labels
  clarification_budget_exhausted instead of turn_budget_exhausted (c14 root cause #2).
  #3 audit chose path β (shared IntakeFieldsMerger helper + characterization test).
  #5 chose overload A (3-arg → 4-arg, single source of truth; sole prod call site
  ControlKernel:313). FENCE WAIVER (deliver-agent approved 2026-06-06): two capability
  YAMLs (tool-policy.yaml + intake skill tools_required) + SkillLoader.VALID_TOOL_NAMES
  were edited as Runtime capability config (§1.4), NOT semantic surface — zero
  procedure / wording / enum change. NO outcome-evidence re-bless this sub-sprint;
  that is the M-Auto-6 milestone-shared re-bless after C-1 + C-2. baseline_dir +
  docs/current_eval_baseline.md UNCHANGED.
---

# Sprint 080 / S-Auto-25 / M-Auto-6 Sub-sprint C-1 — dev handoff

## §0 Cold-start summary + verdict

**Goal.** Ship the runtime enabler for c14 (UC-J multi-turn intake state loss +
RESOLVE-phase clarification-budget mislabel):

- **R7** — a new `update_intake_fields(fields={...})` no-side-effect tool that
  persists `session.intakeFields` across turns via the existing merge path,
  WITHOUT routing through the handover validator. Covers the "send
  incrementally" intake strategy (R1.a already covers "send everything at once"
  on the `request_handover` path).
- **R2.a#5-ext** — extend the S-Auto-23 phase-aware
  `mapBudgetToEscalationReason` re-map so a `max-repeated-same-action` budget on
  a RESOLVE-INTAKE free-text clarification (intake UC) labels
  `clarification_budget_exhausted` instead of `turn_budget_exhausted`. The
  existing DISCOVER path and anti-误杀 #12 (RESOLVE non-intake / tool-call
  repeats stay `turn_budget_exhausted`) are preserved.

Zero semantic procedure / wording change. OBS-S6 (teaching the LLM *when* to
call the new tool) is autoloop work AFTER M-Auto-6 close; R7 is its runtime
enabler only.

**Verdict.** All of #1–#7 landed. **Java `mvn -o test` = 1327 run / 1 fail /
0 err / 2 skip.** The single failure is the inherited
`SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly`
— **verified pre-existing** by `git stash`-ing the full diff and running it on
clean `HEAD` (ea8004f), where it also fails (expected `true` was `false`). This
is the same single inherited failure carried since the S-Auto-23 baseline
(1297/1/0/2 at sub-sprint launch per `sprint-079-handoff.md` §0). Net **+30
tests, no regressions**. Backend `mvn -o -DskipTests package` = **BUILD
SUCCESS** (jar built; Spring-context integration tests in the suite boot with
the new `@Component` tool + edited YAMLs + SkillLoader validation, confirming
startup integrity). **No real-LLM outcome re-bless** — deferred to the
M-Auto-6 milestone-shared run. `baseline_dir` + `docs/current_eval_baseline.md`
**UNCHANGED**.

---

## §1 Implementation

### #3 PRE-FIX audit outcome → path **β** (shared helper)

`AgentRunLoopImpl.persistInlineIntakeFields` (`:999`) was, pre-change:
package-private, keyed on the `intake_fields` argument, run-loop-coupled
(takes a `ToolCall`), and used `objectMapper` +
`IntakeFieldsRegistry.parseCollectedFields` / `mergeFields`. The new tool lives
in a different package (`service.tools`) and uses a `fields` argument, so
direct reuse (α) is impossible. Audit answers:

- (a) tightly coupled to the `request_handover` dispatch path; package-private.
- (b) merge depends ONLY on `call.arguments.intake_fields` — not on `toolName`
  or `escalation_reason`.
- (c) the method does NOT gate on `isIntakeUseCase` internally — that gate is at
  the call site (`AgentRunLoopImpl:505`); the method persists whatever map it is
  given.

→ Chose **β**: extracted the parse → merge → persist body to a new static
helper **`IntakeFieldsMerger.merge(session, incoming, objectMapper)`**
(`server/.../service/runtime/IntakeFieldsMerger.java:55`). Both the
`request_handover` persist path and the new tool call it.
`persistInlineIntakeFields` (`:999–1015`) now extracts `intake_fields` + keeps
its null/empty guard, then delegates at `:1014`. **Byte-equivalence** on the
`request_handover` path is pinned by
`IntakeFieldsMergerCharacterizationTest` (9 tests): empty-existing persist,
union merge, **incoming-wins-on-conflict**, alias canonicalisation,
no-rewrite-when-unchanged, blank-drop, null-session / empty-incoming no-ops,
round-trip. The only behavioural delta vs the inline original is a removed
WARN log on a (practically unreachable) `writeValueAsString` failure of a
`Map<String,String>` — session state is identical (no write on failure in
either version).

### #1 + #2 + #4 — `update_intake_fields` tool

- **#1 tool class** — `service/tools/UpdateIntakeFieldsTool.java` (new,
  `@Component implements Tool`). `getName()`→`update_intake_fields` (`:42`);
  `execute` (`:48`) does structural validation only (`fields` must be a
  non-empty `Map`, else `ToolResult.error`), delegates the merge to
  `IntakeFieldsMerger`, and returns
  `{status: ok, fields_merged: <int>, fields_persisted: [<canonical names>]}`.
  No semantic decision: never inspects the active UC, never derives from message
  content, never calls the validator.
- **#2 dispatch handler** — reuses the shared `IntakeFieldsMerger.merge`
  (path β). Dispatch is the standard `ToolDispatcher.dispatch` path; the tool's
  `ToolEvent` is recorded by the existing generic site
  (`AgentRunLoopImpl:772–777`).
- **#4 registration + projection** — the tool is auto-registered via Spring
  `List<Tool>` injection into `ToolDispatcher`. Its schema is declared in
  `ContextProjectionBuilder.initToolSchemas` (`:149–157`) with a `fields`
  free-form `string→string` object (`additionalProperties: {type: string}`,
  `required: [fields]`) built by `buildUpdateIntakeFieldsArgsSchema` (`:213`).
  The description references `required_intake_fields_for_active_uc` (the R1.a #2
  projection slot, confirmed present at `ContextProjectionBuilder:440`) and does
  NOT enumerate any per-UC field name.

Gating tests: `UpdateIntakeFieldsToolTest` (10) + `UpdateIntakeFieldsDispatchSmokeTest` (2).

### #5 — R2.a#5-ext mapping → overload **A**

`mapBudgetToEscalationReason` audit: the 3-arg overload had **exactly one
production call site** (`ControlKernel:311`, now `:313`) — STOP-#5 (a second
site) **not** triggered. Test call sites: `MapBudgetToClarificationLabelTest`
(3-arg, rewritten) + `ControlKernelEscalationReasonTest` (single-arg only,
untouched).

→ Chose **A**: changed the 3-arg signature to 4-arg
`mapBudgetToEscalationReason(bucket, currentPhase, lastAction, activeUseCase)`
(`ControlKernel:782`) — single source of truth, no vestigial overload. The
mapping now fires when `bucket==max-repeated-same-action` AND
`isFreeTextActionKey(lastAction)` AND (`DISCOVER` OR (`RESOLVE` AND
`IntakeFieldsRegistry.isIntakeUseCase(activeUseCase)`)). The sole prod call site
(`:313–315`) now passes `session.getActiveUseCase()`. `isFreeTextActionKey`
(`:802`) and the single-arg base (`:736`) are unchanged; **no new enum value**.

Gating tests: `MapBudgetToClarificationLabelTest` (11 → 21): RESOLVE-intake
positives (UC-G/H/I/J/K answer + clarify, case-insensitive), and anti-误杀
negatives (RESOLVE non-intake UC, RESOLVE null UC, RESOLVE intake + tool-call,
other budgets on intake UC, "no new enum" canonical-member assertion).

### #6 — anti-误杀 test suite

| Test class | Count | Covers |
|---|---|---|
| `UpdateIntakeFieldsToolTest` | 10 | R7 positives (single / multi-call accumulation / alias); **validator non-bypass** (real `SkillGuardrailDispatcher` still rejects after partial stash); **full-stash → handover passes**; empty / wrong-type / null rejected, session unchanged; **no auto-derivation** (only `fields` arg read); blank-only → 0 merged |
| `UpdateIntakeFieldsDispatchSmokeTest` | 2 | #7 dispatch wiring through the REAL `ToolDispatcher` + REAL `ToolPolicyEnforcer` (loads `tool-policy.yaml`): UC-J dispatch+persist; UC-A policy-blocked |
| `IntakeFieldsMergerCharacterizationTest` | 9 | path-β byte-equivalence (above) |
| `MapBudgetToClarificationLabelTest` | 21 | R2.a #5 + R2.a#5-ext (above) |

The validator non-bypass / full-stash tests use the production
`SkillGuardrailDispatcher.checkBeforeDispatch` (the same `intake_complete_required`
guardrail the run loop uses, which reads `session.intakeFields` via
`IntakeFieldsRegistry.intakeComplete`, `SkillGuardrailDispatcher:282`). Because
the tool persists to the same blob the validator reads, the tool can only make
intake accumulation VISIBLE to the validator — it can never make an incomplete
intake pass.

### #7 — backend rebuild + integration smoke (dispatch wiring, NOT outcome)

- **Rebuild**: `mvn -o -DskipTests package` = BUILD SUCCESS;
  `target/csagent-server-0.1.0-SNAPSHOT.jar` built.
- **Startup integrity**: the suite's `@SpringBootTest` integration tests boot
  the full context (real `ToolDispatcher` with the new `@Component` tool, real
  `ToolPolicyEnforcer` loading the edited `tool-policy.yaml`, `SkillLoader`
  validating the edited intake skill) and pass.
- **Dispatch smoke** (`UpdateIntakeFieldsDispatchSmokeTest`) — the reproducible,
  CI-gated equivalent of the live POST. A live-server LLM-driven invocation is
  NOT possible this sub-sprint (the LLM is not taught to call the tool until
  OBS-S6, post-milestone; teaching is forbidden here), so the dispatch path is
  evidenced through the real beans instead:

  - **Request shape** (as carried by an LLM tool call / `POST
    /v1/chat/sessions/{id}/messages` turn):
    `{"name":"update_intake_fields","arguments":{"fields":{"report_type":"scam"}}}`
  - **Dispatch**: `ToolDispatcher.dispatch("update_intake_fields",
    session(UC-J), {"fields":{"report_type":"scam"}})` → passes
    `isToolAllowed("update_intake_fields","UC-J")` (real `tool-policy.yaml`) →
    `UpdateIntakeFieldsTool.execute`.
  - **Response body** (`ToolResult.data`):
    `{"status":"ok","fields_merged":1,"fields_persisted":["report_type"]}`;
    `session.intakeFields` now parses to `{report_type=scam}`.
  - **Trace event**: standard `ToolEvent` (toolName=`update_intake_fields`,
    arguments=the `fields` map, success=true, resultData=the response body,
    latencyMs) — same shape as every other tool.
  - **Follow-up validator path** (unchanged): after stashing ALL UC-J required
    fields via the tool, `request_handover(intake_complete_for_uc_j)` →
    `checkBeforeDispatch` returns `Optional.empty()` (passes). With only a subset
    → returns `INTAKE_INCOMPLETE_REJECT_REASON` (still rejects). R7 does not
    bypass the validator.

---

## §1.x Capability-wiring fence waiver (deliver-agent approved 2026-06-06)

The prompt fence forbids "Any prompt / yaml / CaseSpec under
`server/src/main/resources/`". During the #4 audit the tool was found to require
**four** runtime gates, all driven by capability config the prompt fence (and
one Java validator) covers:

1. `ToolDispatcher.validateAgainstPlan` → `plan.allowedTools()` =
   `skill.toolsRequired()` (`PhaseEvaluator:469`).
2. `ContextProjectionBuilder` `tool_schemas` filter → `plan.allowedTools()`
   (`:960–977`).
3. `ToolPolicyEnforcer.isToolAllowed` → `config/tool-policy.yaml`.
4. **`SkillLoader.VALID_TOOL_NAMES`** (`SkillLoader.java:95`) — a hardcoded
   known-tools set that rejects a `tools_required` entry naming an unknown tool.
   **This fourth gate was not anticipated by the prompt** and broke skill
   loading for every test once the tool was added to `tools_required`.

The deliver-agent selected **Option A** (treat tool-policy + the intake skill's
`tools_required` as Runtime capability config per §1.4 "tool schema /
capability boundary", NOT a semantic surface). The four capability edits are:

- `config/tool-policy.yaml:17–21` — `update_intake_fields: {AGENT_VISIBLE,
  allowed-ucs: [UC-G, UC-H, UC-I, UC-J, UC-K]}`.
- `skills/resolve_intake_collect_and_handover.yaml:16` —
  `update_intake_fields` added to `tools_required`.
- `service/runtime/skill/SkillLoader.java:102` — `update_intake_fields` added to
  `VALID_TOOL_NAMES` (same capability-registration category; included under the
  approved Option-A scope as a mandatory consequence).

**Zero** procedure / objective / grounding / escalation / wording / enum /
CaseSpec change. The intake skill's `procedure` text is byte-untouched (OBS-S6
teaching is post-milestone).

---

## §2 Test / eval results

- **Java** `mvn -o test`: **1327 / 1 / 0 / 2** (run / fail / err / skip).
  Sole failure = inherited `SystemPromptUserRequestedTiebreakerTest` (verified
  pre-existing via `git stash` on clean HEAD). +30 net tests vs the inherited
  1297/1/0/2 launch baseline; **no regressions**.
- **Golden-set test updates** (driven by the approved tool addition — the
  intake skill's projected tool set grew from `[request_handover]` to
  `[request_handover, update_intake_fields]`): `PhaseEvaluatorPlanTest`
  (4 intake-UC assertions; the `assertFalse contains create_case_controlled /
  search_knowledge` invariants are preserved), `PhaseEvaluatorResolveSkillIntegrationTest:252`,
  `PhaseEvaluatorSkillIntegrationTest:400`.
- **Eval pytest / Autoloop pytest**: **UNCHANGED by construction** — the diff is
  confined to `server/src/{main,test}/...` Java + the two capability YAMLs
  (`git status` shows NO `eval_interactive/`, `autoloop/`, or `docs/` runtime
  file touched). Not re-run: there is no eval-side change, and the env-dependent
  corpus_lint count nuance would add noise without signal.

### Wiring evidence vs outcome evidence (§5.7)

- **Wiring evidence (this sub-sprint)**: the Java unit + integration tests above
  (tool dispatch, merge byte-equivalence, validator non-bypass, projection
  schema, mapping label, real-beans dispatch). Mocked/no-LLM by design.
- **Outcome evidence (deferred)**: the M-Auto-6 milestone-shared **real-LLM**
  re-bless after C-1 + C-2 both land. Mocked-LLM tests are NOT primary evidence
  that a behaviour changed; they cover projection / dispatch wiring only.

---

## §3 STOP / fence confirmations

- File fence respected; capability-wiring waiver (Option A) is the only crossing,
  explicitly approved (§1.x). No other forbidden file touched.
- `IntakeFieldsRegistry.java` content **unchanged** (only iterated /
  `isIntakeUseCase` read).
- `SkillGuardrailDispatcher.java` **unchanged** (validator semantics preserved;
  R7 does not bypass it).
- `BudgetChecker.java`, `FormContextIngestionService.java`, UI, `eval_interactive/`,
  autoloop scoring set, `autoloop/config.yaml` **untouched**.
- **No new `escalation_reason` enum value** (R2.a#5-ext re-maps to the existing
  `clarification_budget_exhausted`).
- No prompt / CaseSpec / simulator / scoring / intake-skill-procedure touched.
- `baseline_dir` **UNCHANGED**; `docs/current_eval_baseline.md` **UNCHANGED**.
- **No real-LLM outcome re-bless launched** (deferred to milestone close).

STOP conditions evaluated, none tripped except the surfaced capability-wiring
fork (resolved by deliver-agent → Option A): #3 audit found no phase/UC-dependent
behaviour the new tool would break; #5 audit found no second prod call site;
no test failure suggested validator bypass (the non-bypass tests assert the
validator still fires).

---

## §7 Layer-classification + anti-hardcode stanza

**Target failure layer:** `skill_state` + `infra` + `prompt_projection`
(R7 new tool — `skill_state` for cross-turn intake-field accumulation;
`infra` for dispatch wiring; `prompt_projection` for the new
`tool_schemas` entry making the tool visible to the LLM) + `infra`
(R2.a#5-ext mapping function signature extension; control-plane label
correctness on the c14-style RESOLVE-phase intake clarification budget
hit).

**Tier-0 invariant:** This sub-sprint adds no Tier-0 invariant. R7 adds
a new TOOL (not a Tier-0 surface per §1.4); the tool does not bypass
the handover validator (which IS Tier-0); R2.a#5-ext extends an
existing mapping function with one additional AND-guarded condition;
no new enum value.

**Semantic hardcode:** No semantic hardcode introduced.
- R7's tool arguments schema is a free-form `string → string` map; NO
  per-UC enumeration in the tool. The per-UC required-fields contract
  remains in `IntakeFieldsRegistry` and is projected via
  `required_intake_fields_for_active_uc` (shipped at R1.a #2). NO
  server-side semantic decision about which fields belong to which UC.
- R7's persist logic reuses the existing
  `persistInlineIntakeFields` merge path (either invoked directly or
  via a shared helper extracted by the #3 audit — behaviour-equivalent
  to the `request_handover` persist path).
- R7 does NOT auto-derive calls from runtime state; the tool MUST be
  LLM-invoked.
- R2.a#5-ext extends the existing mapping function with one additional
  AND-guarded condition (`phase=RESOLVE AND
  IntakeFieldsRegistry.isIntakeUseCase(activeUseCase) AND
  isFreeTextActionKey(lastAction)`). `isIntakeUseCase` is the same
  classification R1.a #2 uses — zero new per-UC matrix.
- No new `escalation_reason` enum value introduced.

**Generalization coverage:** target / neighbor / negative / shadow case
counts: 1 / ~8 / ~10 / 0
- target: c14 (UC-J multi-turn intake state loss + RESOLVE-phase
  clarification budget mislabel).
- neighbor: all intake-UC (UC-G / UC-H / UC-I / UC-J / UC-K) cases
  that go through DISCOVER / RESOLVE intake collection; the new tool
  is universal so adoption can vary by UC but availability is uniform.
- negative: R7 anti-误杀 negatives #1-#5 (validator non-bypass; no
  auto-derivation; empty fields rejected; wrong type rejected;
  full-stash → handover passes); R2.a#5-ext anti-误杀 negatives #1-#5
  (RESOLVE non-intake UC unchanged; RESOLVE no active UC unchanged;
  tool-call repeat unchanged; other budget unchanged; no new enum).
- shadow: not applicable (mocked-LLM tests are wiring evidence; real
  evidence is milestone-shared re-bless).

---

## §4 Commit map

1. `IntakeFieldsMerger` extraction (path β) + characterization test.
2. `UpdateIntakeFieldsTool` + projection schema + capability wiring
   (tool-policy.yaml + intake skill tools_required + SkillLoader.VALID_TOOL_NAMES)
   + tool/dispatch-smoke tests + golden-set test updates.
3. `ControlKernel` R2.a#5-ext (overload A) + `MapBudgetToClarificationLabelTest`.
4. This handoff.

Per-sub-sprint Codex review REQUIRED (R7 adds an LLM-facing tool name —
semantic-touching per `iteration_governance.md` §7 +
`process/milestone-framework.md` §4.3). Codex prompt artifact:
`compact/sprint-080-codex-review-prompt.md` (deliver-agent authors at close).
