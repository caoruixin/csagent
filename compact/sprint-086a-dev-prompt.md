# Dev prompt — Sprint 086a / S-Auto-31 (M-Auto-7 S-Y1 Part A) — CS4 readiness: projection infra

## 1. Role identity

你是 dev agent for **Sprint 086a / S-Auto-31 (M-Auto-7 S-Y1, Part A)** — the
projection-infra half of the last autoloop launch blocker. Goal: **add the two
data-derived projection slots the CS4 autoloop pilot will reference** —
`moderation_reason_available` (boolean) and a NEW backward-compatible
`candidate_use_cases_named` slot. Layer: `prompt_projection`. §7-REQUIRED.
**No skill-procedure text change** (that is the S-Y2 pilot).

## 2. Read order (minimal)

- `AGENTS.md` (auto-loaded).
- This prompt.
- Anchors (verified at HEAD `auto-loop-branch`, 2026-06-08):
  - `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java:499-508` (`candidate_use_cases` bare-ID emission — LEAVE UNCHANGED; add the new slot alongside) + `:1146-1177` (`buildDiscoverDisambiguationSignalsNode`) + `:1185-1206` (`extractListingStatus`, the null-safe pattern to mirror) + `:1384-1477` (`customer_context_status`, already live — do not touch).
  - `server/src/main/java/com/gumtree/csagent/service/runtime/UseCaseRegistryService.java:100` (`getUseCase(String)`) + `:166-168` (`UseCaseDefinition` record, `name` field).
  - `server/src/main/java/com/gumtree/csagent/service/runtime/FormContextIngestionService.java:125-126` (`session.setModerationContext(...)` — the data source for the boolean).
  - `server/src/main/resources/skills/discover_triage.yaml` + `resolve_faq_grounded_answer.yaml` (declaration blocks only).

## 3. Embedded contract

### 3.1 Background

The CS4 pilot (S-Y2) will have the autoloop author a skill procedure that
references projection evidence. Two slots must exist first (human-authored —
autoloop cannot add projection infra):
- `moderation_reason_available` — so the procedure can prefer the
  moderation-grounded answer when a removal reason is on file.
- candidate-UC **names** — today the LLM sees bare IDs (`["UC-A","UC-FP",...]`)
  and must recall from training what each means (the CS2-original UC-G
  hallucination + CS4 UC-FP invisibility). Names make the choice grounded.

### 3.2 What to change (Scope)

A1. **`moderation_reason_available`** — in `buildDiscoverDisambiguationSignalsNode`
   (`:1146-1177`), add `node.put("moderation_reason_available", <bool>)` where
   the bool = `session.getModerationContext() != null && !session.getModerationContext().isBlank()`.
   Mirror the `extractListingStatus` null-safety. This is an ADDITIVE field on
   the existing disambiguation node (existing fields unchanged). **Project ONLY
   the boolean — never parse/emit the moderation text** (hard fence: PII /
   grounding boundary).
A2. **candidate-UC names — BACKWARD-COMPATIBLE ADDITIVE.** Do NOT modify the
   existing `candidate_use_cases` emission at `:499-508` (it stays a string
   array). Add a SEPARATE new slot `candidate_use_cases_named`, gated by its
   own skill declaration, emitting an array of objects:
   ```java
   if (skillRequiresContextKey(session, activeUc, "candidate_use_cases_named")) {
       ArrayNode named = objectMapper.createArrayNode();
       if (session.getCandidateUseCases() != null) {
           for (String uc : session.getCandidateUseCases()) {
               if (uc == null || uc.isBlank()) continue;
               ObjectNode o = objectMapper.createObjectNode();
               o.put("id", uc);
               var def = useCaseRegistry.getUseCase(uc);   // null-safe
               o.put("name", def != null ? def.name() : uc);
               named.add(o);
           }
       }
       projection.set("candidate_use_cases_named", named);
   }
   ```
   Null-safe on unknown id (fall back to the id as the name). No UC-id rename.
A3. **`customer_context_status`** — already emitted (`:1384-1477`). No code
   change; confirm it is reachable on the DISCOVER + UC-A/UC-FP tuples the
   pilot cares about.
A4. **yaml declarations** — declare `candidate_use_cases_named` in
   `discover_triage.yaml` (`required_context_keys`) so the new slot surfaces;
   add `candidate_use_cases_named` + `discover_disambiguation_signals` to
   `resolve_faq_grounded_answer.yaml` `required_context_keys` /
   `state_inheritance.soft_signal_via_projection` IF that skill should see them
   for the pilot. **Declaration-only — NO procedure / grounding_instruction /
   critical_steps text change.**

### 3.3 Hard fences / STOP

- Only the boolean `moderation_reason_available`; never the raw text.
- The EXISTING `candidate_use_cases` string-array shape is UNCHANGED
  (backward-compatible); names live in the NEW additive
  `candidate_use_cases_named` slot. No UC-id rename/migration.
- No procedure-text change in any skill yaml (declaration blocks only).
- Slots are data-derived (registry + context presence); no keyword/regex/
  if-else/enum, no Java branch on the new slots.
- Do not touch `customer_context_status` internals or the existing
  `discover_disambiguation_signals` fields (ad_status_observed,
  topic_subject_carries_multiple_candidate_ucs, candidate_ucs_for_topic).
- If a full-projection golden snapshot test newly includes the additive
  slot(s), reconcile it ADDITIVELY (new fields appear; existing fields
  unchanged) — that is a test file; flag it in the handoff.

### 3.4 Tests

1. `moderation_reason_available` == true when `session.moderationContext` is
   present; == false when null/blank.
2. Anti-leak: the projection JSON contains NO moderation-review text /
   reason-code value (only the boolean).
3. `candidate_use_cases_named` emits `{id,name}` with names from the registry;
   null-safe on unknown id.
4. **Backward-compat:** the existing `candidate_use_cases` slot still emits the
   UNCHANGED string array (no shape change).
5. Existing `discover_disambiguation_signals` fields + `customer_context_status`
   unchanged (regression).
6. Reconcile any golden projection snapshot test additively.
- Focused Java suite: no new regression vs post-S-X baseline (1373/1/0/2).
  Report counts. No real-LLM re-bless (Part A is projection wiring).

### 3.5 §7 / Codex

§7-REQUIRED (stanza in `docs/sprint_objective.md`). Per-sub-sprint Codex
deferred to the M-Auto-7 milestone-shared close. Do NOT dispatch Codex.

### 3.6 Handoff + commit

Author `docs/sprints/sprint-086a-handoff.md` (§0 evidence; §11 Codex deferral +
backward-compat confirmation + golden-reconcile note + any OQs; §12 re-bless
deferred to post-S-Y1). Stage only `ContextProjectionBuilder.java`, the two
skill yamls (declaration-only), projection test file(s), reconciled golden
test(s). No `git add -A`.

## 4. Self-check checklist

- [ ] `moderation_reason_available` boolean added; raw text never emitted (anti-leak test green).
- [ ] NEW `candidate_use_cases_named` slot emits `{id,name}` from registry; null-safe.
- [ ] EXISTING `candidate_use_cases` string-array UNCHANGED (backward-compat test green).
- [ ] No procedure-text change; yaml edits are declaration-only.
- [ ] customer_context_status + existing disambiguation fields untouched.
- [ ] No Java branch on the new slots.
- [ ] Tests green; golden snapshots reconciled additively; focused Java suite no new regression (counts reported).
- [ ] Handoff authored; only authorized files staged.
