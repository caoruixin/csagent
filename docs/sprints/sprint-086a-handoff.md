---
title: Sprint 086a / S-Auto-31 handoff — CS4 readiness Part A (projection infra)
doc_tier: sprint-archive
status: current
implementation_status: implemented
source_of_truth: server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java
last_reviewed: 2026-06-08
review_cadence: ad hoc
supersedes: []
superseded_by: null
notes: >
  M-Auto-7 S-Y1 Part A — the projection-infra half of the last autoloop
  launch blocker. Adds two data-derived, OBSERVABLE projection slots the CS4
  autoloop pilot (S-Y2) will reference, with NO skill-procedure text change:
  (A1) `moderation_reason_available` — a presence-only boolean inside the
  existing `discover_disambiguation_signals` node, derived from
  `session.moderationContext` presence; the raw moderation text is NEVER
  emitted (PII / grounding fence). (A2) `candidate_use_cases_named` — a NEW
  backward-compatible additive slot emitting `{id,name}` from the UC registry
  so the DISCOVER classify choice is grounded by name, not opaque id (CS4
  UC-FP invisibility + CS2 UC-G hallucination). The EXISTING bare-ID
  `candidate_use_cases` slot is UNCHANGED. Layer prompt_projection;
  §7-REQUIRED stanza in sprint_objective. Per-sub-sprint Codex EXEMPT (folds
  into the M-Auto-7 milestone-shared review). A4's CONDITIONAL resolve_faq
  declaration was DECLINED — see OQ-S86a.1 (Sprint53 §3.H audit invariant
  conflict). No real-LLM re-bless in Part A (projection wiring); pre-pilot
  re-bless runs post-S-Y1 (§12).
---

# Sprint 086a / S-Auto-31 — CS4 readiness Part A (projection infra)

## §0 — Cold-start evidence table

| Item | Result |
|------|--------|
| **Goal** | Make the two data-derived projection slots the CS4 autoloop pilot (S-Y2) will reference EXIST first (autoloop cannot author projection infra). No skill-procedure text change here. |
| **Layer (§3.2)** | `prompt_projection` (additive observable slots; the runtime does NOT branch on either). |
| **A1 — `moderation_reason_available`** | `ContextProjectionBuilder.java` `buildDiscoverDisambiguationSignalsNode` (now `:1179-1221`; the boolean `put` at `:1219`). Additive boolean on the existing disambiguation node: `node.put("moderation_reason_available", session.getModerationContext() != null && !session.getModerationContext().isBlank())`. Mirrors the `extractListingStatus` null-safety. **PRESENCE-ONLY**: the raw moderation-review text / reason-code is never parsed or emitted (hard PII / grounding fence). |
| **A2 — `candidate_use_cases_named`** | `ContextProjectionBuilder.java:511-542` (NEW block, immediately after the UNCHANGED `candidate_use_cases` block at `:499-509`). Emits an array of `{id, name}`; `name` from `useCaseRegistry.getUseCase(uc).name()`, **null-safe** → falls back to the id when the registry has no definition. Gated by `skillRequiresContextKey(session, activeUc, "candidate_use_cases_named")` — same registry-data-driven helper + default-TRUE-on-unmapped pattern as `candidate_use_cases`. Null/blank candidate ids skipped. |
| **A2 — backward-compat** | The EXISTING `candidate_use_cases` block (`:499-509`) is UNCHANGED — still a bare string array. The new slot is a SEPARATE additive companion. No UC-id rename / migration. |
| **A3 — `customer_context_status`** | No code change (already live; `addAdContextPremiseProjection` `:1509`, the `put` at `:1522`). Reachable on the DISCOVER + UC-A/UC-FP tuples via `addAdContextPremiseProjection`; covered by `AdReferenceProjectionTest` (untouched). |
| **A4 — yaml (declaration-only)** | `discover_triage.yaml` `required_context_keys` += `candidate_use_cases_named` (its `state_inheritance.soft_signal_via_projection` ALREADY declares `discover_disambiguation_signals`, so `moderation_reason_available` surfaces in DISCOVER). `resolve_faq_grounded_answer.yaml` — **NOT edited** (A4 conditional declined; see OQ-S86a.1). NO procedure / grounding_instruction / critical_steps text change. |
| **Test #1 — moderation true/false** | `moderation_reason_available` == true when `moderationContext` present; == false when null AND when blank. PASS. |
| **Test #2 — anti-leak** | A recognizable reason-code + reviewer-note in `moderationContext` does NOT appear anywhere in the projection JSON (only `"moderation_reason_available":true` does). PASS. |
| **Test #3 — named {id,name}** | `candidate_use_cases_named` emits `{id,name}` with registry names; null-safe on unknown id (falls back to id); null/blank ids skipped; empty array when no candidates. PASS. |
| **Test #4 — backward-compat** | `candidate_use_cases` still a bare string array (`isTextual()`), same elements, shape unchanged. PASS. |
| **Test #5 — regression** | Existing `discover_disambiguation_signals` sub-fields (`ad_status_observed` / `topic_subject_carries_multiple_candidate_ucs` / `candidate_ucs_for_topic`) keep value+type alongside the new boolean. PASS. |
| **New test class** | `server/.../service/runtime/Sprint86aProjectionSlotsTest.java` (10 tests). |
| **Golden reconcile (additive)** | `PhaseEvaluatorSkillIntegrationTest.assertGoldenDiscover:271` pinned `requiredContextKeys == {form_context, candidate_use_cases}`; reconciled ADDITIVELY to `{form_context, candidate_use_cases, candidate_use_cases_named}` (existing keys unchanged). Test file; see OQ-S86a.2. |
| **Focused gates** | `Sprint86aProjectionSlotsTest` 10/0/0/0 · `DiscoverDisambiguationSignalsProjectionTest` 9/0/0/0 (regression, untouched) · `Sprint7CandidateUseCasesProjectionTest` 7/0/0/0 (backward-compat regression, untouched) · `Sprint52ProjectionSkillDrivenTest` 7/0/0/0 (`moderation_context` no-leak guard regression, untouched) · `Sprint53SkillDeclarationGatingTest` 11/0/0/0 (audit-coverage guard, green after the resolve_faq revert) · `ContextProjectionBuilderTest` 17/0/0/0 · `SkillLoaderTest` 18/0/0/0 · `PhaseEvaluatorSkillIntegrationTest` 13/0/0/0 (golden reconciled). |
| **Full Java module suite** | `mvn test` → **1383 run / 1 failure / 0 errors / 2 skipped**. |
| **Delta vs post-S-X baseline `1373 / 1 / 0 / 2`** | **+10 run** = the 10 new `Sprint86aProjectionSlotsTest` cases. Failures / errors / skipped unchanged. **No new regression.** |
| **Sole failure** | `SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly` — the documented inherited OQ-S41.5 prompt-tiebreaker test. Provably uncoupled: verified failing with this sub-sprint's production code stashed out (1/1 fail on clean code); touches no projection surface. |
| **Hard fences honored** | Only the boolean `moderation_reason_available` projected — raw text never (anti-leak test green). Existing `candidate_use_cases` string-array shape UNCHANGED. No procedure-text change (declaration-only yaml). Slots data-derived (registry + context presence); no keyword/regex/if-else/enum; NO Java branch on either new slot. `customer_context_status` internals + existing `discover_disambiguation_signals` fields untouched. |
| **§7 stanza** | §7-REQUIRED — present in `docs/sprint_objective.md` (lines 121-135). |
| **Codex** | Per-sub-sprint EXEMPT; folds into M-Auto-7 milestone-shared close review. Codex NOT dispatched. |

## §11 — Codex deferral, backward-compat confirmation, golden-reconcile note, OQs

**Codex (§4.3):** per-sub-sprint Codex review is DEFERRED to the M-Auto-7
milestone-shared close review. No §4.3 trigger fires: adds no Tier-0
invariant; not §1.7-adjacent (both slots are registry/context-presence
data-derived — no keyword/regex/enum/per-UC matrix, no Java branch on the
slots); no hard-fence violation; not a fix-iteration. Codex was NOT dispatched.

**Backward-compat confirmation.** The existing bare-ID `candidate_use_cases`
emission (`ContextProjectionBuilder.java:499-509`) was NOT modified — it still
emits a string array. `candidate_use_cases_named` is a SEPARATE additive slot.
Pinned by `Sprint86aProjectionSlotsTest.candidateUseCases_backwardCompat_stillBareStringArray`
(asserts each element `isTextual()`) and by the untouched
`Sprint7CandidateUseCasesProjectionTest` (7/0/0/0). No UC-id rename/migration.

**OQ-S86a.1 — A4 resolve_faq declaration DECLINED (Sprint53 §3.H conflict);
decision surfaced for S-Y2 / deliver.** sprint_objective A4 (lines 60-62) and
the dev prompt A4 both made the `resolve_faq_grounded_answer.yaml` declaration
**conditional** ("…if that skill should see them"). Adding
`discover_disambiguation_signals` to `resolve_faq`'s
`soft_signal_via_projection` was attempted, then **reverted**, because it
directly violates an existing audit invariant:
`Sprint53SkillDeclarationGatingTest.productionSkillYamls_matchAuditDeclarationCoverage:326`
("Audit §3.H: resolve_faq_grounded_answer MUST NOT declare
discover_disambiguation_signals", per
`docs/diagnostics/m5-s4-skill-declaration-audit.md` §3.H — only `discover_triage`
may declare it). Silently rewriting that guard test + its audit doc is an
architecture decision, not projection wiring, and out of Part A's scope.
**Why declining is safe for the pilot:** CS4's UC-FP *invisibility* lives at
DISCOVER *classification* time — and `discover_triage` already declares
`discover_disambiguation_signals`, so `moderation_reason_available` DOES surface
during DISCOVER (where the route to UC-FP vs UC-H is chosen). The RESOLVE-time
moderation grounding the pilot wants is already covered by the existing
`consult-moderation-context-on-removal-explanation` critical step (mandatory for
UC-FP), which uses the `get_moderation_review_context` tool, not this projection
boolean. `candidate_use_cases_named` was likewise NOT added to `resolve_faq`:
in RESOLVE the UC is already committed, so candidate *names* are a DISCOVER-time
disambiguation aid with no RESOLVE consumer. **Decision needed from S-Y2 /
deliver:** if the pilot procedure genuinely needs `moderation_reason_available`
(or candidate names) inside RESOLVE-FAQ, that requires a deliberate revisit of
the M5-S4 §3.H audit decision (update the audit doc + `Sprint53…` guard) — a
governance change, tracked here rather than smuggled into Part A.

**OQ-S86a.2 — golden snapshot test required a sanctioned additive update.**
The contract (§3.3 / §3.4) anticipated this: the authorized A4 addition of
`candidate_use_cases_named` to `discover_triage.required_context_keys` changed
the composed DISCOVER `PhasePlan.requiredContextKeys`, which
`PhaseEvaluatorSkillIntegrationTest.assertGoldenDiscover` pins as a closed set.
Three golden tests failed transiently; the expected set was reconciled
**additively** (`{form_context, candidate_use_cases}` →
`{…, candidate_use_cases_named}` — existing keys unchanged); all 13 pass. This
is a **test file**, within the contract's stage set. Flagged for visibility,
not a scope breach.

## §12 — Re-bless deferred to post-S-Y1 (no real-LLM evidence in Part A)

Per the sprint contract, **no real-LLM re-bless runs in Part A** — this is
projection wiring. The change is verified by Java/unit tests + diff inspection
only. Both new slots are OBSERVABLE inputs to the LLM; whether their presence
actually improves the CS4 DISCOVER route (UC-FP becomes visible by name; a
moderation reason on file biases toward the removal-explanation UC) is an
eval-evidence claim that, per §5.7 (a mocked-LLM test cannot be primary
evidence that a projection change altered LLM behaviour, since the mock
controls the measured variable), is confirmed only at the **pre-pilot baseline
re-bless after S-Y1** (= pilot Part C.1; batched re-bless cadence,
deliver-agent + human). The mocked/unit tests above cover the projection
emission, anti-leak fence, registry-name lookup, backward-compat shape, and the
skill-declaration gate only.

Expected re-bless signal (forensic, for the post-S-Y1 reviewer): on the CS4
UC-FP-invisibility family (removed-listing DISCOVER turns with a moderation
reason on file), the DISCOVER classify choice should reach UC-FP / ask the
understand-vs-appeal clarifying question rather than missing UC-FP or
hallucinating an out-of-registry UC; no rise in mis-routes on the anti-误杀
negative-control (generic UC-A policy question with no moderation reason).
