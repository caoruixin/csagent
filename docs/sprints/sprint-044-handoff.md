---
title: Sprint 44 (NEW M3-Eval sub-sprint 3, S-Eval-3) — Populate `critical_steps` for the 6 Skills — DEV HANDOFF
doc_tier: sprint-archive
status: current
implementation_status: implemented
source_of_truth: this file (dev-authored); deliver-agent + human + Codex append §12 closure verdict at close (PER-SUB-SPRINT Codex review per §4.3 trigger #2 BEFORE S-Eval-4 begins)
last_reviewed: 2026-05-21
review_cadence: per sub-sprint
supersedes: []
superseded_by: null
notes: >
  Sprint 44 / S-Eval-3 dev handoff. Populates 18 `critical_steps`
  entries across the 6 production Skill YAMLs (3 + 2 + 5 + 5 + 2 + 1
  per Skill, within the contract §2 envelope 16-22 lower-bound; 18-30
  outer envelope). Each step carries the five contractual fields (`id`,
  `desc`, `trace_check`, `mandatory_for`, `severity`). Every `desc` is
  soft procedural narrative per the proposal §5.3 standard (row 1 ✅
  on all 18); every `trace_check` uses ONLY the six frozen DSL
  primitives from S-Eval-2 — verified by extractor construction +
  individual parse runs.

  The Tier-2 verification recorded in §9 demonstrates the Alice
  UC-H intake-then-handover failure surface emerges as `uc-h-intake-
  complete-before-handover` Tier-2 mandatory FAIL when the trace
  shows `intake_state.fields_collected` empty (the canonical Alice
  shape per `bad_cases/_manifest.md`). Per-cluster negative checks
  confirm UC-A traces do not trigger UC-FP `critical_steps` and
  UC-FP traces do not trigger UC-H intake `critical_steps`
  (`mandatory_for` scoping returns N/A for non-applicable UCs;
  `tier2_results_to_gate` returns PASS / critical with no failed
  step ids).

  This sub-sprint requires **per-sub-sprint Codex review** per
  `iteration_governance.md` §4.3 trigger #2 (LLM-visible content;
  §1.7 forbidden-list adjacent). S-Eval-4 BLOCKED until Codex
  returns `decision: pass` (or `approve with downgrade-to-signal
  follow-up`). The Codex review independently re-walks the §5.3
  standard against every populated `desc` and the §4.1 nine-question
  kernel against the full diff.
---

# Sprint 44 (NEW M3-Eval sub-sprint 3, S-Eval-3) — Dev Handoff

## 1. Sprint identity

- **Sprint number**: 44.
- **Sub-sprint**: S-Eval-3 (third sub-sprint of M3-Eval; LLM-visible
  content authoring per `docs/milestone_objective.md` §3).
- **Milestone**: M3-Eval (Coarse-to-Fine Evaluation Architecture).
- **Layer (per `iteration_governance.md` §3.2)**: `eval_spec` (content
  under the LLM-visible contract that S-Eval-2 wired). §7 stanza
  REQUIRED; filled at `docs/sprint_objective.md` §8 with the proposal
  §5.3 standard table reproduced verbatim.
- **Codex review plan**: **PER-SUB-SPRINT** per §4.3 trigger #2
  (`critical_steps[].desc` is LLM-visible and crosses §1.7 forbidden-
  list territory). Codex must independently verify each populated
  `desc` against the proposal §5.3 standard at S-Eval-3 close,
  BEFORE S-Eval-4 begins. This IS the milestone's critical
  governance gate.
- **HEAD baseline at sub-sprint start**: `db19a47` (`docs: S-Eval-2
  close (A — Clean PASS) + S-Eval-3 launch + v0_2 supersession
  landing`).

## 2. Summary

S-Eval-3 populates 18 LLM-visible `critical_steps` across the 6
production Skill YAMLs. Per-Skill counts: `discover_triage` 3 (DISCOVER
disambiguation + UC-FP route hint, all advisory); `confirm` 2 (CONFIRM
record-outcome + dissatisfaction handling); `resolve_faq_grounded_answer`
5 (search-before-answer mandatory; resolve-after-search-hit advisory;
record-outcome advisory; UC-FP moderation-context mandatory; UC-FP
appeal-reroute advisory); `resolve_intake_collect_and_handover` 5 (per-
UC G/H/I/J/K intake-complete-before-handover mandatory, anchored on
the canonical `IntakeFieldsRegistry` field names); `escalate` 2
(request_handover mandatory; record_outcome-after-handover advisory);
`terminal` 1 (record_outcome mandatory). Every `desc` is row-1 soft
procedural narrative per the proposal §5.3 standard; every
`trace_check` uses only the six frozen DSL primitives from S-Eval-2
(no regex / keyword / message-content matching, structurally
prevented by `parse_trace_check`). The Tier-2 verification in §9
demonstrates Alice's UC-H intake-then-handover failure shape
surfaces as `uc-h-intake-complete-before-handover` Tier-2 mandatory
FAIL (the `intake_complete_required` equivalent the contract §10 #5
named as the expected surface). The dev bundle contains 8 files: 6
Skill YAMLs (content extension only) + 1 Java S-Eval-2 anchor test
updated to the S-Eval-3 anchor + 1 Python S-Eval-2 anchor test
updated to the S-Eval-3 anchor (both classified §7-a contract drift
in §10 — planned S-Eval-2-checkpoint update anticipated by S-Eval-2
author at S-Eval-2 close).

## 3. Files shipped (per-file numstat)

Reproducible via `git show --numstat <commit>` after the bundle
lands. Pre-commit numstat (from `git diff --numstat`):

### Skill YAMLs (6 files; content extension only)

| Path | Change | + | − | Notes |
|---|---|---:|---:|---|
| `server/src/main/resources/skills/discover_triage.yaml` | EDIT | 56 | 0 | Appended `critical_steps:` block with 3 advisory steps (cluster: DISCOVER disambiguation + UC-FP route hint). No other field touched. |
| `server/src/main/resources/skills/confirm.yaml` | EDIT | 39 | 0 | Appended `critical_steps:` block with 2 steps (1 mandatory record-outcome; 1 advisory dissatisfaction-handling). |
| `server/src/main/resources/skills/resolve_faq_grounded_answer.yaml` | EDIT | 92 | 0 | Appended `critical_steps:` block with 5 steps (cluster: UC-A/B/C/D/E/F FAQ-grounded + UC-FP explain-when-actionable). |
| `server/src/main/resources/skills/resolve_intake_collect_and_handover.yaml` | EDIT | 67 | 0 | Appended `critical_steps:` block with 5 per-UC G/H/I/J/K intake-complete-before-handover steps, all mandatory. The Alice anchor (UC-H) lives here. |
| `server/src/main/resources/skills/escalate.yaml` | EDIT | 49 | 0 | Appended `critical_steps:` block with 2 steps (1 mandatory request_handover; 1 advisory record-outcome-after-handover ordering). |
| `server/src/main/resources/skills/terminal.yaml` | EDIT | 24 | 0 | Appended `critical_steps:` block with 1 mandatory record-outcome step. |

**Skill YAML line additions: 327 lines (+) / 0 lines (−).** Every
edit is APPEND-ONLY (the existing `state_inheritance:` block was the
last block in each YAML; `critical_steps:` follows it). No `procedure`
/ `guardrails` / `state_inheritance` / `applicable_use_cases` /
`system_instruction` text touched on any of the 6 Skills.

### Anchor-test updates (2 files; §7-a contract drift)

| Path | Change | + | − | Notes |
|---|---|---:|---:|---|
| `server/src/test/java/com/gumtree/csagent/service/runtime/skill/SkillCriticalStepsLoadingTest.java` | EDIT | 46 | 5 | Renamed S-Eval-2 anchor test `loadAll_productionSkills_haveEmptyCriticalSteps_atSEval2_close` → `loadAll_productionSkills_havePopulatedCriticalSteps_atSEval3_close`; new assertion shape: 6 Skills, per-Skill counts per the contract §2 distribution table, 18 total, each populated step carries all 5 fields cleanly. Justification + drift classification in §10 below. |
| `eval_interactive/tests/test_skill_procedure_extractor.py` | EDIT | 50 | 9 | Same shape on the Python side: renamed `test_load_all_six_production_skills_empty_critical_steps` → `test_load_all_six_production_skills_populated_critical_steps` + introduced `_EXPECTED_PER_SKILL_COUNTS` map; loosened `test_extractor_handles_full_production_skill_set_without_crash` from "results == []" (S-Eval-2 empty default) to "no exception raised + results is a list" (S-Eval-3 populated). Justification + drift classification in §10 below. |

### Handoff (1 file; dev-authored)

- `docs/sprints/sprint-044-handoff.md` (this file) — 12-section
  archive per Sprint 35 / 41 / 42 / 43 precedent.

**Bundle file count**: 8 (6 Skill YAMLs + 2 anchor-test updates + 1
handoff) → 9 with the handoff. Right at the contract §7 estimate of
7-8 plus handoff; matches expectation. **Skill YAML additions
themselves come to 327 lines**, all within the 6 named files; zero
lines deleted from any production Java / Python / YAML source.

### OQ-S43.2 `id` cross-reference usage (preserved per contract §9)

The `id` field was used as a stable per-step identifier within each
Skill; **no `desc` cross-references another step's `id` by name in
this S-Eval-3 batch**. The `id` field still served as the
authoring-side anchor (each populated step's `id` is mentioned in
this handoff's §4 + §5 tables, and Tier-2 fail surfaces in §9 cite
the `id`). Whether the LLM-visible projection benefits from the
`id` in the rendered `[{id, desc}]` payload is an M3-Eval-close
calibration question per S-Eval-2 OQ-S43.2 disposition; recorded as
observation for that round. No step authored required reading
another step's `id` to make sense.

### OQ-S43.5 `session.<flag>_present` reliance observation

**Zero steps in this S-Eval-3 batch use the `session.<flag>_present`
primitive.** This was a deliberate authoring choice: the bare-flag
truthiness double-fallback flagged by S-Eval-2 OQ-S43.5 as
potentially over-permissive does not affect any populated step in
this batch. Every step relies on `accumulated_tool_results.<tool>`
(tool presence), `tool_event_seq(<a>) < tool_event_seq(<b>)` (tool
order), `intake_state.fields_collected.contains(<field>)` (intake-
field presence), or `any_of` / `all_of` combinators over those
three. Recorded as observation; OQ-S43.5 is not surfaced as an
S-Eval-3 fix-iteration candidate.

## 4. Per-Skill `critical_steps` content map

The full content of each Skill's populated `critical_steps:` block
is in the Skill YAML at HEAD; the table below maps each step to its
cluster and Tier-2 surface intent.

### `discover_triage.yaml` (3 advisory)

| `id` | Cluster | `mandatory_for` | `severity` | Tier-2 surface intent |
|---|---|---|---|---|
| `faq-uc-search-before-commit` | DISCOVER disambiguation (FAQ-path) | UC-A / B / C / D / E / F / FP | advisory | When a FAQ-path UC commits, retrieval evidence ideally precedes the commit. Cases that legitimately commit on a sharp form-context signal don't trigger this; the advisory severity records without gating. |
| `removed-listing-route-with-moderation-context` | UC-FP route hint | UC-FP | advisory | When the active UC is UC-FP (correct-deletion explanation), the moderation context should appear before the route is finalised. Advisory because the runtime owns the route-anchor decision; this surfaces the diagnostic. |
| `intake-uc-classified-without-pre-search-noise` | DISCOVER disambiguation (intake-path) | UC-G / H / I / J / K | advisory | The desc is the load-bearing LLM-visible procedural narrative for the intake-path commit pattern; the trace_check confirms classification happened. Advisory; never gate-contributing. |

### `confirm.yaml` (2 steps)

| `id` | Cluster | `mandatory_for` | `severity` | Tier-2 surface intent |
|---|---|---|---|---|
| `record-outcome-on-confirmed-resolve` | DISCOVER disambiguation tail | UC-A / B / C / D / E / F / FP | **mandatory** | A CONFIRM-phase case for an FAQ-path UC must record the outcome; otherwise the session cannot be closed as RESOLVED. Mandatory gate on the canonical post-resolve disposition. |
| `confirm-honours-user-dissatisfaction` | DISCOVER disambiguation tail | UC-A / B / C / D / E / F / FP | advisory | Captures the state-fidelity invariant that the bot must NOT record RESOLVED while the user is unhappy; the trace_check requires either `record_outcome` or `request_handover` as a CONFIRM exit. Advisory because the legitimate exits are heterogeneous. |

### `resolve_faq_grounded_answer.yaml` (5 steps)

| `id` | Cluster | `mandatory_for` | `severity` | Tier-2 surface intent |
|---|---|---|---|---|
| `search-knowledge-before-faq-answer` | FAQ-grounded answer | UC-A / B / C / D / E / F / FP | **mandatory** | FAQ-path retrieval before composing a factual customer-facing reply is the grounding-floor anchor. Mandatory gate. |
| `resolve-article-after-search-hit` | FAQ-grounded answer | UC-A / B / C / D / E / F / FP | advisory | When `search_knowledge` returns a viable hit, `resolve_article` should follow. Advisory because the no-hit-handover path also legitimately skips `resolve_article`. |
| `record-outcome-on-grounded-answer` | FAQ-grounded answer | UC-A / B / C / D / E / F / FP | advisory | Records the resolve disposition; advisory because the no-hit-handover path also legitimately bypasses this step from the FAQ Skill (the case escalates instead). |
| `consult-moderation-context-on-removal-explanation` | UC-FP explain-when-actionable | UC-FP | **mandatory** | UC-FP factual reply about removal reason must be anchored in the moderation context (canonical reason metadata). Mandatory; without it the answer is fabrication-prone. |
| `reroute-on-actionable-appeal-intent` | UC-FP explain-when-actionable | UC-FP | advisory | Captures the soft procedural posture that UC-FP resolution should surface the UC-H appeal-intake path when the user signals they want to act on the removal. Advisory; the trace_check accepts either `record_outcome` (resolved with appeal-route named in the answer) or `request_handover` (escalated with appeal context). |

### `resolve_intake_collect_and_handover.yaml` (5 mandatory per-UC steps)

Each step's `trace_check` enumerates the canonical required-field
list per `IntakeFieldsRegistry.REQUIRED_FIELDS_BY_UC`. The Alice
anchor is the UC-H step.

| `id` | `mandatory_for` | Required fields enumerated in `trace_check` | `severity` |
|---|---|---|---|
| `uc-g-intake-complete-before-handover` | UC-G | `registered_email` + `data_request_type` | **mandatory** |
| `uc-h-intake-complete-before-handover` (Alice anchor) | UC-H | `ad_id_or_listing_url` + `registered_email` + `stated_reason_or_context` | **mandatory** |
| `uc-i-intake-complete-before-handover` | UC-I | `transaction_reference` + `dispute_reason` | **mandatory** |
| `uc-j-intake-complete-before-handover` | UC-J | `report_target` + `report_type` + `description` | **mandatory** |
| `uc-k-intake-complete-before-handover` | UC-K | `platform` + `repro_steps_or_error_message` | **mandatory** |

### `escalate.yaml` (2 steps)

| `id` | `mandatory_for` | `severity` | Tier-2 surface intent |
|---|---|---|---|
| `escalate-via-request-handover` | UC-A / B / C / D / E / F / FP / G / H / I / J / K (all 12) | **mandatory** | An ESCALATE phase case must dispatch `request_handover`. Mandatory gate. |
| `record-outcome-after-handover-on-escalate` | UC-A / B / C / D / E / F / FP / G / H / I / J / K | advisory | Records the canonical post-handover state-of-record + order (handover then record_outcome). Advisory because some legitimate paths defer `record_outcome` to terminal Skill. |

### `terminal.yaml` (1 mandatory step)

| `id` | `mandatory_for` | `severity` | Tier-2 surface intent |
|---|---|---|---|
| `terminal-records-outcome` | UC-A / B / C / D / E / F / FP / G / H / I / J / K | **mandatory** | A terminal-phase case must call `record_outcome`. Mandatory gate. |

**Total: 18 steps across 6 Skills.** Mandatory : Advisory split = 10 : 8.

### Severity rationale

The mandatory : advisory split (10 : 8) was chosen so that **the
most procedurally-load-bearing steps gate Tier-2** (FAQ retrieval,
intake completion, UC-FP moderation context, escalation handover,
terminal outcome) while **steps whose violation could fire on
legitimate alternate paths stay advisory** (resolve-after-search,
record-outcome-on-grounded-answer, DISCOVER disambiguation, CONFIRM
dissatisfaction exit, escalate-then-record-outcome ordering). The
advisory tier still records the diagnostic; only `severity=mandatory`
FAILs flip `case_passed` per the S-Eval-2 `tier2_results_to_gate`
contract.

## 5. §5.3 anti-hardcode self-walk (CRITICAL for Codex per-sub-sprint review)

Self-walk over every populated `desc` against the proposal §5.3
standard table. The four-row standard table:

- **Row 1 ✅** Soft procedural narrative ("For UC-FP issues, consult
  the moderation context before explaining the reason")
- **Row 2 ⚠️** Soft diagnostic signal ("If the user mentions 'appeal'
  or 'review', consider whether this is actually UC-H rather than
  UC-FP") — edge; Codex scrutinises
- **Row 3 ❌** Hard if-else rule ("IF user.message.contains('appeal')
  THEN active_use_case := UC-H")
- **Row 4 ❌** Keyword enumeration matrix ("Trigger words for UC-H:
  [...]")

| Skill | step_id | `desc` snippet | Verdict |
|---|---|---|---|
| discover_triage | faq-uc-search-before-commit | "grounding the `classify_use_case` commitment in prior `search_knowledge` evidence reduces the chance of a hasty commit ... For sharply-shaped intake the LLM may commit without a prior search" | **Row 1 ✅** soft procedural; LLM owns whether to act on the heuristic |
| discover_triage | removed-listing-route-with-moderation-context | "the moderation context is the canonical reason metadata for deciding between a correct-deletion explanation (UC-FP) and an appeal intake (UC-H) ... The LLM owns whether the user's stated need is understanding or acting" | **Row 1 ✅** procedural narrative; explicit LLM-ownership disclaimer |
| discover_triage | intake-uc-classified-without-pre-search-noise | "`classify_use_case` is anchored on the form's topic-subject plus the user's explicit intake / appeal / report signal ... Commit the intake use case promptly so the intake collection can begin" | **Row 1 ✅** soft procedural; no `if user says X then Y` |
| confirm | record-outcome-on-confirmed-resolve | "Once the user confirms the prior answer worked, call `record_outcome` so the case closes with a RESOLVED disposition. Skipping `record_outcome` leaves the resolution unrecorded" | **Row 1 ✅** procedural — tool-name-grounded, not keyword-grounded |
| confirm | confirm-honours-user-dissatisfaction | "If the user's CONFIRM-phase reply expresses dissatisfaction or asks for further help, do not record the outcome as RESOLVED. The healthy exits are either return control to a fresh RESOLVE attempt ... or dispatch `request_handover`" | **Row 1 ✅** soft procedural; describes user-signal posture without enumerating keywords |
| resolve_faq_grounded_answer | search-knowledge-before-faq-answer | "Before composing a factual customer-facing answer ... retrieve the knowledge surface via `search_knowledge`. A factual answer not anchored in retrieved knowledge is a fabrication risk" | **Row 1 ✅** procedural; tool-name-grounded |
| resolve_faq_grounded_answer | resolve-article-after-search-hit | "Once `search_knowledge` returns a viable hit, run `resolve_article` for the chosen hit before answering ... Cases with no viable search hit may legitimately escalate via `request_handover` with `escalation_reason=faq_miss_threshold_exceeded`" | **Row 1 ✅** procedural |
| resolve_faq_grounded_answer | record-outcome-on-grounded-answer | "Once a customer-facing FAQ-grounded answer is delivered, call `record_outcome` to bind the resolution disposition to the case" | **Row 1 ✅** procedural |
| resolve_faq_grounded_answer | consult-moderation-context-on-removal-explanation | "For UC-FP (Correct Deletion Explanation), the moderation context is the canonical reason metadata for an honest explanation ... Without the moderation context, a UC-FP factual reply about the removal reason is a fabrication risk" | **Row 1 ✅** procedural; tool-name-grounded |
| resolve_faq_grounded_answer | reroute-on-actionable-appeal-intent | "If during UC-FP resolution the user's stance shifts from understanding the removal to actively appealing it, the appropriate next move is to surface the UC-H appeal-intake path — either by completing the explanation and naming the appeal route, or by escalating with a useful handover summary. ... The failure mode this step guards against is locking the user into UC-H intake purely on the listing-status signal" | **Row 1 ✅** procedural narrative; describes a user-stance shift in goal-language ("understand vs. actively appealing"), NOT a keyword/regex on user message text — the LLM owns reading the stance from the conversation |
| resolve_intake_collect_and_handover | uc-g-intake-complete-before-handover | "For UC-G (GDPR / data deletion), the canonical intake fields `registered_email` and `data_request_type` must both be collected before `request_handover` ... the `intake_complete_required` guardrail explicitly refuses (it will downgrade the call and hint at the missing fields)" | **Row 1 ✅** procedural; canonical field names (not user-message keywords) — references `IntakeFieldsRegistry` canonical schema |
| resolve_intake_collect_and_handover | uc-h-intake-complete-before-handover (Alice anchor) | "For UC-H (Ad Removal Appeal), the canonical intake fields `ad_id_or_listing_url`, `registered_email`, and `stated_reason_or_context` must all be collected before `request_handover` ... The `intake_complete_required` guardrail refuses an `intake_complete_for_uc_h` handover with missing fields" | **Row 1 ✅** procedural; canonical schema names |
| resolve_intake_collect_and_handover | uc-i-intake-complete-before-handover | "For UC-I (Refund / Payment Dispute), the canonical intake fields `transaction_reference` and `dispute_reason` must both be collected before `request_handover` ..." | **Row 1 ✅** procedural; canonical schema names |
| resolve_intake_collect_and_handover | uc-j-intake-complete-before-handover | "For UC-J (Trust & Safety Report), the canonical intake fields `report_target`, `report_type`, and `description` must all be collected before `request_handover` ..." | **Row 1 ✅** procedural; canonical schema names |
| resolve_intake_collect_and_handover | uc-k-intake-complete-before-handover | "For UC-K (Technical Issue Intake), the canonical intake fields `platform` and `repro_steps_or_error_message` must both be collected before `request_handover` ..." | **Row 1 ✅** procedural; canonical schema names |
| escalate | escalate-via-request-handover | "In the ESCALATE phase, `request_handover` is the tool that completes the escalation, carrying an `escalation_reason` from the canonical enum and the relevant intake context" | **Row 1 ✅** procedural; tool-name-grounded |
| escalate | record-outcome-after-handover-on-escalate | "After `request_handover` is dispatched in ESCALATE, also call `record_outcome` so the session has a final disposition recorded. ... The order matters: record the outcome after the handover" | **Row 1 ✅** procedural; tool-name-grounded |
| terminal | terminal-records-outcome | "In the CLOSE phase, `record_outcome` must be called so the case has a recorded disposition. Without it the runtime cannot finalise the session state" | **Row 1 ✅** procedural; tool-name-grounded |

**Self-walk summary:**

- Row 1 ✅ Soft procedural narrative: **18 / 18**
- Row 2 ⚠️ Soft diagnostic signal: **0 / 18**
- Row 3 ❌ Hard if-else rule: **0 / 18**
- Row 4 ❌ Keyword enumeration matrix: **0 / 18**

**Zero edge-case (row-2) entries; zero rejected (row-3 / row-4)
entries.** Every populated `desc` is row-1 soft procedural narrative
per the proposal §5.3 standard. Codex's per-sub-sprint review can
independently re-walk each entry against the §5.3 table — the
canonical-schema-reference style (tool names + intake-field names
from the runtime's authoritative registries) is the substantive
defence against §1.7 violations on the LLM-visible surface.

## 6. §4.1 anti-hardcode self-walk verdicts

| Q | Verdict | Justification |
|---|---|---|
| Q1 — Semantic hardcode introduced (keyword / regex / if-else / enum / per-UC matrix)? | **pass** | S-Eval-3 ships 18 populated `critical_steps[].desc` strings + 18 matching `trace_check` strings on the 6 Skills. **Two layers of defence cited**: (a) the proposal **§5.3 standard self-walk** on every populated `desc` per §5 above (semantic defence; all 18 desc strings are row-1 ✅); (b) the **S-Eval-2 DSL parser** `parse_trace_check` + `test_skill_procedure_dsl_parser_rejects_hardcodes.py` 15 hardcode-flavoured rejections (structural defence; verified all 18 populated `trace_check` strings parse cleanly through `SkillProcedureExtractor.from_skills(...)` construction without raising `TraceCheckDSLSyntaxError`). Every `desc` is row-1 soft procedural narrative; every `trace_check` is one of the six frozen primitives (`accumulated_tool_results.<tool>`, `tool_event_seq(...) < tool_event_seq(...)`, `intake_state.fields_collected.contains(<field>)`, `any_of(...)`, `all_of(...)`); zero steps in this batch use `session.<flag>_present` per OQ-S43.5 monitoring posture. |
| Q2 — Tier-0 invariant claim? | **pass** | No Tier-0 invariant added. The Tier-2 `skill_procedure_followship` gate (S-Eval-2 ship) gains its first content but remains an eval-side check per S-Eval-2 §6 Q2 disposition, not a Tier-0 runtime invariant per `docs/runtime_freeze_and_risk_policy.md` §1/§2. The new mandatory Tier-2 gate band gates `case_passed` on the eval side; it does NOT add a runtime decision branch. Milestone §6 hard fence #2 preserved. |
| Q3 — Could a soft signal replace a hard branch? | **pass** | The `desc` IS the soft signal — LLM-visible procedural guidance that the LLM owns acting on per §1.3. No hard runtime branch added anywhere: no `if` / `switch` / regex / keyword matrix in Java; no `tool-policy.yaml` change; no `system_prompt.txt` edit; no `RuntimeIntentClassifier` / `DriftDetector` / `UseCaseRouter` / `ClassifyUseCaseTool` change. The eval-side `trace_check` is a structural check (observe the trace; PASS / FAIL / N/A), not a decision branch. |
| Q4 — Eval phrase / trace-specific phrasing / CaseSpec-id encoded? | **pass** | No CaseSpec id / trace-specific phrasing / visible-eval case text appears in any populated `desc` or `trace_check`. Every reference is to a canonical runtime surface: tool names (`search_knowledge`, `resolve_article`, `classify_use_case`, `record_outcome`, `request_handover`, `get_moderation_review_context`) — all from `tool-policy.yaml`'s canonical tool registry — and intake-field names (`ad_id_or_listing_url`, `registered_email`, `stated_reason_or_context`, `data_request_type`, `transaction_reference`, `dispute_reason`, `report_target`, `report_type`, `description`, `platform`, `repro_steps_or_error_message`) — all from `IntakeFieldsRegistry.REQUIRED_FIELDS_BY_UC` canonical schema. No `cs_*` ids, no "Alice" / "Trish" / "Helena" names, no smoke / anchor case fixture references. |
| Q5 — LLM ownership shrunk (§1.3 surfaces moved to Java)? | **pass** | The LLM's per-turn projection GAINS 18 soft procedural narratives (rendered as `[{id, desc}]` under `phase_plan.critical_steps` per S-Eval-2 wiring). The LLM still owns user goal, issue relation, UC hypothesis, drift / topic shift, next action, escalation posture, response strategy, customer-facing wording (§1.3 verbatim). The eval-side Tier-2 check observes the bot's behavioural trace; it does NOT make any LLM decisions. Multiple steps explicitly cede ownership in the `desc` (e.g., `removed-listing-route-with-moderation-context`: "The LLM owns whether the user's stated need is understanding or acting on the removal"; `reroute-on-actionable-appeal-intent`: "The failure mode this step guards against is locking the user into UC-H intake purely on the listing-status signal" — guidance not branch). |
| Q6 — Prompt if-else added? | **pass** | Every populated `desc` is principle-level / procedural guidance per the §5.3 standard. Zero entries use "IF user.message.contains(...)" or equivalent. No `system_prompt.txt` edit; no Skill `procedure` text edit on any of the 6 Skills (the contract §6 hard fence #1 explicitly preserves `procedure` text). |
| Q7 — Tool schema / capability / PII / grounding floor preserved? | **pass** | No tool schema change; no capability / permission change; no PII handling change. The FAQ grounding contract (`faq_grounding_contract.md`) is preserved verbatim — in fact several `critical_steps[].desc` strings reinforce the grounding-floor anchor (e.g., `search-knowledge-before-faq-answer` desc references "the FAQ grounding contract"; `consult-moderation-context-on-removal-explanation` desc references the moderation-reason grounding surface). Tier-0 safety floor (`no_pii_leakage`, `no_critical_policy_violation`, `escalation_compliance`, `phase_transition_validity`, `no_human_only_tool_exposure`) remains critical-severity unchanged. |
| Q8 — Generalization coverage (target / neighbor / negative / shadow)? | **pass** | Per §8 below. Target: 18 populated `critical_steps` across the 6 Skills walked individually against §5.3 (all row 1 ✅). Neighbor: 14 smoke + 159 anchor + 12 anchor_outcome + 12 case-family + 1 Alice bad case all load cleanly under the populated YAMLs (verified by Python case-loader output in §9). Negative: per-cluster (UC-A → no UC-FP step trigger; UC-FP → no UC-H intake step trigger) verified in §9 Tier-2 verification table; runtime semantic surfaces untouched (M2 §6 #5 hard fence intact). Shadow: 3-5 cases held out of dev visibility per `eval_interactive/case_specs_shadow/_ACCESS_BOUNDARY.md` convention; S-Eval-3 dev did NOT read shadow — those exercise at M3-Eval close. |
| Q9 — Rollback / sunset plan if temporary? | **N/A** | S-Eval-3 changes are intended permanent. The 18 populated `critical_steps[].desc` strings ARE the soft-signal form that replaces existing scattered teaching paragraphs in `system_prompt.txt` (per proposal §5.2 single-source-of-truth claim) and the implicit reliance on tool whitelisting. Not a temporary workaround; the proposal §5 design freeze is the long-term posture. |

No `concern` / `fail` verdict on any of Q1-Q9; no STOP signal (§10
#1-#9) fired during implementation.

## 7. Open questions for deliver-agent + human + Codex at close

- **OQ-S44.1** (executor wiring of Tier-2 gate): the
  `SkillProcedureExtractor` is constructed and `tier2_results_to_gate`
  is reduced from the per-step results by `composite.py`, but the
  executor at `eval_interactive/eval_interactive/batch/executor.py:252`
  does NOT pass `tier2_result=` to `compute_composite(...)`. The
  `Tier2Result` default is empty-list / PASS / advisory, so the
  Tier-2 gate has no effect on `case_passed` in actual eval-harness
  runs at S-Eval-3 close — the populated content is structurally
  inert from the harness's perspective until executor wiring is
  added. **Surfaces as an OQ for S-Eval-4 / M3-Eval close**: the
  populated content's Tier-2 effect is provable offline (per §9
  verification) but is not exercised by the production eval harness
  end-to-end. Deliver-agent + human decide at M3-Eval close whether
  the executor wiring is in scope for S-Eval-4, deferred to S-Eval-5,
  or surfaces as a follow-on R-item for a post-M3-Eval milestone.
- **OQ-S44.2** (`active_skill` / `active_use_case` resolution at the
  extractor call site): even when the executor wiring lands, the
  caller must pick which Skill + UC to evaluate the trace against.
  Alice's case shape is the load-bearing example: the bot stamped
  UC-H but the expected primary_uc is UC-A. Should the extractor
  run against (UC-A, resolve_faq_grounded_answer) per expected
  behaviour, against (UC-H, resolve_intake_collect_and_handover)
  per actual behaviour, or BOTH? The §9 verification ran both and
  found both surface failures (FAQ-side: `search-knowledge-before-
  faq-answer` mandatory fail; intake-side: `uc-h-intake-complete-
  before-handover` mandatory fail). Codex per-sub-sprint review +
  deliver-agent decide the resolution semantics at S-Eval-3 / M3-Eval
  close.
- **OQ-S44.3** (UC-FP `accumulated_tool_results.get_moderation_review_context`
  pre-condition): the `consult-moderation-context-on-removal-
  explanation` step on `resolve_faq_grounded_answer` is mandatory on
  UC-FP. For UC-FP cases where the moderation context is unavailable
  (e.g., listing not in moderation queue or context lookup
  unsuccessful), this step would FAIL even if the bot did its best
  with what it had. Codex independently verifies whether this is
  the right semantic; if too tight, deliver-agent + human consider
  downgrading to advisory at fix-iteration. The §9 synthetic
  verification used a healthy UC-FP trace (moderation context
  available); a real-LLM UC-FP run might surface edge cases here.
- **OQ-S44.4** (mandatory : advisory split rationale): 10 of 18
  steps are mandatory (FAQ retrieval / per-UC intake completion /
  UC-FP moderation context / escalation handover / terminal
  outcome); 8 are advisory. The mandatory steps gate `case_passed`
  on the eval side; advisory steps record diagnostics only. Codex
  independently verifies the split is calibrated — too many
  mandatory steps risk false-positive Tier-2 fails on legitimate
  alternate paths; too few defeats the purpose of Tier-2 gating.
- **OQ-S44.5** (Tier-2 verification reliance on synthetic traces):
  the §9 verification used deterministic synthetic trace fixtures
  per `feedback_mocked_llm_cannot_prove_prompt_causal_change.md`
  spirit — the populated `critical_steps` were exercised against
  hand-constructed traces, not against real-LLM eval-harness runs.
  This is the conservative S-Eval-3 evidence form (avoids the
  §5.5 LLM-provider-drift confound + the OQ-S44.1 executor-wiring
  gap). Codex + deliver-agent + human consider whether the synthetic
  verification is sufficient for S-Eval-3 close or whether a
  real-LLM run is needed before M3-Eval close.

## 8. Generalization coverage (per §8 stanza)

- **Target**: 18 populated `critical_steps[].desc` strings + 18
  matching `trace_check` strings across the 6 Skills (per the §2
  per-Skill anticipated distribution table — 3 / 2 / 5 / 5 / 2 / 1).
  Each `desc` walked against the §5.3 standard table (§5 self-walk,
  all 18 row-1 ✅); each `trace_check` parsed cleanly through the
  S-Eval-2 DSL parser at `SkillProcedureExtractor.from_skills(...)`
  construction.
- **Neighbor**: Alice bad case + 12 anchor_outcome cases (1 per
  canonical UC including 1 synthetic UC-G) + 14 smoke + 12 case-
  family directories all LOAD without modification (verified by
  the Python case-loader test suite that runs as part of `uv run
  pytest`; 392 passed at S-Eval-3 close matches S-Eval-2 baseline).
  Java SkillLoader accepts the populated YAMLs at Spring bootstrap
  per the updated S-Eval-3-anchor test
  `loadAll_productionSkills_havePopulatedCriticalSteps_atSEval3_close`.
- **Negative**: per-cluster scoping verified in §9 Tier-2
  verification — UC-A traces on `resolve_faq_grounded_answer` do
  NOT trigger the UC-FP mandatory step `consult-moderation-context-
  on-removal-explanation` (returns N/A; gate severity=critical /
  passed=True / failed_step_ids=[]); UC-FP traces on
  `resolve_intake_collect_and_handover` do NOT trigger any of the
  per-UC G/H/I/J/K mandatory intake-complete steps (all return N/A;
  gate severity=critical / passed=True / failed_step_ids=[]). The
  `mandatory_for` scoping is the structural defence: a step whose
  `mandatory_for` does not contain `active_use_case` is N/A and
  cannot fail. Verified deterministically without an LLM provider.
- **Shadow**: Cases held out of dev visibility per
  `eval_interactive/case_specs_shadow/_ACCESS_BOUNDARY.md` —
  S-Eval-3 dev did NOT read shadow CaseSpecs at any point during
  authoring or verification. Shadow exercise happens at M3-Eval
  close per `docs/milestone_objective.md` §5 acceptance bar.

## 9. Validation runs

### Java test suite (`mvn test`)

```
$ mvn test 2>&1 | grep "Tests run" | tail -1
[ERROR] Tests run: 1163, Failures: 1, Errors: 0, Skipped: 2
```

**1163 total / 1 inherited failure / 0 errors / 2 skipped** —
exactly matches the S-Eval-2 close baseline (`1163 / 1-inherited /
0 / 2` per S-Eval-2 §12.2).

- **+0 new tests**: S-Eval-3 updated 1 existing S-Eval-2 anchor test
  (`SkillCriticalStepsLoadingTest.loadAll_productionSkills_haveEmptyCriticalSteps_atSEval2_close`
  → `loadAll_productionSkills_havePopulatedCriticalSteps_atSEval3_close`)
  in place per the planned S-Eval-2-to-S-Eval-3 transition (§10
  drift). No new test files added.
- **Inherited failure unchanged**: `SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly:53`
  persists per M2-close STATUS QUO (Sprint 24-era working-tree mod;
  documented continuously through S-Eval-1 §9, Sprint 41 handoff,
  and S-Eval-2 §9). No new failures introduced by S-Eval-3.

### Python test suite (`uv run pytest`)

```
$ cd eval_interactive && uv run pytest 2>&1 | tail -1
======================== 9 failed, 392 passed in 11.85s ========================
```

**392 passed / 9 pre-existing failed** — exactly matches the
S-Eval-2 close baseline (`392 passed / 9 pre-existing failed` per
S-Eval-2 §12.2).

- **+0 new tests, 0 new failures**: S-Eval-3 updated 1 existing
  S-Eval-2 anchor test class (`TestProductionSkillLoad` —
  `test_load_all_six_production_skills_empty_critical_steps` →
  `test_load_all_six_production_skills_populated_critical_steps`
  + loosened `test_extractor_handles_full_production_skill_set_without_crash`
  from `results == []` assertion to "no exception raised + results
  is a list" per the planned S-Eval-2-to-S-Eval-3 transition (§10
  drift). No new test files added.
- **9 pre-existing failures unchanged** (identical to S-Eval-2 §9
  baseline): 2 in `test_escalation_enum_sync.py`
  (deleted `customer_service_tool_spec_v0_2.yaml` reference;
  S-Eval-1 surfaced `R-stale-test-escalation-enum-sync-v0_2-to-v0_3-migration`);
  5 in `test_corpus_lint.py` (cross-repo PYTHONPATH leak); 2 in
  `test_case_spec_overrides.py` (17-vs-15 override count drift).
  S-Eval-3 introduces zero new test failures.

### Tier-2 verification (deterministic offline; §9 acceptance bar)

Per §10 #5 STOP discipline and OQ-S44.5 design rationale, the
Tier-2 verification at S-Eval-3 close is **deterministic
synthetic-trace exercise** rather than an end-to-end eval-harness
run. The synthetic verification (a) avoids the §5.5 LLM-provider-
drift confound, (b) avoids the OQ-S44.1 executor-wiring gap (the
production harness doesn't yet pass `tier2_result` to
`compute_composite`), and (c) is reproducible without provider
credits.

**Verification harness**:

```python
from eval_interactive.scoring.skill_procedure_check import (
    SkillProcedureExtractor, load_skills_from_dir, tier2_results_to_gate,
)
skills = load_skills_from_dir('server/src/main/resources/skills')
ext = SkillProcedureExtractor.from_skills(skills)
# Synthetic trace constructor: tool sequence + intake fields + session flags
# → per-turn projection in the canonical eval-harness shape
```

**Per-scenario Tier-2 verification table**:

| Scenario | active_skill | active_uc | Synthetic trace shape | Tier-2 gate | Failed step ids | Maps to |
|---|---|---|---|---|---|---|
| **Alice failure shape** (UC-H, fields empty) | `resolve_intake_collect_and_handover` | UC-H | tools: classify_use_case → request_handover; intake_fields_collected: [] | **FAIL / critical** | `['uc-h-intake-complete-before-handover']` | `bad_cases/_manifest.md` Alice row (`intake_complete_required` Tier-2 fail expected on UC-H per contract §10 #5) ✓ |
| Alice happy path (UC-A FAQ-grounded) | `resolve_faq_grounded_answer` | UC-A | tools: classify_use_case → get_customer_context → search_knowledge → resolve_article → record_outcome | **PASS / critical** | [] | Alice's closure_criterion (a) end-state — bot routes UC-A FAQ-path; all FAQ-cluster steps PASS |
| UC-FP no moderation context | `resolve_faq_grounded_answer` | UC-FP | tools: classify_use_case → search_knowledge → resolve_article → record_outcome | **FAIL / critical** | `['consult-moderation-context-on-removal-explanation']` | UC-FP route-anchor failure mode (per `discover_disambiguation_signals` projection-slot guidance) |
| UC-FP healthy | `resolve_faq_grounded_answer` | UC-FP | tools: classify → get_moderation_review_context → search_knowledge → resolve_article → record_outcome | **PASS / critical** | [] | UC-FP canonical resolve path (anchor_outcome_uc_fp_removed.yaml shape) |
| **Negative**: UC-A on resolve_faq | `resolve_faq_grounded_answer` | UC-A | tools: classify → get_customer_context → search_knowledge → resolve_article → record_outcome | **PASS / critical** | [] | UC-FP-scoped mandatory step (`consult-moderation-context-...`) returns N/A for UC-A (mandatory_for scoping); UC-FP advisory step (`reroute-on-actionable-appeal-intent`) returns N/A. Per-cluster negative confirmed ✓ |
| **Negative**: UC-FP on intake skill | `resolve_intake_collect_and_handover` | UC-FP | tools: classify → search → resolve → record_outcome | **PASS / critical** | [] | All 5 UC-G/H/I/J/K mandatory intake-complete steps return N/A (mandatory_for scoping). Per-cluster negative confirmed ✓ |
| UC-H healthy intake | `resolve_intake_collect_and_handover` | UC-H | intake_fields: [ad_id_or_listing_url, registered_email, stated_reason_or_context]; request_handover | **PASS / critical** | [] | `anchor_outcome_uc_h_appeal.yaml` canonical shape — fields prefilled from form_context + user message |
| UC-G missing data_request_type | `resolve_intake_collect_and_handover` | UC-G | intake_fields: [registered_email]; request_handover | **FAIL / critical** | `['uc-g-intake-complete-before-handover']` | Partial-intake failure shape; runtime `intake_complete_required` guardrail would also refuse this |
| UC-K healthy intake | `resolve_intake_collect_and_handover` | UC-K | intake_fields: [platform, repro_steps_or_error_message]; request_handover | **PASS / critical** | [] | `anchor_outcome_uc_k_tech.yaml` canonical shape |
| Terminal missing record_outcome | `terminal` | UC-A | tools: classify_use_case only | **FAIL / critical** | `['terminal-records-outcome']` | Terminal-phase incomplete-disposition failure mode |
| Escalate missing request_handover | `escalate` | UC-J | tools: classify → record_outcome | **FAIL / critical** | `['escalate-via-request-handover']` | ESCALATE-phase missing-handover failure mode (handover-completeness contract surface) |
| Confirm happy resolved | `confirm` | UC-A | tools: search → resolve → record_outcome | **PASS / critical** | [] | Canonical CONFIRM RESOLVED close shape |
| Confirm no exit signal | `confirm` | UC-A | tools: search → resolve (no record_outcome / no request_handover) | **FAIL / critical** | `['record-outcome-on-confirmed-resolve']` | CONFIRM-without-exit failure mode |

**13 scenarios; 7 FAIL surfaces all map to expected failure modes;
6 PASS surfaces all map to canonical healthy shapes. The Alice
anchor surfaces as `uc-h-intake-complete-before-handover` Tier-2
mandatory FAIL per the contract §10 #5 expected outcome ✓.**

### Per-cluster negative check (per contract §9 acceptance bar)

| Negative check | active_skill | active_uc | Result |
|---|---|---|---|
| UC-A case must NOT trigger UC-FP mandatory step | `resolve_faq_grounded_answer` | UC-A | ✓ UC-FP step `consult-moderation-context-on-removal-explanation` returns N/A; UC-FP step `reroute-on-actionable-appeal-intent` returns N/A. Gate passed=True severity=critical failed_ids=[]. |
| UC-FP case must NOT trigger UC-H intake mandatory step | `resolve_intake_collect_and_handover` | UC-FP | ✓ All 5 UC-G/H/I/J/K mandatory steps return N/A. Gate passed=True severity=critical failed_ids=[]. |

Per-cluster negative confirmed via `mandatory_for` UC-list scoping.

### Backward-compat fixture load (per contract §9 self-check #8)

```
$ uv run python -c "from eval_interactive.scoring.skill_procedure_check import load_skills_from_dir; ..."
loaded 6 Skills
  confirm: critical_steps=2 applicable_use_cases=['*']
  discover_triage: critical_steps=3 applicable_use_cases=['*']
  escalate: critical_steps=2 applicable_use_cases=['*']
  resolve_faq_grounded_answer: critical_steps=5 applicable_use_cases=['UC-A', 'UC-B', 'UC-C', 'UC-D', 'UC-E', 'UC-F', 'UC-FP']
  resolve_intake_collect_and_handover: critical_steps=5 applicable_use_cases=['UC-G', 'UC-H', 'UC-I', 'UC-J', 'UC-K']
  terminal: critical_steps=1 applicable_use_cases=['*']
TOTAL critical_steps: 18
extractor built; all trace_checks parsed cleanly across 6 Skills
```

All 18 `trace_check` strings parse cleanly through the S-Eval-2 DSL
parser; `SkillProcedureExtractor.from_skills(...)` construction does
NOT raise `TraceCheckDSLSyntaxError`. The 6 Skills load via the Java
`SkillLoader` allowlist validation (verified by the updated
S-Eval-3-anchor test at `mvn test` baseline of `1163 / 1-inherited
/ 0 / 2`).

The 14 smoke + 159 anchor + 12 anchor_outcome + 12 case-family + 1
Alice case fixtures load unchanged under the populated YAMLs
(verified indirectly via the full `uv run pytest` run at S-Eval-3
baseline of 392 passed / 9 pre-existing failed; the loader tests
under `tests/case_spec/` + the regression tests under
`tests/regression/` exercise the loader path).

### Projection token-cost observation

Per-Skill `critical_steps[]` rendering payload (JSON `[{id, desc}]`
list-of-objects per S-Eval-2 outcome 3):

| Skill | steps | bytes | approx tokens |
|---|---:|---:|---:|
| confirm | 2 | 915 | ~229 |
| discover_triage | 3 | 1851 | ~463 |
| escalate | 2 | 945 | ~236 |
| resolve_faq_grounded_answer | 5 | 2922 | ~730 |
| resolve_intake_collect_and_handover | 5 | 2411 | ~603 |
| terminal | 1 | 369 | ~92 |
| **All combined (not per-turn)** | **18** | **9413** | **~2353** |

**Per-turn projection delta** (ONE Skill rendered per turn, the
active Skill):

- Best case (terminal): +369 bytes / ~92 tokens
- Worst case (resolve_faq_grounded_answer): +2922 bytes / ~730 tokens
- Mean of mandatory active Skills (DISCOVER → RESOLVE → CONFIRM →
  CLOSE chain): roughly (1851 + 2922 + 915 + 369) / 4 ≈ +1514 bytes
  / ~378 tokens per active Skill turn

**Well within the proposal §5.4 estimate of 500-900 tokens and the
milestone §5 acceptance bar of ≤ 1000 tokens per turn.** No §10 #9
STOP signal fired. Recorded as S-Eval-3 baseline; M3-Eval close
re-measures on a real-LLM trace to confirm the synthetic estimate
matches actual rendered byte counts.

### S-Eval-2 §1.7 structural defence test (CRITICAL hard gate)

```
$ uv run pytest tests/test_skill_procedure_dsl_parser_rejects_hardcodes.py -v
...
========================= 36 passed in 0.06s =========================
```

All 36 tests pass: 15 hardcode-flavoured `trace_check` strings still
rejected at parse time (≥6 contract floor exceeded by 2.5× per
S-Eval-2 ship); 11 canonical-form positive-grammar tests pass; 10
malformed-syntax tests raise `TraceCheckDSLSyntaxError`. **The
S-Eval-2 structural defence remains the structural mechanism that
protects S-Eval-3's authoring surface from §1.7 violations**, and
remains green at S-Eval-3 close.

## 10. Contract drift

Two **§7-a (deliberate, planned)** drifts logged; neither requires
deliver-agent action — both are S-Eval-2-checkpoint test updates
explicitly anticipated by the S-Eval-2 author at S-Eval-2 close.

- **§7-a #1 (Java)**: `SkillCriticalStepsLoadingTest.loadAll_productionSkills_haveEmptyCriticalSteps_atSEval2_close`
  renamed to `..._havePopulatedCriticalSteps_atSEval3_close` with a
  re-shaped assertion (6 Skills × per-Skill count map ×
  totals=18 × per-step field-present checks). The original test
  name's `_atSEval2_close` suffix is the explicit S-Eval-2 author
  signal that the assertion is a checkpoint to be retired at
  S-Eval-3 close; this drift is the planned retirement. **Drift
  scope**: 1 test method rename + assertion body rewrite within a
  single existing test file (`SkillCriticalStepsLoadingTest.java`,
  46 lines +, 5 lines −). No production Java source touched.

- **§7-a #2 (Python)**: `tests/test_skill_procedure_extractor.py::TestProductionSkillLoad`
  class updated on the same pattern — `test_load_all_six_production_skills_empty_critical_steps`
  renamed to `..._populated_critical_steps` with an
  `_EXPECTED_PER_SKILL_COUNTS` map and an 18-total assertion;
  `test_extractor_handles_full_production_skill_set_without_crash`
  loosened from "results == []" (S-Eval-2 empty default) to "no
  exception + results is a list" (S-Eval-3 populated). **Drift
  scope**: 2 test methods updated within a single existing test
  file (`test_skill_procedure_extractor.py`, 50 lines +, 9 lines −).
  No production Python source touched.

Justification for accepting both drifts under §5's "optional minor
fixes" allowance: (a) the S-Eval-2 anchor tests' names explicitly
carry the `_atSEval2_close` / `empty_critical_steps` suffixes,
flagging them as transition checkpoints; (b) §9's success metrics
budget "0-5 new Java tests / 0-15 new Python tests" — these updates
are within budget (net 0 new tests; 2 test methods updated in place);
(c) without these updates, the S-Eval-3 baseline would show net +2
new failures (1 Java + 1 Python, both the now-incorrect anchor
assertions), violating §9's "no new Java/Python failures
introduced" hard gate; (d) the §6 hard fences enumerate production
source (`Skill.java`, `CriticalStep.java`, `SkillLoader.java`, etc.)
without naming test files — the fences protect S-Eval-2's shipped
semantics, not its checkpoint tests.

No other drift. No §10 STOP signal (#1-#9) fired during
implementation. No file under §6 "Files NOT in scope" was touched
(all 6 Skill YAMLs received `critical_steps:` appendix only; zero
edits to `procedure` / `guardrails` / `state_inheritance` /
`applicable_use_cases` / `system_instruction` text on any of the 6
Skills; zero edits to runtime semantic surfaces; zero edits to case
fixtures; zero edits to governance docs or sprint / milestone
archives).

## 11. Bundle policy honored

Dev ships **ONE bundle commit** containing only the files listed in
§3 (6 Skill YAMLs + 2 anchor-test updates + 1 handoff = 9 files).

**Dev did NOT stage** any deliver-agent territory file per
`feedback_commit_at_end_bundles_deliver_artefacts.md` and contract
§7:

- `docs/sprint_objective.md` (live S-Eval-3 contract; deliver-agent
  + human archive at close to `docs/sprints/sprint-044-objective.md`).
- `docs/milestone_objective.md` (live M3-Eval; deliver-agent
  territory).
- `docs/10-handoff.md` §1 lead refresh (deliver-agent territory).
- `docs/codex-findings.md` (review-agent territory; Codex
  per-sub-sprint review WRITES here at S-Eval-3 close).
- `docs/action_bank.md` (Sprint 44 close-action index row append
  optional at deliver-agent + human close-out bundle; dev did NOT
  append at commit time per minimum-touch posture).
- `compact/sprint-044-dev-prompt.md` (deliver-agent territory).
- `compact/sprint-044-review-prompt.md` (deliver-agent territory;
  drafted by deliver-agent BEFORE Codex dispatch).

Suggested commit message:

```
sprint 44 / S-Eval-3: populate critical_steps for the 6 Skills (NEW M3-Eval sub-sprint 3)
```

## 12. Closure verdict

**Classification: A — Clean PASS** (deliver-agent + human, 2026-05-22).

- **§12.1 Verdict**: dev commit `01770ac` delivered the S-Eval-3 contract end-to-end. 18 `critical_steps` populated across the 6 production Skill YAMLs (3 + 2 + 5 + 5 + 2 + 1) within the §2 envelope (16-22 lower-bound; 18-30 outer envelope). Every `desc` is row-1 ✅ soft procedural narrative per the proposal §5.3 standard (Codex independent §5.3 re-walk returned 18× row-1 / 0× row-2 / 0× row-3 / 0× row-4; deliver-agent has NOT independently spot-checked — the row-1 verdicts are Codex's primary semantic defence). Every `trace_check` parses through the S-Eval-2 6-primitive DSL parser (zero `TraceCheckDSLSyntaxError`); none resembles any of the 15 hardcode-flavoured patterns rejected by `test_skill_procedure_dsl_parser_rejects_hardcodes.py:36` (structural defence layered with semantic defence). No §6 hard fence touched (all M2 + S-Eval-2 Java / Python surfaces UNCHANGED via empty `git diff db19a47..01770ac`; runtime semantic surfaces UNCHANGED; case fixtures UNCHANGED; governance / foundational / milestone archives UNCHANGED); no §10 stop signal fired; §4.1 nine-question self-walk passes Q1-Q8 with Q9 N/A (intended permanent); §7 stanza honoured.

- **§12.2 Commit range + test counts** (every number reproducible per `feedback_deliver_agent_cited_numbers_must_be_reproducible.md`):
  - **Single dev commit `01770ac`** (9 files in bundle; `git show --numstat 01770ac` reports `1080 insertions(+), 14 deletions(-)`).
  - **Skill YAML additions**: `56 + 39 + 92 + 67 + 49 + 24 = 327` lines and 0 deletions across the 6 YAMLs (`git show --numstat 01770ac -- server/src/main/resources/skills/`).
  - **Per-Skill `critical_steps` count**: `discover_triage = 3`, `confirm = 2`, `resolve_faq_grounded_answer = 5`, `resolve_intake_collect_and_handover = 5`, `escalate = 2`, `terminal = 1`. **Total = 18.** Independently reproduced by Codex via YAML parse / `grep -c "^  - id:"`.
  - **Mandatory : advisory split**: 10 mandatory + 8 advisory (`grep -c "severity: mandatory"` / `severity: advisory` across the 6 YAMLs). Independently reproduced by Codex.
  - **Java baseline**: `Tests run: 1163, Failures: 1, Errors: 0, Skipped: 2` (UNCHANGED from S-Eval-2 close baseline; S-Eval-3 added 0 Java production source; 14 new `SkillCriticalStepsLoadingTest` checkpoint-test reshaped assertions per OQ-S43.2 / §10 §7-a planned drift — net delta 0 tests counted since the test class re-uses the same checkpoint slot; inherited `SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly:53` persists unchanged per M2-close STATUS QUO).
  - **Python baseline**: dev handoff §9 cited `392 passed / 9 pre-existing failed` via `uv run pytest`. **Codex independent reproduction returned `5 failed / 396 passed` via `uv run python -m pytest`** (project-interpreter invocation; `uv run pytest` failed under Codex's local checkout because `.venv/bin/pytest` shebang points at a different checkout, exiting 139 under `set -o pipefail`). The 5 failure CLASSES Codex observed are a subset of the 9 dev cited (2 enum-sync + 2 override-count/pipeline + 1 corpus-lint aggregate); no S-Eval-3-specific failure appears under either runner. **Surfaced as OQ-S44.6 (NEW) below**; non-blocking for S-Eval-3 close because both reproductions confirm no S-Eval-3-attributable regression, but the discrepancy is the second observed `feedback_deliver_agent_cited_numbers_must_be_reproducible.md` lapse at the per-sub-sprint dev-handoff level (first M3-Eval instance; first M2-era instance was Codex's Sprint 41 / Sprint 40 review-prompt drift).
  - **§1.7 structural defence test**: `36 passed` via `uv run python -m pytest tests/test_skill_procedure_dsl_parser_rejects_hardcodes.py -v` (15 hardcode-flavoured rejections + 11 positive-grammar + 10 malformed-syntax). UNCHANGED since S-Eval-2 ship.
  - **Skill loader sanity**: `load_skills_from_dir('../server/src/main/resources/skills')` returns 6 Skills + total `critical_steps = 18`; extractor construction parses all 18 `trace_check` strings cleanly. Independently reproduced by Codex per `docs/codex-findings.md` "Validation Runs" / "Skill loader sanity".
  - **Tier-2 deterministic synthetic verification**: dev §9 ran 13 scenarios with the §5.3-aligned expected-shape table (7 FAIL surfaces all map to expected failure modes; 6 PASS surfaces all map to canonical healthy shapes). Codex independently re-ran spot-checks: **Alice UC-H empty-fields trace** fails `uc-h-intake-complete-before-handover` mandatory ✓; **Alice-as-UC-A FAQ trace** fails `search-knowledge-before-faq-answer` mandatory ✓; **UC-FP no-moderation trace** fails `consult-moderation-context-on-removal-explanation` mandatory ✓; **UC-A / UC-FP negative scoping traces** pass with non-applicable steps excluded via `mandatory_for` ✓.
  - **Projection token-cost observation**: dev §9 per-active-Skill turn delta: best 92 / worst 730 / mean ~378 tokens. Codex independently confirmed direction (worst `resolve_faq_grounded_answer` ~728 tokens; `resolve_intake_collect_and_handover` ~603 tokens; DISCOVER→RESOLVE→CONFIRM→CLOSE mean ~377 tokens/turn). Well within proposal §5.4 envelope (500-900) and milestone §5 acceptance bar (≤ 1000 tokens/turn).
  - Backward-compat: 6 Skill YAMLs + 14 smoke + 159 anchor + 12 anchor_outcome + 12 case-family + 1 Alice all load and run unchanged through the populated `critical_steps`.

- **§12.3 Codex verdict (per-sub-sprint per §4.3 trigger #2)**: **`decision: pass / blocking_count: 0`** on first pass, single round. Codex independent §5.3 re-walk returned 18× row-1 ✅ / 0× row-2 / 0× row-3 / 0× row-4 (all 18 entries soft procedural narrative; none encode keyword/regex/message-content if-else; none encode keyword enumeration matrix). Codex §4.1 nine-question kernel: Q1-Q8 pass with Q9 N/A. Codex §1.7 boundary check (a-e): all pass. Codex hard-fence verification: all empty-diff confirmed. Codex Tier-0 candidate independent verification: no new Tier-0 candidate; `runtime_freeze_and_risk_policy.md` §1/§2 UNCHANGED in the commit range. Codex contract-drift independent verification: both §7-a planned drifts accepted as planned-not-drift (S-Eval-2-checkpoint-retirement pattern). Codex archive will be at `docs/sprints/sprint-044-codex-review.md` (delete-and-add supersession of live `docs/codex-findings.md` per `feedback_packaging_codex_findings_supersession.md`). **S-Eval-4 UNBLOCKS.**

- **§12.4 Open questions disposition** (6 OQs total; 5 dev-surfaced + 1 NEW deliver-agent-surfaced):
  - **OQ-S44.1 (executor wiring gap — LOAD-BEARING)**: dev §7 surfaces that `eval_interactive/eval_interactive/batch/executor.py:252` does NOT pass `tier2_result=` to `compute_composite(...)`; populated content is structurally inert in the production eval-harness path. **Codex verdict: acceptable as-is for S-Eval-3 with carry-over** ("the gap is real, but executor wiring is outside S-Eval-3 §5/§6 and the content review can close on synthetic Tier-2 evidence"). **Disposition: `out_of_scope_review` per `feedback_out_of_scope_review_packaging_rollforward.md`** — routed to S-Eval-4 / S-Eval-5 planning as carry-over with explicit packaging note (S-Eval-4 trial milestone-close dry-run may surface this as a real-LLM blocker; S-Eval-5 is the natural home for executor wiring before M3-Eval close). NEW R-item NOT opened at this close (the carry-over packaging is sufficient); deliver-agent + human will revisit at S-Eval-4 planning round.
  - **OQ-S44.2 (active Skill / active UC resolution at extractor call site)**: dev §7 ran Alice's case against both (UC-A, resolve_faq) and (UC-H, resolve_intake), surfacing failures in both. **Codex verdict: acceptable to defer with OQ-S44.1** ("this belongs at the extractor call-site design: expected Skill/UC, actual Skill/UC, or both"). **Disposition: bundled with OQ-S44.1 carry-over** to S-Eval-4 / S-Eval-5 planning.
  - **OQ-S44.3 (UC-FP `consult-moderation-context-on-removal-explanation` mandatory tightness)**: dev §7 notes UC-FP cases without an available moderation context would FAIL this mandatory step even if the bot did its best with the unavailable signal. **Codex verdict: semantically defensible for a factual UC-FP removal explanation, but calibration is needed before executor wiring makes it production-active** ("if a legitimate no-context path escalates or defers instead of answering, convert the trace check to accept that path or downgrade the step to advisory"). **Disposition: monitoring posture preserved** — S-Eval-3 ships mandatory; S-Eval-4 trial real-LLM run (if executor wiring lands) OR M3-Eval-close real-LLM run surfaces whether downgrade is needed; fix-iteration candidate IF surfaced.
  - **OQ-S44.4 (mandatory:advisory split 10:8)**: **Codex verdict: acceptable calibration** ("the 10 mandatory steps cover grounding, intake completeness, escalation handover, terminal outcome, and UC-FP moderation grounding; the 8 advisory steps cover heterogeneous or alternate-path-prone behaviours; only the UC-FP no-context path needs close calibration"). **Disposition: ACCEPTED**; bundled with OQ-S44.3 as the single calibration surface.
  - **OQ-S44.5 (synthetic vs real-LLM verification at S-Eval-3 close)**: **Codex verdict: synthetic evidence is sufficient for S-Eval-3 close because the sprint is content-only and the production executor is not wired**. **Disposition: ACCEPTED for S-Eval-3 close**. Real-LLM run carry-over: needed by M3-Eval close after executor wiring / resolution semantics decided (bundled with OQ-S44.1 + OQ-S44.2 carry-over). S-Eval-4 trial milestone-close dry-run is the natural calibration vehicle.
  - **OQ-S44.6 (NEW — Python baseline reproducibility lapse) — non-blocking**: per `feedback_deliver_agent_cited_numbers_must_be_reproducible.md`. Dev handoff §9 cited `9 failed / 392 passed` via `uv run pytest`; Codex independent reproduction returned `5 failed / 396 passed` via `uv run python -m pytest`. Codex hypothesis: stale `.venv/bin/pytest` shebang under Codex's local checkout. The 5 failure classes are a subset of the 9 dev cited; no S-Eval-3-specific failure appears under either runner. **Disposition: ROUTED TO M3-EVAL-SHARED CODEX REVIEW QUEUE** alongside OQ-S42.3 / OQ-S43.1 / OQ-S43.4 as a confirmation item — Codex independently confirms at M3-Eval close which runner invocation produces the true baseline and whether the 4-test delta is genuinely environment-only OR signals a latent test-collection issue (e.g., a pytest collection-time module-import error that `uv run python -m pytest` swallows). NOT a fix-iteration trigger at S-Eval-3 close. Next sub-sprint dev prompts MUST cite Python baselines via the runner that reproduces consistently across local + Codex environments (likely `uv run python -m pytest`); deliver-agent flags for `compact/sprint-045-dev-prompt.md`.

- **§12.5 R-item flips at S-Eval-3 close**: **none**. The 4 M3-Eval R-items (`R-l3-judge-form-context-trust-rubric` / `R-l1-source-citation-quality-rubric` / `R-cs040-l3-review-intake-completion-semantics` / `R-cs038-l3-review-intake-efficiency`) flip at S-Eval-5 per `docs/milestone_objective.md` §7. 2 unblocked-but-not-closed R-items (`R-case-spec-overrides-schema-scoring-extension`, `R-escalation-reason-runtime-evidence-contract-review`) stay as carry-over from S-Eval-1. The S-Eval-1-close-opened R-item `R-stale-test-escalation-enum-sync-v0_2-to-v0_3-migration` stays `proposed; impl deferred to whichever milestone consumes it` per S-Eval-1 close human direction (no path picked). **No new R-items surfaced at S-Eval-3 close**; the 6 OQ dispositions in §12.4 above are sufficient (carry-overs use OOSR-with-packaging-note pattern; no fresh R-item required at this close).

- **§12.6 Sprint 44 close-action row**: appended at `docs/action_bank.md` §6 close-action index per the housekeeping bundle.

- **§12.7 Carry-over to S-Eval-4 planning (LOAD-BEARING)**:
  - **Executor wiring (OQ-S44.1) is the natural S-Eval-4-or-S-Eval-5 blocker** for Tier-2 becoming a true production eval gate. S-Eval-4 trial milestone-close dry-run may surface this as the first observed real-LLM blocker; if so, deliver-agent + human decide whether to (a) broaden S-Eval-4 scope to include the executor wiring (small change at `executor.py:252` per Codex's diagnostic), OR (b) carry to S-Eval-5 + ship M3-Eval close with the wiring fix bundled into S-Eval-5.
  - **Active Skill/UC resolution semantics (OQ-S44.2) bundled with OQ-S44.1**: the call-site design (expected vs actual Skill/UC) is the natural S-Eval-4 trial dry-run surfacing.
  - **UC-FP moderation-context mandatory tightness (OQ-S44.3 + OQ-S44.4)**: S-Eval-4 trial real-LLM run (if executor wiring lands) is the natural calibration surface — if a legitimate UC-FP no-context path produces escalate-or-defer behaviour, downgrade `consult-moderation-context-on-removal-explanation` to advisory OR loosen its `trace_check` to accept the escalate-or-defer path.
  - **Real-LLM run by M3-Eval close (OQ-S44.5)**: synthetic Tier-2 verification is sufficient for S-Eval-3 close but NOT for M3-Eval close. S-Eval-4 trial milestone-close dry-run is the planned calibration vehicle; S-Eval-5 + M3-Eval close is the natural fallback if S-Eval-4 dry-run cannot land a real-LLM run.
  - **Python baseline runner discipline (OQ-S44.6)**: `compact/sprint-045-dev-prompt.md` carries an explicit note that all Python baseline citations in the S-Eval-4 handoff §9 MUST use `uv run python -m pytest` (or whichever invocation reproduces consistently across local + Codex environments). Deliver-agent's draft of S-Eval-4 review prompt (at M3-Eval close) will pre-derive Python baseline counts from `git`-equivalent reproducible commands BEFORE citing.
  - **Thin-corpus UCs preserved** (S-Eval-1 §12.7 + S-Eval-2 §12.7 carry-over): UC-G / UC-H / UC-I / UC-J = 0 anchor coverage; UC-F = 1; UC-B = 5. **S-Eval-4 bad-case selection from 17 approved overrides SHOULD prioritise UC-G/H/I/J entries** per S-Eval-1 close §12.7 direction; deliver-agent + human re-verify D1-D4 × UC-FP/G/H coverage at S-Eval-4 planning round. If a dimension or UC family is undersampled in the 17, the 3 fallback paths from S-Eval-1 launch constraint #2 (NOT YET LOCKED) apply: (a) accept narrower coverage; (b) supplement with non-override-sourced real sessions; (c) defer the missing dimension to a follow-on milestone with an R-item.
  - **OQ-S43.2 informs M3-Eval close calibration** (carry-over from S-Eval-2 + confirmed at S-Eval-3): dev observed NO `desc` cross-references another step's `id` by name across all 18 populated entries. M3-Eval close calibration candidate: whether to drop the `id` field as a follow-on simplification. Routed to M3-Eval-shared Codex review queue for confirmation.
  - **OQ-S43.5 monitoring posture preserved**: dev observed ZERO use of `session.<flag>_present` primitive across all 18 populated `trace_check` strings. The S-Eval-2 fallback permissiveness question remains dormant; NOT a fix-iteration candidate at S-Eval-3 close.
  - **Optional `create_case_controlled` before `request_handover` Tier-2 step NOT covered by S-Eval-3** (Codex deferred / non-blocking observation): the live `resolve_intake_collect_and_handover` Skill exposes `request_handover` as the required tool and the sprint's Alice gate is intake-complete-before-handover. M3-Eval close should decide whether controlled-case creation needs a future Tier-2 step (deliver-agent + human visit at M3-Eval close).

- **§12.8 Token-cost observation**: per-active-Skill turn delta best 92 / worst 730 / mean ~378 tokens (dev §9). Codex confirmed direction (worst `resolve_faq_grounded_answer` ~728; `resolve_intake_collect_and_handover` ~603; DISCOVER→RESOLVE→CONFIRM→CLOSE mean ~377 tokens/turn). Well within proposal §5.4 envelope (500-900) and milestone §5 acceptance bar (≤ 1000 tokens/turn). **No M3-Eval close calibration action needed** at this surface.

- **§12.9 Architecture-health metrics direction** (§6 of `iteration_governance.md`; collection not started but direction observable):
  - `new_semantic_hardcode_count` = 0 (every populated `desc` is row-1 ✅ soft narrative per Codex independent §5.3 walk; every `trace_check` uses only the 6 frozen DSL primitives — no keyword/regex/message-content).
  - `soft_signal_conversion_count` = +18 at S-Eval-3 close (the 18 populated `critical_steps[].desc` ARE soft-signal projections of previously implicit/scattered procedural intent per proposal §5.2 single-source-of-truth claim). Cumulative M3-Eval direction: 0 at S-Eval-2 close → +18 at S-Eval-3 close.
  - `planner_ownership_ratio` not decreased (projection slot adds soft signal; LLM still owns `goal / drift / UC hypothesis / next action / escalation posture / response strategy / customer-facing wording` per §1.3; several `desc` strings explicitly cede ownership to the LLM e.g., `removed-listing-route-with-moderation-context`: "The LLM owns whether the user's stated need is understanding or acting").
  - `shadow_disagreement_rate` not yet collected.

- **§12.10 OOSR / OOSR-with-packaging-note observations** (per `feedback_out_of_scope_review_packaging_rollforward.md`):
  - **OOSR 1**: OQ-S44.1 executor wiring (Codex acceptable-as-is for S-Eval-3) — packaging note: carry to S-Eval-4 / S-Eval-5 planning with explicit blocker hypothesis (Tier-2 cannot become production eval gate until wiring lands).
  - **OOSR 2**: OQ-S44.6 Python baseline reproducibility — packaging note: carry to M3-Eval-shared Codex review queue (alongside OQ-S42.3 / OQ-S43.1 / OQ-S43.4) for runner-invocation confirmation.
  - **OOSR 3**: Codex's `create_case_controlled` non-coverage observation — packaging note: M3-Eval close decision on whether a future Tier-2 step is needed (the live Skill exposes `request_handover` as required; deliver-agent + human visit at M3-Eval close).

- **§12.11 Next sub-sprint**: **S-Eval-4 (Sprint 45)** — Bad-case suite expansion + trial milestone-close dry-run. Layer `eval_spec` (data; not code). §7 stanza REQUIRED. **Codex deferred to M3-Eval milestone-shared review** per §4.3 default (no per-sub-sprint trigger fires for S-Eval-4 by default). Estimated 5-7 dev-days (heavy in human-review collaboration time for the trial close dry-run). Live `docs/sprint_objective.md` REPLACED with the full S-Eval-4 contract in this close-out bundle per the convention established at S-Eval-2 / S-Eval-3 launch (draft the full contract now while context is fresh). Dev prompt at `compact/sprint-045-dev-prompt.md` (NEW; bundled with this close-out). **S-Eval-3 close-out bundle (deliver-agent close, separate from dev commit `01770ac`)** contains: this §12 closure verdict appended to `docs/sprints/sprint-044-handoff.md`; `docs/sprints/sprint-044-objective.md` archive of the S-Eval-3 contract; `docs/sprints/sprint-044-codex-review.md` archive of the Codex per-sub-sprint review (delete-and-add supersession of live `docs/codex-findings.md` per `feedback_packaging_codex_findings_supersession.md`); live `docs/codex-findings.md` RESET to scaffold; live `docs/sprint_objective.md` REPLACED with S-Eval-4 contract; live `docs/10-handoff.md` §1 lead REFRESHED; `docs/action_bank.md` §6 Sprint 44 close-action row APPENDED; NEW `compact/sprint-045-dev-prompt.md`; NEW `compact/context-handoff-sprint-044-post-close.md` (cross-session continuity for next deliver-agent on S-Eval-4 dispatch). Per `feedback_commit_at_end_bundles_deliver_artefacts.md`: dev did NOT stage these deliver-agent close-out files at commit `01770ac`; human bundles separately at the deliver-agent close commit.
