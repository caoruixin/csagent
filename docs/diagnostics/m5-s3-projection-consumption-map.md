---
title: M5 / S3 — Projection field consumption map (C1)
doc_tier: diagnostic
status: current
implementation_status: implemented
source_of_truth: this file (matrix); ContextProjectionBuilder.java (raw projection emit)
last_reviewed: 2026-05-25
review_cadence: per sub-sprint at S3 close; per milestone at M5 close
supersedes: []
superseded_by: null
notes: >
  C1 deliverable for Sprint 52 / M5 S3 — produced BEFORE any C2 projection-field
  change per the sprint contract's hard ordering fence. Diagnostic only: the
  matrix observes consumer dependencies of every emitted projection field; it
  introduces no semantic decision and ships no behaviour change. Per the §5.6
  eval-evidence-gate principle, the matrix evidence column "real trace shows
  the LLM using it" is sampled (one per family) — a full LLM-attention audit
  is out of scope. Each C2 recommendation surfaces an explicit STOP-and-surface
  decision for the human; S3 does NOT auto-converge any field whose row leaves
  ambiguity. Consumes OQ-S51.2 (§3 row #1a). HEAD anchor: `cf9c120`.
---

# M5 / S3 — Projection field consumption map (C1)

## 0. Why this matrix exists

The per-turn projection built by `ContextProjectionBuilder.buildProjection`
(`server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java:337`)
and `ContextProjectionBuilder.build` (`:787`) emits ~30 top-level fields. M2
made the plan / tool surface Skill-driven (`PhasePlan.allowedTools()` /
`Skill.toolsRequired()`) but the projection layer kept its pre-M2 UC-driven
emit. Sprint 52 / S3 must:

- (C1, this doc) prove which consumer depends on each field, so any C2
  convergence is provably consumer-safe;
- (C2, gated by this matrix) make the projection read Skill declarations
  where M2 left it UC-driven, without removing a signal the LLM relies on.

The §5.6 evidence-gate rule applies: real-LLM rerun on the bad-case suite is
the behaviour-risk gate for C2; this matrix is the **wiring-risk gate** for
C2 (no field is touched unless its row shows no breakable consumer).

## 1. Consumer columns (legend)

For each emitted projection field, the matrix records dependence on five
consumer surfaces. Cell semantics:

| Column | Meaning | Authoritative source |
|---|---|---|
| **SP — system_prompt** | The 80-line system prompt explicitly names the slot (or rules over it). Slots NOT named here are surfaced to the LLM as raw JSON only. | `server/src/main/resources/prompts/system_prompt.txt` |
| **EC — eval trace contract** | `eval_interactive/.../trace/collector.py` (`TraceCollector` strict-mode validator) raises `TraceContractError` if the field is missing. | `eval_interactive/eval_interactive/trace/collector.py:108-144` (`REQUIRED_*_FIELDS`, `CONDITIONAL_SESSION_FIELDS`, `TOOL_SPECIFIC_REQUIRED_ARGS`) |
| **ES — eval scoring** | `eval_interactive/.../scoring/skill_procedure_check.py` (Tier-2 Skill procedure followship) OR `batch/executor.py` reads the slot from `per_turn_trace[].projection`. | `eval_interactive/eval_interactive/scoring/skill_procedure_check.py:485-537`; `eval_interactive/eval_interactive/batch/executor.py:524-571` |
| **DT — drift/task reconstruction** | `ContextProjectionBuilder.buildDriftAndTaskHistory` (`:1265-1305`) re-parses the field from the prior turn's persisted `BotTurn.projected_context`. | `ContextProjectionBuilder.java:1283-1298` |
| **SK — Skill declaration** | The active Skill names the slot in its `required_context_keys` OR `state_inheritance.soft_signal_via_projection` declaration (M2 abstraction). | `server/src/main/resources/skills/*.yaml` |
| **LLM — sampled raw trace** | A real-LLM per-step projection (`bot_turn_llm_calls.projection`, persisted post-S2) shows the field present and non-trivial; the LLM's raw response cites or appears to use it. (Sampled, not exhaustive.) | OQ-S51.1 eyeball session `92a5c7c7-2054-4b54-84f0-0dc41a95703c` (UC-A FAQ) per `sprint-051-handoff.md:469-477`; field-by-field LLM-attention auditing is OOSR for S3 — only the presence column is asserted here. |

Cell values: **REQ** (raises / breaks if absent) · **READ** (consumed but
tolerates absence — empty/null fallback) · **DECL** (declaration only —
not yet enforced/wired) · **PRESENT** (sampled in raw trace) · **—**
(no observed consumer in this column).

## 2. Two builder entry points

The projection is emitted via two ContextProjectionBuilder methods, with
distinct caller populations and one OVERWRITE point:

| Entry point | Caller(s) | Knowledge surface | tool_schemas surface |
|---|---|---|---|
| `buildProjection(session, history, knowledgeHits, userMessage)` (`:337-702`) | (a) Legacy fallback in `PhaseEvaluator.evaluate(...)` for unmapped (phase, UC) tuples; (b) `PhaseEvaluator.evaluateResolveFaq*` pre-loaded-knowledge paths (`:945,:982,:1025,:1105,:1373`); (c) `ControlKernel.recordTurn(...)` for non-run-loop turn persistence (`:1670`); (d) `AgentRunLoopImpl` indirectly via `build(...)` which delegates to `buildProjection(.., null, ..)` first. | `knowledge_hits` slot when caller passes non-null `searchResult.getHits()`. NULL when called from `build(...)` (run-loop). | UC-driven base via `toolPolicyEnforcer.getVisibleToolsForUc(activeUc)` (`:621-633`). |
| `build(session, history, plan, userMessage, accumulatedToolResults, priorToolEvents)` (`:787-934`) | Sole caller: `AgentRunLoopImpl.run(...)` (`:190-192`) per turn-step. | `accumulated_tool_results.search_knowledge` (run-loop fetches knowledge via tool, results land here). | OVERWRITES the UC-driven base from `buildProjection(...)` with the **Skill/plan-filtered** set via `plan.allowedTools()` (`:845-863`). |

OVERWRITE evidence: `build(...)` first delegates to
`buildProjection(.., null, ..)` (`:797`), then unconditionally rewrites the
`tool_schemas` slot to the plan-filtered list when `plan != null` and
`plan.allowedTools() != null` (`:845-863`). So when the run-loop owns the
turn, the UC-driven base is computed and discarded; when `recordTurn`
re-builds the projection for persistence on non-run-loop paths, the
UC-driven base is what gets persisted.

`BotTurn.projected_context` (the single-column persisted value) carries a
**FAQ-grounding overlay** applied by
`ControlKernel.mergeFaqGroundingIntoProjection(...)` (`:2363-2385`) post-
loop (`:2029-2031`). This is the OQ-S51.2 distinction — see row #1a below.

## 3. Field consumption matrix

> Field rows are grouped by source builder (base vs run-loop overlay). Line
> anchors are `ContextProjectionBuilder.java` unless noted.

### 3.A `session.*` (`:345-353`)

| Field | SP | EC | ES | DT | SK | LLM | Disposition |
|---|---|---|---|---|---|---|---|
| `session.session_id` | — | — | — | — | — | PRESENT | Required for projection identity; trivially safe. |
| `session.current_phase` | — | **REQ** (session-level, `REQUIRED_SESSION_FIELDS = ("current_phase",)`, collector.py:108-110 — read via `/v1/chat/sessions/{id}`, NOT projection. So removal from projection would NOT fail the contract; admin trace would still show it. Listed here for completeness.) | READ (`skill_procedure_check.py:514-516` reads `proj.get("session")`; current_phase is a sub-field) | — | — | PRESENT | KEEP. EC requires it via the session endpoint, but the projection also re-emits it; LLM reads phase from `phase_plan.phase` (run-loop path) and from `session.current_phase` (base path). |
| `session.active_use_case` | — | **REQ** (conditional after first bot turn, collector.py:120-122 / 367-375 — again read from session endpoint, not projection. Removal from projection is contract-safe but a duplication-cleanup candidate for S4.) | READ (via `proj.get("session")`) | DT REQ (drift_history copies `active_use_case` from prior turn's projection — `:1287`) | — | PRESENT | KEEP. `buildDriftAndTaskHistory` reads it from historical `projected_context`; removing it would break drift reconstruction. |
| `session.total_bot_turns` | — | — | READ | — | — | PRESENT | KEEP. Duplicated in `budget_state.total_bot_turns` (`:609`); S4 dedup candidate. |
| `session.clarification_count` | — | — | READ | — | — | PRESENT | KEEP. Duplicated in `budget_state.clarification_count` (`:611`); S4 dedup candidate. |
| `session.faq_miss_count` | — | — | READ | — | — | PRESENT | KEEP. Duplicated in `budget_state.faq_miss_count` (`:613`); S4 dedup candidate. |
| `session.handling_state` | — | — | READ | — | — | PRESENT | KEEP. Used informationally by the LLM. |

### 3.B `task_summary` (`:357-358`, builder `buildTaskSummary` `:1333-1355`)

| Field | SP | EC | ES | DT | SK | LLM | Disposition |
|---|---|---|---|---|---|---|---|
| `task_summary` (natural-language restatement of topic_subject + UC name + intent_confidence + phase) | — | — | — | — | — | PRESENT (restates structured info) | S4 CANDIDATE — restates info the LLM already has structured (`session.active_use_case`, `phase_plan.phase`, etc.). Cheap to keep; removal needs real-LLM rerun. NOT in scope for S3 C2. |

### 3.C `risk_flags` (`:361-368`)

| Field | SP | EC | ES | DT | SK | LLM | Disposition |
|---|---|---|---|---|---|---|---|
| `risk_flags` (array of UC's risk-level label) | — | — | — | — | — | PRESENT | KEEP. UC-registry-driven; no per-UC if-else. |

### 3.D `intake_state` (`:375-402`, gated `IntakeFieldsRegistry.isIntakeUseCase`)

| Field | SP | EC | ES | DT | SK | LLM | Disposition |
|---|---|---|---|---|---|---|---|
| `intake_state.required_fields` / `fields_collected` / `fields_remaining` / `intake_complete` | — | — | **READ** (`skill_procedure_check.py:528-537` walks turns reverse, reads `proj.get("intake_state")` → `fields_collected` — LOAD-BEARING for Tier-2 `intake-complete-before-handover` step) | — | INDIRECT (the `resolve_intake_collect_and_handover` Skill `critical_steps` references intake-complete semantics) | PRESENT (UC-G/H/I/J/K only) | KEEP. The eval Tier-2 scorer requires `intake_state.fields_collected`. Builder already gates emit by UC path via the IntakeFieldsRegistry (registry-driven, NOT a per-UC if-else). |

### 3.E Candidate / disambiguation soft signals (`:410-457`)

| Field | SP | EC | ES | DT | SK | LLM | Disposition |
|---|---|---|---|---|---|---|---|
| `candidate_use_cases` (`:410-418`) | — | — | — | — | DECL (`discover_triage.yaml` declares `candidate_use_cases` in `required_context_keys`) | PRESENT (often empty) | KEEP. Skill declares it; Skill-gated emit would EMIT it for `discover_triage` and OMIT for `confirm`/`terminal`/`escalate` (no declaration). C2 #2 candidate (with risk: emitting the slot unconditionally is current behaviour, and confirm/terminal/escalate Skills' empty `required_context_keys` would silently drop it — semantic effect unproven). **S3 disposition: STOP-and-surface — do NOT auto-gate without real-LLM rerun proving no regression.** |
| `alternate_candidate_use_cases` (`:431-440`, Sprint 31) | — | — | — | — | **DECL** (`discover_triage.yaml` `state_inheritance.soft_signal_via_projection`) | PRESENT (empty array unless intake AMBIGUOUS) | KEEP. C2 #5 candidate: gate emit by `skill.stateInheritance().softSignalViaProjection().contains("alternate_candidate_use_cases")`. Today: emitted unconditionally for all phases; under Skill gating: emitted only for `discover_triage`. **Semantic risk:** non-DISCOVER turns lose this slot's visibility. Test bar is real-LLM rerun on bad cases that traverse DISCOVER→RESOLVE (alice, cs011, cs012). **S3 disposition: STOP-and-surface.** |
| `discover_disambiguation_signals` (`:456-457`, Sprint 33) | — | — | — | — | **DECL** (`discover_triage.yaml` soft_signal_via_projection) | PRESENT (sub-fields often null/empty) | Same as `alternate_candidate_use_cases`. C2 #5 candidate. **S3 disposition: STOP-and-surface.** |
| `prior_use_case_carry` (`:472-474`, Sprint 41) | — | — | — | — | **DECL** (`resolve_faq_grounded_answer.yaml` + `resolve_intake_collect_and_handover.yaml` soft_signal_via_projection) | PRESENT (NullNode unless prior UC switch within aging window) | KEEP. C2 #5 candidate: gate emit by Skill declaration. Today emitted unconditionally, but always NullNode for confirm/escalate/terminal phases (no prior switch within window). Switch to Skill-gating would change shape from explicit null → absent slot for non-FAQ/INTAKE Skills. **Semantic risk:** low (signal is null-when-N/A today); still requires real-LLM rerun. **S3 disposition: STOP-and-surface — proceed only with real-LLM rerun gate.** |

### 3.F Reroute / issue / task state (`:484-548`)

| Field | SP | EC | ES | DT | SK | LLM | Disposition |
|---|---|---|---|---|---|---|---|
| `previous_active_use_case` (`:484-489`) | — | — | — | **DT REQ** (`:1288`) | — | PRESENT (often null) | KEEP. DT consumer requires it for drift_history reconstruction. |
| `drift_type` (`:490-495`) | — | — | — | **DT REQ** (`:1283`) | — | PRESENT (often null) | KEEP. DT consumer. |
| `current_task_type` (`:496-501`) | — | — | — | **DT REQ** (`:1294`) | — | PRESENT (often null) | KEEP. DT consumer (task_history). |
| `primary_entity` (`:502-516`, ObjectNode | null) | — | — | — | — | — | PRESENT (often null) | KEEP. Used informationally by the LLM; no consumer would break on removal but no clear benefit to remove. |
| `issue_status_summary` (`:517-526`, default "open") | — | — | — | **DT REQ** (`:1298`) | — | PRESENT | KEEP. DT consumer. |
| `task_status` (`:533-538`, default "in_progress") | — | — | — | **DT REQ** (`:1295`) | — | PRESENT | KEEP. DT consumer. |
| `last_entity_context_ref` (`:543-548`) | — | — | — | — | — | PRESENT (often null) | KEEP. Informational. |

### 3.G §N0 nullable strings (`:560-571`, Sprint 12 §N0)

| Field | SP | EC | ES | DT | SK | LLM | Disposition |
|---|---|---|---|---|---|---|---|
| `predicted_use_case` | — | — | — | **DT REQ** (`:1286`) | — | PRESENT (often null) | KEEP. DT consumer. |
| `intent_relation` | — | — | — | **DT REQ** (`:1284`) | — | PRESENT (often null) | KEEP. DT consumer. |
| `reroute_action` | — | — | — | **DT REQ** (`:1285`) | — | PRESENT (often null) | KEEP. DT consumer. |
| `phase_transition_reason` | — | — | — | **DT REQ** (`:1289`) | — | PRESENT (often null) | KEEP. DT consumer. |
| `resolve_disposition` | — | — | — | **DT REQ** (`:1296`) | — | PRESENT (often null) | KEEP. DT consumer. |
| `record_outcome_guard_result` | — | — | — | **DT REQ** (`:1297`) | — | PRESENT (often null) | KEEP. DT consumer. |

### 3.H `terminal_evidence` (`:577-592`)

| Field | SP | EC | ES | DT | SK | LLM | Disposition |
|---|---|---|---|---|---|---|---|
| `terminal_evidence.record_outcome_attempted` / `_succeeded` / `_success` | — | — | — | — | — | PRESENT | KEEP. Used by trace UI; no Skill / EC / DT dependency. |

### 3.I `drift_history` / `task_history` (`:600-605`)

| Field | SP | EC | ES | DT | SK | LLM | Disposition |
|---|---|---|---|---|---|---|---|
| `drift_history` (array of {turn_index, drift_type, intent_relation, reroute_action, predicted_use_case, active_use_case, previous_active_use_case, phase_transition_reason}) | — | — | — | self-recursion (builds itself) | — | PRESENT | KEEP. Self-reconstruction from prior turns' `projected_context`. S4 candidate to evaluate cost-of-reparse vs LLM benefit. |
| `task_history` (array of {turn_index, current_task_type, task_status, resolve_disposition, record_outcome_guard_result, issue_status_summary}) | — | — | — | self-recursion | — | PRESENT | KEEP. Self-reconstruction; same S4 candidate as above. |

### 3.J `budget_state` (`:608-615`)

| Field | SP | EC | ES | DT | SK | LLM | Disposition |
|---|---|---|---|---|---|---|---|
| `budget_state.total_bot_turns` / `max_bot_turns` / `clarification_count` / `max_clarification` / `faq_miss_count` / `max_faq_miss` | — | — | — | — | — | PRESENT | KEEP. Used by LLM for budget awareness. Some sub-fields duplicate `session.*` — S4 dedup candidate. |

### 3.K `tool_schemas` (`:621-633` UC-driven base; `:845-863` Skill-filtered overwrite)

| Field | SP | EC | ES | DT | SK | LLM | Disposition |
|---|---|---|---|---|---|---|---|
| `tool_schemas` (base, UC-driven via `toolPolicyEnforcer.getVisibleToolsForUc(activeUc)`) | **REQ** (system_prompt:8 names `tool_schemas` as canonical tool surface) | — | — | — | — | PRESENT (run-loop path: overwritten by Skill-filtered; legacy/recordTurn: this is the persisted value) | **C2 #4 candidate.** SP requires the slot; the question is which COMPUTATION fills it. Today: `buildProjection(...)` computes UC-driven, `build(...)` overwrites with Skill-filtered. Removing the UC-driven base would break legacy / `recordTurn` callers (which call `buildProjection` directly and never reach the Skill-filtered overwrite). **Safe convergence path:** replace the `toolPolicyEnforcer.getVisibleToolsForUc(activeUc)` call in `buildProjection(...)` with a Skill-registry-derived equivalent (`skillRegistry.select(phase, activeUc).map(Skill::toolsRequired)`), so BOTH entry points produce the Skill-filtered set and the OVERWRITE in `build(...)` becomes a no-op (still safe to keep as defense-in-depth). **S3 disposition: REQUIRES PER-FIELD HUMAN APPROVAL at C1 review.** The change is registry/Skill-driven, no per-UC if-else; but it touches the legacy fallback path. Real-LLM rerun is the proof. |
| `tool_schemas` (Skill-filtered overwrite, `:845-863`) | as above | — | — | — | **DECL** (Skill `tools_required`) | PRESENT (run-loop path) | KEEP. This is the M2-correct end state. |

### 3.L Context blocks (`:636-648`)

| Field | SP | EC | ES | DT | SK | LLM | Disposition |
|---|---|---|---|---|---|---|---|
| `form_context` (`:636-638`, emitted when `session.formContext` non-blank) | — | — | — | — | **DECL** (every Skill declares `form_context` in `required_context_keys`) | PRESENT | KEEP. Already emitted only when data exists (data-driven gate, not UC if-else); no change required. C2 #2 candidate (Skill-gating) would NOT change current behaviour. |
| `customer_context` (`:641-643`) | — | — | — | — | **DECL** (`confirm.yaml`, `escalate.yaml`, `resolve_*.yaml` declare; `discover_triage.yaml` and `terminal.yaml` do NOT) | PRESENT | **C2 #2 candidate with RISK.** Today emitted unconditionally when present. Skill-gating would suppress for `discover_triage` and `terminal`. **Semantic risk:** DISCOVER turns lose `customer_context` even when present — could degrade clarification quality. **S3 disposition: STOP-and-surface — do NOT auto-gate.** |
| `listing_context` (`:646-648`) | — | — | — | — | **DECL** (only `resolve_faq_grounded_answer.yaml` declares; all other Skills do NOT) | PRESENT | **C2 #2 candidate with RISK.** Skill-gating would suppress for DISCOVER, CONFIRM, ESCALATE, CLOSE, RESOLVE-INTAKE — significantly narrowing visibility. **Semantic risk:** HIGH (e.g., UC-G/H listing-removed flows might need the context during INTAKE). **S3 disposition: STOP-and-surface.** |

### 3.M `conversation_history` (`:651-668`, last 10 turns)

| Field | SP | EC | ES | DT | SK | LLM | Disposition |
|---|---|---|---|---|---|---|---|
| `conversation_history[].{turn_index, user_message, bot_response, tool_calls}` | — | — | — | (DT walks the same source list, not this slot) | DECL (`discover_triage.yaml` declares) | PRESENT | KEEP. C2 #2 would suppress for non-discover Skills — high-risk (loses dialogue memory). **S3 disposition: STOP-and-surface.** |

### 3.N `knowledge_hits` + `knowledge_instruction` (`:670-691`, gated by non-null hits)

| Field | SP | EC | ES | DT | SK | LLM | Disposition |
|---|---|---|---|---|---|---|---|
| `knowledge_hits` (array of {source_id, title, snippet, score}) + `knowledge_instruction` | — | — | — | — | — | PRESENT (legacy FAQ path only; run-loop fetches via tool, results land in `accumulated_tool_results`) | KEEP. Non-dead: `PhaseEvaluator.evaluateResolveFaq*` paths pass non-null hits (`:945,:982,:1025,:1105,:1373`). C3 dual-path coherence question (M5 §7) — defer to S4 (which canonical path under M3-Eval). |

### 3.O `current_user_message` (`:694`)

| Field | SP | EC | ES | DT | SK | LLM | Disposition |
|---|---|---|---|---|---|---|---|
| `current_user_message` (PII-redacted) | — | — | — | — | — | PRESENT | KEEP. Used by LLM as the prompt. |

### 3.P Run-loop additions (in `build(...)`, `:787-934`)

| Field | SP | EC | ES | DT | SK | LLM | Disposition |
|---|---|---|---|---|---|---|---|
| `already_called` (`:805`, Sprint 20) | **REQ** (system_prompt:23-28 explicitly instructs on the slot) | — | — | — | — | PRESENT | KEEP. SP requires it. |
| `phase_plan` (`:812-912`, Sprint D16) | — | — | **READ** (`executor.py:560` reads `projection.get("phase_plan")` to extract `critical_steps[].id` for Tier-2 scoping per the Option A executor wiring — LOAD-BEARING for `_compute_tier2_result`) | — | — | PRESENT | KEEP. Tier-2 scoring depends on `phase_plan.critical_steps`. |
| `phase_plan.critical_steps` (`:885-898`, Sprint 43 / S-Eval-2) | — | — | **READ** (`executor.py:489` via `_collect_presented_step_ids(per_turn_trace)` — single-source-of-truth for Tier-2 scoping) | — | DECL (Skill `critical_steps`) | PRESENT | KEEP. Tier-2 evidence column. |
| `accumulated_tool_results` (`:916-926`) | **REQ** (system_prompt:24-25 instructs on the slot) | — | **READ** (`skill_procedure_check.py:497` reads `proj.get("accumulated_tool_results")` for Tier-2 followship; load-bearing) | — | — | PRESENT | KEEP. Both SP and ES require it. |

### 3.Q `faq_grounding` overlay (PRE-EXISTING, applied post-loop by ControlKernel — see row #1a below)

| Field | SP | EC | ES | DT | SK | LLM | Disposition |
|---|---|---|---|---|---|---|---|
| `faq_grounding.{faq_grounding_state, citation_present, citation_match, citation_drift, resolved_but_uncited, retrieved_but_unresolved}` (`ControlKernel:2363-2385`) | — | — | — | (not in DT walked keys) | — | PRESENT (overlay; not in raw per-step projection) | DO NOT TOUCH (OQ-S51.2 fence). Load-bearing for `Sprint141FaqGroundingTracePersistenceTest`. See row #1a. |

### 3.R `moderation_context` (declared but never emitted — C2 #3 target)

| Field | SP | EC | ES | DT | SK | LLM | Disposition |
|---|---|---|---|---|---|---|---|
| `moderation_context` (never emitted) | — | — | — | — | **DECL ORPHAN** (`resolve_faq_grounded_answer.yaml:23` lists `moderation_context` in `required_context_keys`; `SkillLoader` validates the *name* but does NOT enforce its emission by the projection builder) | NOT PRESENT (zero grep hits in any projected_context across sampled traces) | **C2 #3 SAFE-TO-RESOLVE.** No consumer reads it anywhere (system_prompt does not mention, EC does not require, ES does not read, DT does not walk, no other Skill declares it, no LLM trace contains it). Two coherent resolutions: **(a) emit it** (requires defining content + a source on `BotSession`; out-of-scope without a product/policy decision); **(b) remove the Skill declaration** (registry-only edit; preserves §1.7 boundary; no LLM-visible change since no LLM has ever seen the slot). **Recommendation: (b) — remove the stale declaration from `resolve_faq_grounded_answer.yaml:23` and update the StateInheritance allowed-key validator if needed.** Awaiting human approval at C1 review. |

## 4. C2 disposition summary (input to deliver-agent + human review)

| Item | Action proposed | Risk | Convergence path | Gate |
|---|---|---|---|---|
| **#3 moderation_context** | Remove the declaration from `resolve_faq_grounded_answer.yaml:23`. | **LOW** (no consumer; LLM has never seen it). | Registry-only edit (`*.yaml` strip + Java test asserting the slot is not emitted and not declared). | Java rendering test PASS + Codex §4.1. Real-LLM rerun is a defense-in-depth confirmation, not required for correctness. |
| **#4 tool_schemas discarded base** | Replace `toolPolicyEnforcer.getVisibleToolsForUc(activeUc)` in `buildProjection(...)` with a Skill-registry-derived equivalent (`skillRegistry.select(phase, activeUc).map(Skill::toolsRequired)`). The OVERWRITE in `build(...)` becomes a no-op (kept as defense-in-depth). | **MEDIUM** (touches the legacy fallback path used by `recordTurn` for non-run-loop traces; behaviour-equivalent only if `Skill.toolsRequired` matches `toolPolicyEnforcer.getVisibleToolsForUc` UC-by-UC; the projection is a sub-band — the dispatcher remains the canonical enforcement). | Registry/Skill-driven; no per-UC if-else. | Java rendering test asserting parity between Skill-derived set and `toolPolicyEnforcer.getVisibleToolsForUc(activeUc)` for each (phase, UC) combination + Codex §4.1 + real-LLM rerun (the legacy `recordTurn` path is rare but exists). |
| **#2 Skill-declared context-key gating** | Make the projection read `PhasePlan.requiredContextKeys()` / `Skill.requiredContextKeys()` to decide which of `form_context` / `customer_context` / `listing_context` / `conversation_history` / `candidate_use_cases` / `accumulated_tool_results` to emit. | **HIGH** (Skill vocabulary ≠ projection vocabulary; gating would silently drop `customer_context` from DISCOVER turns and `listing_context` from non-FAQ phases — LLM-visible semantic change that could regress UC-G/H listing flows or DISCOVER clarification quality). | Would need (a) a vocabulary-reconciliation step (Skill key names → projection slot names), AND (b) explicit per-Skill audit of which slots each Skill genuinely needs vs which it omits by historical accident. | **STOP and surface.** Cannot be done without removing LLM-visible signal availability. The matrix DECL column shows the Skill declarations are themselves under-specified (e.g., DISCOVER's `discover_triage` does not declare `customer_context` — likely an omission, not an intentional exclusion). |
| **#5 state_inheritance.soft_signal_via_projection gating** | Gate `alternate_candidate_use_cases` / `discover_disambiguation_signals` / `prior_use_case_carry` emit by the active Skill's `state_inheritance.soft_signal_via_projection` list. | **MEDIUM-HIGH** (low data risk since signals are usually null/empty for non-declaring Skills; but real-LLM rerun is the only proof). | Registry/Skill-driven via `Skill.stateInheritance().softSignalViaProjection()`. No per-UC if-else. | **STOP and surface.** Convergence is correct-shaped (registry-driven), but the §5.6 evidence gate is a real-LLM rerun on bad cases that exercise DISCOVER→RESOLVE (alice / cs011 / cs012); requires backend + LLM keys, which are not available in the dev sandbox per OQ-S51.1 STOP-surface pattern. **Recommendation:** defer to a §4.3-per-sub-sprint S4 or to a follow-on M5 sub-sprint where the deliver-agent can run the real-LLM rerun at close. |

## 5. Recommended close shape (C1 review input)

Per the sprint contract's "C1 alone is a valid partial close if C2 proves
larger than one sub-sprint (deliver-agent + human decide)" and per the
hard fence "STOP and surface if any in-scope convergence (#2-#5) cannot
be done without removing an LLM-visible signal":

1. **Land in S3 (low-risk, matrix-confirmed safe):**
   - C2 **#3** — strip the stale `moderation_context` declaration from
     `resolve_faq_grounded_answer.yaml:23`.
   - C2 **#4** — replace the UC-driven `tool_schemas` base in
     `buildProjection(...)` with a Skill-registry-derived equivalent,
     conditional on (a) Java parity test PASS and (b) human approval at
     C1 review.

2. **STOP and surface (HIGH risk of LLM-visible semantic loss):**
   - C2 **#2** — Skill-declared context-key gating (the cross-Skill
     `customer_context` / `listing_context` / `conversation_history`
     omissions are not provably intentional; gating without a Skill
     declaration audit + real-LLM rerun would silently drop signals the
     LLM relies on).
   - C2 **#5** — Skill-declared soft-signal gating (low semantic risk in
     theory since signals are null-when-N/A for non-declaring Skills,
     but the §5.6 evidence gate is a real-LLM rerun; defer to S4 or to
     a deliver-agent-led close where backend + LLM keys are available).

3. **C2 #6 (tests + real-LLM bad-case rerun):** Java wiring/rendering
   tests cover the landed-in-S3 changes (#3 + #4 if approved). The
   real-LLM bad-case rerun is the M5-close gate per
   `milestone_objective.md` §5; for S3 close, it is a **deliver-agent
   close-bundle responsibility** (the dev sandbox lacks backend + LLM
   keys; STOP-surface per OQ-S51.1 pattern).

## 6. OQ-S51.2 row (item #1a per sprint contract)

The S2 eyeball surfaced: `BotTurn.projected_context` (the persisted
single-column value) carries a FAQ-grounding overlay that the raw
per-invocation projection (`bot_turn_llm_calls.projection`) does not.
This row maps the canonical-ness of each value across consumers.

| Consumer | Source consumed today | Sees the overlay? | Notes |
|---|---|---|---|
| **What the LLM actually saw** | `bot_turn_llm_calls.projection` (per-step, raw, S2-added) | **NO** | The LLM was invoked with the raw projection BEFORE `mergeFaqGroundingIntoProjection`. |
| **Eval trace contract validator** (`TraceCollector` strict mode) | `/v1/demo/sessions/{id}/trace` → `bot_turns.projected_context` (single column, post-overlay) | **YES** | Reads the overlay-merged value via the trace endpoint. None of the contract-required fields (`current_phase`, conditional `active_use_case`, `containment_outcome`, turn `phase_after`, tool call `tool_name`/`arguments`) live in the overlay — overlay is `faq_grounding` only. So the overlay does NOT affect contract enforcement. |
| **Eval scoring** (`per_turn_trace[].projection` via `_build_per_turn_trace` `executor.py:557`) | `bot_turns.projected_context` (single column, post-overlay) | **YES** | The Tier-2 scorer reads `projection.session.*` / `projection.accumulated_tool_results` / `projection.intake_state` — none of which are in the overlay. The overlay (`projection.faq_grounding`) is read by **no scorer** (zero Python grep hits). So the overlay does NOT affect scoring. |
| **Admin trace UI** (`TraceViewer` per-turn `LlmDetailPanel` `projected_context`) | `bot_turns.projected_context` (single column, post-overlay) | **YES** | UI shows the post-overlay JSON; per-step `LlmInvocationCard.projected_context` (S2-added) shows the raw per-step values. So both views are AVAILABLE in the admin UI — the human can compare them. |
| **`buildDriftAndTaskHistory`** (`:1265-1305`) | Walks prior turns' `BotTurn.projected_context` (post-overlay) | **YES** (but `faq_grounding` is not in its walked key set) | DT reads drift_type/intent_relation/etc. — overlay does NOT affect reconstruction. |
| **`Sprint141FaqGroundingTracePersistenceTest`** | `bot_turns.projected_context.faq_grounding` | **YES (REQUIRED)** | This test directly asserts the overlay is durable. **Load-bearing.** |

**Canonicality summary:**
- For **"what the LLM saw"**: the per-step `bot_turn_llm_calls.projection` is
  the canonical truth (overlay-free).
- For **"what eval scores against"**: the overlay-merged single column is
  canonical (and the overlay does not affect any scorer that exists today).
- For **"what `Sprint141FaqGroundingTracePersistenceTest` asserts"**: the
  overlay-merged single column is canonical and load-bearing.

**Recommendation (S3 disposition — DEFERRED to human at C1 review):** the
overlay is benign and PRE-EXISTING; the divergence between raw and
overlaid projection is now OBSERVABLE thanks to S2 (no longer invisible).
**Do NOT auto-converge in S3.** Three rational dispositions exist:

- **(a) Leave as-is + note** — the divergence is benign; document in
  `10-handoff.md` and admin-trace UI helptext. **Lowest cost.**
- **(b) Move the overlay to the raw per-step projection** — apply
  `mergeFaqGroundingIntoProjection` per-step (would need to compute
  diagnostics per-step, which changes ControlKernel's diagnostic
  computation cadence). **Higher cost; out-of-scope for S3.**
- **(c) Stop merging into `bot_turns.projected_context` and write
  `faq_grounding` to a dedicated column or table** — relieves the
  divergence by isolating the diagnostic, but `Sprint141FaqGroundingTracePersistenceTest`
  + any downstream reader of `projected_context.faq_grounding` would
  need to be updated. **Higher cost; out-of-scope for S3.**

**S3 ships option (a) by default (no overlay change).** Human decides at
C1 review whether to surface this for a future R-item.

## 7. Cross-references

- Sprint contract: `docs/sprint_objective.md` (Sprint 52 / M5 S3) §"Scope #1 / #1a".
- Milestone objective: `docs/milestone_objective.md` §3 (S3 paragraph) + §6 (S3/S4 fences).
- Solution proposal: `docs/solutions/observability_coherence_admin_trace_and_projection.md`
  §2.C (data flow + line anchors), §4.C (C1/C2 design), §8 #3 (fences).
- S2 handoff (OQ-S51.2 origin): `docs/sprints/sprint-051-handoff.md` §12.3.
- Governance: `docs/current/iteration_governance.md` §1.7 (forbidden), §4.1
  (anti-hardcode kernel), §5.6 (real-LLM rerun gate), §7 (stanza).
- Code anchors are HEAD `cf9c120` (sprint-051 close working tree, including
  uncommitted S2 dev work per `10-handoff.md` §0).
