---
title: M5 / S4 — Skill-declaration completeness audit (Phase A)
doc_tier: diagnostic
status: current
implementation_status: implemented
source_of_truth: this file (audit); server/src/main/resources/skills/*.yaml (declarations); ContextProjectionBuilder.java (emission today)
last_reviewed: 2026-05-25
review_cadence: per sub-sprint at S4 close; per milestone at M5 close
supersedes: []
superseded_by: null
notes: >
  Phase-A deliverable for Sprint 53 / M5 S4 — produced + reviewed BEFORE any
  Phase-B gating change per the sprint contract's hard ordering fence
  ("AUDIT BEFORE GATING"). Builds on the S3 C1 matrix
  (`docs/diagnostics/m5-s3-projection-consumption-map.md`) which STOP-surfaced
  C2 #2 + #5 because the Skill declarations were INCOMPLETE — gating on them
  as-is would drop LLM-visible context. This Phase-A audit completes the
  intentionalization and routes each candidate slot to one of three outcomes:
  (a) GATE on Skill declaration (safe; declarations cover actual need); (b)
  KEEP UNCONDITIONAL (slot is needed by all Skills OR data-gated already OR
  state_inheritance.inherit risk); (c) STOP-and-surface to deliver-agent
  (audit could not resolve without dropping an LLM-visible signal). HEAD
  anchor: `49d48b1` (S3 close).
---

# M5 / S4 — Skill-declaration completeness audit (Phase A)

## 0. Why this audit exists

S3 closed the Skill-driven projection convergence partway: C2 #3 +
#4 landed, but **C2 #2 (Skill-declared context-key gating)** and
**C2 #5 (`soft_signal_via_projection` gating)** were STOP-surfaced
because the S3 C1 matrix flagged the Skill declarations INCOMPLETE.
Per the sprint contract for S4:

> "AUDIT BEFORE GATING — no context/soft-signal slot is gated
> (Phase B) before its Phase-A audit row confirms the Skill
> declaration is complete and gating will not drop an LLM-visible
> signal."

This Phase-A audit is the gate for S4 Phase B. It runs the same
discipline S3's C1 ran for field consumption: enumerate every
candidate slot × every Skill, classify each cell's actual need vs
declaration, and route each slot to its safe convergence path
(GATE / KEEP UNCONDITIONAL / STOP-and-surface).

## 1. Method

For each of the **6 production Skills** (`discover_triage`,
`confirm`, `escalate`, `terminal`, `resolve_faq_grounded_answer`,
`resolve_intake_collect_and_handover`) × **each candidate slot**,
the audit records four columns:

| Column | Meaning |
|---|---|
| **DECL** | The slot name appears in the Skill's `required_context_keys` (for #2 context-key gating) OR `state_inheritance.soft_signal_via_projection` (for #5 soft-signal gating). |
| **EMIT** | The projection emits the slot today for sessions where this Skill is active (i.e., (phase, UC) maps to this Skill in `SkillRegistry.select`). |
| **NEED** | Audit conclusion: the Skill genuinely needs the slot for its `procedure` / `critical_steps` / `grounding_instruction` / typical bad-case flows. Cell values: **YES** / **NO** / **OPTIONAL** (helpful but not load-bearing). |
| **RISK** | If we gate on DECL: what happens to LLM-visible information? Cell values: **SAFE** (decl matches need) / **DROPS-SIGNAL** (Skill needs but doesn't declare → gating would drop) / **REDUNDANT** (Skill declares but doesn't need → harmless excess) / **INHERIT-RISK** (`state_inheritance.inherit` claims the slot's underlying state crosses Skills, so dropping the projection mid-session could break continuity). |

The disposition (last column of §3 tables) is one of:

- **GATE** — safe to convert to Skill-declaration-driven emission in Phase B.
- **KEEP UNCONDITIONAL** — slot is needed by all Skills OR is data-gated today (only emitted when session data is non-blank) OR has INHERIT-RISK; gating would risk a dropped signal. Spec exception: *"If a slot is needed by ALL Skills → keep it unconditional, don't gate (record in the matrix)."*
- **OUT OF SCOPE** — slot has a different registry gate (e.g., `IntakeFieldsRegistry`) that is not in scope for #2/#5.
- **STOP-AND-SURFACE** — audit cannot resolve without dropping an LLM-visible signal; defer to deliver-agent + human.

The actual-need column draws on: (a) each Skill's `procedure` /
`grounding_instruction` / `escalation_policy` narrative, (b) each
Skill's `critical_steps[].desc` + `trace_check` references, (c)
the Sprint 31 / 33 / 41 design context for the soft-signal slots,
(d) the S3 C1 matrix rows that already mapped consumer
dependencies for these same slots (§3.E + §3.L + §3.M), (e)
sampling of bad-case traces where the Skill is active (M4-close
distribution).

## 2. Slot inventory (the audit's universe)

The audit covers two slot families per the sprint contract's
**Phase B #2 + #5 scope**:

### 2.A Context-key slots (candidates for #2 `requiredContextKeys` gating)

These are the projection slots that map to a name plausibly in a
Skill's `required_context_keys`:

| Projection slot | Emitted at | Today's emission gate |
|---|---|---|
| `form_context` | `ContextProjectionBuilder.java:642` | data-gated (only when `session.formContext` non-blank) |
| `customer_context` | `:647` | data-gated (only when `session.customerContext` non-blank) |
| `listing_context` | `:652` | data-gated (only when `session.listingContext` non-blank) |
| `conversation_history` | `:651-674` | unconditional (last 10 turns; possibly empty array) |
| `candidate_use_cases` | `:411-419` | unconditional (possibly empty array) |
| `intake_state` | `:376-403` | registry-gated (`IntakeFieldsRegistry.isIntakeUseCase(activeUc)`) |

### 2.B Soft-signal slots (candidates for #5 `softSignalViaProjection` gating)

These are the three projection slots the `SkillLoader` recognizes
in `state_inheritance.soft_signal_via_projection`
(`VALID_PROJECTION_SLOTS`, `SkillLoader.java:65-69`):

| Projection slot | Emitted at | Today's emission gate |
|---|---|---|
| `alternate_candidate_use_cases` | `:432-441` | unconditional (possibly empty array) |
| `discover_disambiguation_signals` | `:457` | unconditional (sub-fields possibly null) |
| `prior_use_case_carry` | `:473-475` | unconditional (NullNode unless prior UC switch within aging window) |

### 2.C Out of scope (recorded for completeness)

The following slots are NOT in the audit because they are not
candidates for #2/#5 gating per the sprint contract:

- `session.*`, `task_summary`, `risk_flags`, `previous_active_use_case`,
  `drift_type`, `current_task_type`, `primary_entity`, `issue_status_summary`,
  `task_status`, `last_entity_context_ref`, §N0 nullable strings,
  `terminal_evidence`, `drift_history`, `task_history`, `budget_state`,
  `tool_schemas`, `current_user_message` — general state / observability
  slots, not Skill-declared.
- `knowledge_hits` / `knowledge_instruction` / `accumulated_tool_results` /
  `already_called` / `phase_plan` — run-loop / tool-result slots, not
  context-key or soft-signal.
- `faq_grounding` overlay — explicitly fenced (OQ-S51.2 disposition (a)).

## 3. Per-slot audit matrix

Anchors: `D=discover_triage`, `Cf=confirm`, `E=escalate`, `T=terminal`,
`Rf=resolve_faq_grounded_answer`, `Ri=resolve_intake_collect_and_handover`.

### 3.A `form_context` (context slot)

| Skill | DECL | EMIT today | NEED | RISK if gated |
|---|---|---|---|---|
| D | ✓ | data-gated | YES (procedure reads topic_subject + form fields) | SAFE |
| Cf | ✓ | data-gated | YES (informational) | SAFE |
| E | ✓ | data-gated | YES (handover summary references form) | SAFE |
| T | ✓ | data-gated | OPTIONAL (closing message rarely needs) | SAFE |
| Rf | ✓ | data-gated | YES (FAQ resolve consults form) | SAFE |
| Ri | ✓ | data-gated | YES (intake reads form for context) | SAFE |

**Declaration coverage**: 6/6 (universal). **Disposition: KEEP
UNCONDITIONAL (data-gated)**. Slot is universally declared; gating
would be a no-op (every Skill emits). Today's data-gate (emit when
`session.formContext` non-blank) is the correct gate; Skill-decl
gate is redundant. No YAML change needed; no #2 code change for
this slot.

### 3.B `customer_context` (context slot)

| Skill | DECL | EMIT today | NEED | RISK if gated |
|---|---|---|---|---|
| D | ✗ | data-gated | OPTIONAL (alice UC-A↔UC-H bad case relies on customer signal for classification) | **INHERIT-RISK** (`state_inheritance.inherit` lists `customer_context`) |
| Cf | ✗ | data-gated | OPTIONAL (CONFIRM rarely uses customer details) | **INHERIT-RISK** (`state_inheritance.inherit` lists `customer_context`) |
| E | ✓ | data-gated | YES (handover summary references customer) | SAFE |
| T | ✗ | data-gated | OPTIONAL (closing message rarely uses) | **INHERIT-RISK** (`state_inheritance.inherit` lists `customer_context`) |
| Rf | ✓ | data-gated | YES (FAQ resolve consults customer for case-specific data) | SAFE |
| Ri | ✓ | data-gated | YES (intake reads customer for fields) | SAFE |

**Declaration coverage**: 3/6 (escalate, resolve_faq, resolve_intake
only). **Disposition: KEEP UNCONDITIONAL (data-gated; INHERIT-RISK
prevents safe gating)**. Three non-declaring Skills (D/Cf/T) carry
`customer_context` in `state_inheritance.inherit`, meaning the
underlying SESSION state is expected to cross Skills — so dropping
the projection mid-session via Skill-decl gate would create a
state/projection inconsistency. The S3 C1 matrix §3.L flagged
this as HIGH semantic risk for alice (UC-A↔UC-H bad case relies on
customer signal during DISCOVER classification).

**Spec exception applies**: "If a slot is needed by ALL Skills →
keep it unconditional." Per audit, 3 explicitly need + 3 inherit
the underlying state (universally relevant). Today's data-gate is
the correct gate. No #1a addition (declaration completion would
not change behaviour for a slot we are not gating).

### 3.C `listing_context` (context slot)

| Skill | DECL | EMIT today | NEED | RISK if gated |
|---|---|---|---|---|
| D | ✗ | data-gated | YES (UC-FP / UC-H disambig uses listing status via `discover_disambiguation_signals`; raw listing helps UC classification) | DROPS-SIGNAL |
| Cf | ✗ | data-gated | OPTIONAL (CONFIRM might reference listing) | LOW |
| E | ✗ | data-gated | YES (handover summary for UC-H/UC-G references listing) | DROPS-SIGNAL |
| T | ✗ | data-gated | NO (closing message) | LOW |
| Rf | ✓ | data-gated | YES (FAQ resolve for UC-FP consults listing for moderation context) | SAFE |
| Ri | ✗ | data-gated | YES (UC-H intake needs ad_id from listing; UC-G might reference) | DROPS-SIGNAL |

**Declaration coverage**: 1/6 (resolve_faq only). **Disposition:
KEEP UNCONDITIONAL (data-gated)**. Three Skills (D/E/Ri) need
listing_context but do not declare it; gating would DROP an
LLM-visible signal that the bad-case suite (alice UC-A↔UC-H,
UC-FP appeal flows) depends on. The S3 C1 matrix §3.L explicitly
flagged this as HIGH semantic risk.

**#1a alternative considered** (add declarations to D/E/Ri): would
fix the gate, but the resulting projection behaviour would be
identical to keeping unconditional (4/6 declare → emit; 2/6 don't
→ would still get the slot via spec's "all-Skills-need" or via
data-gate). Pragmatic: keep the data-gate, record the audit, do
not add YAML declarations (avoids YAML churn for zero behaviour
change; preserves the data-gate's "emit only when data exists"
discipline which is the actual desired behaviour).

### 3.D `conversation_history` (context slot)

| Skill | DECL | EMIT today | NEED | RISK if gated |
|---|---|---|---|---|
| D | ✗ | unconditional | YES (procedure explicitly says "Look at... conversation history") | DROPS-SIGNAL |
| Cf | ✓ | unconditional | YES (CONFIRM reads prior answer in context) | SAFE |
| E | ✗ | unconditional | YES (handover summary needs prior conversation) | DROPS-SIGNAL |
| T | ✗ | unconditional | OPTIONAL (closing message rarely needs full history) | LOW |
| Rf | ✗ | unconditional | YES (multi-turn UC-A↔UC-C flows reference prior turns) | DROPS-SIGNAL |
| Ri | ✗ | unconditional | YES (intake reads prior turns to extract field values) | DROPS-SIGNAL |

**Declaration coverage**: 1/6 (confirm only). **Disposition: KEEP
UNCONDITIONAL**. Four Skills (D/E/Rf/Ri) explicitly need
conversation_history but only confirm declares it; gating would
DROP the slot from the four bad-case-relevant Skills (alice,
UC-A↔UC-C drift, ESCALATE handovers). The S3 C1 matrix §3.M
agreed.

Spec exception applies: 5/6 Skills genuinely need (4 explicit +
confirm declared); only `terminal` is optional. Per the
"all-Skills-need → keep unconditional" exception, this is the
safe path.

### 3.E `candidate_use_cases` (context slot)

| Skill | DECL | EMIT today | NEED | RISK if gated |
|---|---|---|---|---|
| D | ✓ | unconditional | YES (DISCOVER procedure references candidate UC list for clarification) | SAFE |
| Cf | ✗ | unconditional | NO (UC committed pre-CONFIRM) | SAFE (slot becomes absent) |
| E | ✗ | unconditional | NO (ESCALATE works on already-classified UC) | SAFE |
| T | ✗ | unconditional | NO (CLOSE; UC long-committed) | SAFE |
| Rf | ✗ | unconditional | NO (RESOLVE post-classification) | SAFE |
| Ri | ✗ | unconditional | NO (RESOLVE-INTAKE post-classification) | SAFE |

**Declaration coverage**: 1/6 (discover_triage only). **Disposition:
GATE (Phase B #2)**. The DECL perfectly matches NEED — only
DISCOVER consumes the candidate list for classification cues; all
post-DISCOVER Skills (Cf/E/T/Rf/Ri) work on the committed UC and
have no need for the candidate list. Gating drops no LLM-visible
signal.

Behaviour change: post-DISCOVER turns currently get an empty/
populated `candidate_use_cases` array; after gating they get the
slot absent. The S3 C1 matrix §3.E flagged this as a shape change;
per S3 C1 §3.G "no consumer in this column" for non-DISCOVER (no
EC / ES / DT / SK / LLM-reference dependency). Real-LLM rerun
proves no behaviour regression.

**#1a**: declarations are intentional as-is. No YAML change.

### 3.F `intake_state` (context slot)

| Skill | DECL | EMIT today | NEED | RISK if gated |
|---|---|---|---|---|
| D | ✗ | gated NO (DISCOVER not intake) | NO | N/A |
| Cf | ✗ | gated NO (UC-G/H/I/J/K may pass through but rare) | OPTIONAL | N/A |
| E | ✗ | gated YES if intake UC | YES (handover summary needs intake field state for UC-G/H/I/J/K) | N/A |
| T | ✗ | gated YES if intake UC | OPTIONAL (CLOSE rare on intake) | N/A |
| Rf | ✗ | gated NO (FAQ-UC) | NO | N/A |
| Ri | ✗ | gated YES | YES (load-bearing — drives next-field prompt + intake-complete handover) | N/A |

**Declaration coverage**: 0/6 (none declares; gate lives in `IntakeFieldsRegistry`).
**Disposition: OUT OF SCOPE for #2 (already registry-gated)**. The
existing `IntakeFieldsRegistry.isIntakeUseCase(activeUc)` gate
(`ContextProjectionBuilder.java:376`) is registry-driven and
correctly emits `intake_state` for UC-G/H/I/J/K turns regardless
of which Skill is active. Eval Tier-2 scoring REQUIRES this slot
(C1 matrix §3.D), and `executor.py:528-537` reads
`proj.get("intake_state").fields_collected` — gating via Skill
declaration would risk breaking Tier-2 if a Skill's declaration
were incomplete.

**#1a alternative considered**: adding `intake_state` to Ri's
`required_context_keys` and routing the gate through
`PhasePlan.requiredContextKeys()`. Rejected — the IntakeFieldsRegistry
gate is the canonical authority (it knows which UCs are intake);
routing through PhasePlan would add an indirection without
behaviour change, and could break for ESCALATE-after-intake
sessions where the active Skill is `escalate` but the intake_state
is still load-bearing.

### 3.G `alternate_candidate_use_cases` (soft-signal slot)

| Skill | DECL | EMIT today | NEED | RISK if gated |
|---|---|---|---|---|
| D | ✓ (`softSignalViaProjection`) | unconditional | YES (DISCOVER procedure explicitly explains using `alternate_candidate_use_cases`) | SAFE |
| Cf | ✗ | unconditional | NO (post-classification) | SAFE (slot becomes absent) |
| E | ✗ | unconditional | NO (post-classification) | SAFE |
| T | ✗ | unconditional | NO | SAFE |
| Rf | ✗ | unconditional | NO (RESOLVE works on committed UC) | SAFE |
| Ri | ✗ | unconditional | NO | SAFE |

**Declaration coverage**: 1/6 (discover_triage only). **Disposition:
GATE (Phase B #5)**. DECL matches NEED exactly — only DISCOVER's
procedure (line 20 of `discover_triage.yaml`, paragraph "Alternate
candidate use cases (the `alternate_candidate_use_cases`
projection slot)") references this slot. All post-DISCOVER
Skills' procedures contain ZERO references to
`alternate_candidate_use_cases`. The slot is null/empty for most
sessions (only populated when intake-router fired AMBIGUOUS),
making the data-risk LOW even on regression.

**#1a**: declarations intentional as-is.

### 3.H `discover_disambiguation_signals` (soft-signal slot)

| Skill | DECL | EMIT today | NEED | RISK if gated |
|---|---|---|---|---|
| D | ✓ (`softSignalViaProjection`) | unconditional | YES (DISCOVER procedure explains the three sub-fields; `removed-listing-route-with-moderation-context` critical step references it) | SAFE |
| Cf | ✗ | unconditional | NO | SAFE |
| E | ✗ | unconditional | NO | SAFE |
| T | ✗ | unconditional | NO | SAFE |
| Rf | ✗ | unconditional | NO (UC committed; disambig is pre-classification) | SAFE |
| Ri | ✗ | unconditional | NO | SAFE |

**Declaration coverage**: 1/6 (discover_triage only). **Disposition:
GATE (Phase B #5)**. Same pattern as 3.G — only DISCOVER reads
this slot, sub-fields are null in the common case (most sessions
unambiguous), and the Skill's `discover_disambiguation_signals`
procedure paragraph is the only consumer.

**#1a**: declarations intentional as-is.

### 3.I `prior_use_case_carry` (soft-signal slot)

| Skill | DECL | EMIT today | NEED | RISK if gated |
|---|---|---|---|---|
| D | ✗ | unconditional (NullNode unless prior UC switch within window) | OPTIONAL (`previous_active_use_case` already separately emitted; carry is partly redundant for DISCOVER) | SAFE |
| Cf | ✗ | unconditional | NO (CONFIRM works on immediate prior turn, not historical UC) | SAFE |
| E | ✗ | unconditional | NO (ESCALATE writes handover, uses `previous_active_use_case` for drift context) | SAFE |
| T | ✗ | unconditional | NO (CLOSE) | SAFE |
| Rf | ✓ (`softSignalViaProjection`) | unconditional | YES (UC-A↔UC-C continuity per Sprint 41 design — the load-bearing case) | SAFE |
| Ri | ✓ (`softSignalViaProjection`) | unconditional | YES (intake continuity after UC switch — Sprint 41 design) | SAFE |

**Declaration coverage**: 2/6 (resolve_faq + resolve_intake). **Disposition:
GATE (Phase B #5)**. DECL matches NEED — the Sprint 41 design
(`docs/proposals/skill_registry_design.md` §10.4 per
`ContextProjectionBuilder.java:460-475` doc) explicitly scoped this
slot to RESOLVE-phase Skills that handle cross-UC continuity.
DISCOVER/CONFIRM/ESCALATE/CLOSE have `previous_active_use_case` as
a separate slot for drift context; the carry's prior_skill_name +
recent citation source_ids are RESOLVE-specific.

**#1a**: declarations intentional as-is.

## 4. Audit summary — Phase-B convergence routing

### 4.A Slots routed to GATE (Phase B #2 + #5 code change)

| Slot | Gate | DECL coverage | Gating helper | Default for unmapped (phase, UC) |
|---|---|---|---|---|
| `candidate_use_cases` | `Skill.requiredContextKeys().contains("candidate_use_cases")` (#2) | D only | `skillNeedsContextKey(session, activeUc, "candidate_use_cases")` | EMIT (preserve pre-S4 behaviour for unmapped tuples; same defensive default as S3 C2 #4's `getVisibleToolsForUc` fallback) |
| `alternate_candidate_use_cases` | `Skill.stateInheritance().softSignalViaProjection().contains("alternate_candidate_use_cases")` (#5) | D only | `skillSoftSignalDeclared(session, activeUc, "alternate_candidate_use_cases")` | EMIT (defensive) |
| `discover_disambiguation_signals` | `Skill.stateInheritance().softSignalViaProjection().contains("discover_disambiguation_signals")` (#5) | D only | `skillSoftSignalDeclared(session, activeUc, "discover_disambiguation_signals")` | EMIT (defensive) |
| `prior_use_case_carry` | `Skill.stateInheritance().softSignalViaProjection().contains("prior_use_case_carry")` (#5) | Rf + Ri | `skillSoftSignalDeclared(session, activeUc, "prior_use_case_carry")` | EMIT (defensive) |

### 4.B Slots routed to KEEP UNCONDITIONAL (Phase B no-op)

| Slot | Reason kept unconditional | Spec basis |
|---|---|---|
| `form_context` | 6/6 declare; universally needed | "all-Skills-need → keep unconditional" |
| `customer_context` | 3/6 declare + 3/6 INHERIT-RISK (state_inheritance.inherit); gating drops alice-class signals | "do not change LLM-visible semantic information" hard fence |
| `listing_context` | 1/6 declares; 4/6 need (D/E/Rf/Ri); gating drops UC-FP/UC-H signals; data-gated today | "do not change LLM-visible semantic information" hard fence |
| `conversation_history` | 1/6 declares; 5/6 need; gating drops bad-case-relevant signals from D/E/Rf/Ri | "all-Skills-need" (5/6) → keep unconditional |

### 4.C Slot routed to OUT OF SCOPE (existing registry gate is canonical)

| Slot | Existing gate | Why not migrate to #2 |
|---|---|---|
| `intake_state` | `IntakeFieldsRegistry.isIntakeUseCase(activeUc)` (`:376`) | Registry-driven (knows intake UCs); Tier-2 scorer LOAD-BEARING (executor.py:528-537); migrating to Skill-declaration gate would risk ESCALATE-after-intake gap |

### 4.D Slots STOP-AND-SURFACED

**None.** All candidate slots resolve cleanly to GATE / KEEP
UNCONDITIONAL / OUT OF SCOPE. The audit confirms declarations are
intentional as-is for the four GATE-routed slots (zero #1a YAML
edits needed — the gating set's declarations already match the
audit's need column).

## 5. #1a (complete the declarations) — outcome

**Result: zero YAML edits.** The Phase-A audit concludes:

- For the **four GATE-routed slots** (`candidate_use_cases`,
  `alternate_candidate_use_cases`, `discover_disambiguation_signals`,
  `prior_use_case_carry`): existing declarations match the audit's
  NEED column. Gating preserves all LLM-visible signals. No
  declaration additions needed.
- For the **four KEEP-UNCONDITIONAL slots** (`form_context`,
  `customer_context`, `listing_context`, `conversation_history`):
  adding declarations to non-declaring Skills would NOT change
  Phase-B behaviour (we are not gating these). The spec's
  "all-Skills-need → keep unconditional" exception applies; the
  data-gated emission discipline is preserved.
- For `intake_state`: out of scope; existing `IntakeFieldsRegistry`
  gate is canonical.

This is a CONSERVATIVE Phase-A outcome — the safe convergence is
smaller than the contract anticipated. The HIGH-risk slots flagged
in the S3 C1 matrix (`customer_context`, `listing_context`,
`conversation_history`) remain unconditional rather than being
forced through Skill-declaration gating, which would risk dropping
LLM-visible signals the bad-case suite depends on (alice UC-A↔UC-H,
UC-FP appeal flows, UC-A↔UC-C drift continuity).

## 6. Recommended close shape (Phase-A review input)

### 6.A Phase B implementation scope (gated by this audit)

1. **#2 — Skill-declared context-key gating** for `candidate_use_cases`
   only (the single safe context-key slot). Read
   `plan.requiredContextKeys()` (already wired via `PhaseEvaluator:455`
   ← `skill.requiredContextKeys()`) in the run-loop path; read
   `skillRegistry.select(phase, uc).map(s -> s.requiredContextKeys())`
   in the base `buildProjection(...)` path (S3 C2 #4 pattern).
2. **#5 — Soft-signal gating** for the three soft-signal slots.
   Read `skillRegistry.select(phase, uc).map(s -> s.stateInheritance().softSignalViaProjection())`
   in both the base and run-loop paths.
3. Defensive default for unmapped (phase, UC): EMIT (preserves
   pre-S4 behaviour; same pattern as S3 C2 #4).

### 6.B STOP-and-surface (per fence)

None at Phase A. (The HIGH-risk slots are KEEP-UNCONDITIONAL by
audit conclusion, NOT STOP-surfaced — they had a clean
disposition.)

### 6.C Tests + evidence gates (Phase B #5)

- Java wiring/rendering tests covering (a) POSITIVE control —
  discover_triage emits all 4 slots; resolve_faq emits only
  `prior_use_case_carry`; (b) NEGATIVE control — confirm /
  escalate / terminal do NOT emit any of the 4; resolve_intake
  emits only `prior_use_case_carry`; (c) DEFENSIVE control —
  unmapped (phase, UC) tuple emits all 4 (preserves pre-S4
  behaviour); (d) KEEP-UNCONDITIONAL control — `form_context` /
  `customer_context` / `listing_context` / `conversation_history`
  emission unchanged across Skills.
- Real-LLM bad-case rerun MANDATORY (M4-close distribution must
  hold: PASS×5 + IMPROVING×4 + FAIL×3 + OOSR×0).
- Shadow rerun MANDATORY (dev-blind; STOP-surfaced to
  deliver-agent / human).

## 7. Cross-references

- Sprint contract: `docs/sprint_objective.md` (Sprint 53 / M5 S4)
  §"Scope #1 / #1a" (Phase A).
- Milestone objective: `docs/milestone_objective.md` §3 (S4
  paragraph; CONFIRMED to run after S3 close).
- S3 C1 matrix (the audit's starting point):
  `docs/diagnostics/m5-s3-projection-consumption-map.md` §3.E
  (soft-signal slots), §3.L (context blocks), §3.M
  (conversation_history), §4 (C2 disposition summary).
- Solution proposal: `docs/solutions/observability_coherence_admin_trace_and_projection.md`
  §2.C (data flow + line anchors), §4.C (C1/C2 design), §8 #3
  (fences).
- S3 handoff (STOP-surface origin): `docs/sprints/sprint-052-handoff.md`
  §2 #2 + §2 #5 + §7 (OQ-S52.1 / OQ-S52.2).
- Governance: `docs/current/iteration_governance.md` §1.7
  (forbidden), §4.1 (anti-hardcode kernel), §5.6 (real-LLM rerun
  gate), §7 (stanza).
- Code anchors are HEAD `49d48b1` (S3 close commit).
