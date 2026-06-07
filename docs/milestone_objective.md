---
title: Milestone M-Auto-7 — Autoloop readiness and CS4 entity-context pilot
doc_tier: current-runtime
status: current
implementation_status: not_started
source_of_truth: this file
last_reviewed: 2026-06-08
review_cadence: per milestone
supersedes: []
superseded_by: null
notes: >
  North star is AUTOLOOP READINESS, validated by running the CS4
  entity-context autoloop pilot end-to-end — NOT completing every
  bad-case semantic fix. Two phases inside one milestone:
    Phase 1 (autoloop launch blockers): CS1 measurement honesty,
      CS3 DISCOVER runtime hygiene, CS4 projection infra, CS4
      executable CaseSpecs.
    Phase 2 (CORE GATE — the pilot): autoloop authors a skill-yaml
      procedure candidate against the CaseSpecs on the honest
      baseline; human reviews under §4.1; accepted candidate merged
      + re-blessed.
  CS2-original / perspective grounding is additive back-half work
  (or a follow-on sub-sprint); it is explicitly NOT an autoloop
  pilot launch blocker and NOT part of the core acceptance bar.
  Source proposals (single coherent input):
    - docs/solutions/2026-06-07-cs1-default-resolved-cs2-user-role-projection-gap.md
    - docs/solutions/2026-06-07-cs3-cs4-discover-stall-uc-fp-boundary-empty-trace-ux.md
    - docs/solutions/2026-06-08-cs4-casespec-drafts-appendix.md
  Launch precondition (M-Auto-6 fully CLOSED 2026-06-07: S-Auto-28
  Definition-of-done + milestone-shared re-bless + Codex §4.3) is MET.
  Autoloop capability spike RESOLVED READY (CS3/CS4 §3.5) — no tooling
  pre-req for the pilot. D-full-issue-ledger-light (CS3/CS4 §10.4) is
  NOT in this milestone (post-close research workstream). Replaces the
  post-M-Auto-6 M-Auto-7-candidate-selection placeholder.
---

# Milestone M-Auto-7

## 1. Milestone class

| Sub-sprint | Phase | Layer (§3.2) | §7 stanza | Per-sub-sprint Codex (§4.3) |
|---|---|---|---|---|
| S-A (CS1) | Blocker | infra (trace-contract) | EXEMPT | EXEMPT → milestone close |
| S-X (CS3) | Blocker | infra + prompt_projection | REQUIRED | defer → milestone close |
| S-Y1 (CS4 readiness: Part A+B) | Blocker | prompt_projection (A) + eval_spec (B) | REQUIRED | defer → milestone close; Part B gated by §5.6 human tiering |
| **S-Y2 (CS4 pilot: Part C+D)** | **CORE GATE** | semantic_planner (autoloop-authored) + eval_spec/governance | REQUIRED | **REQUIRED** — autoloop output is the §1.7 binding gate (§4.3 trigger #2) |
| S-B (CS2-original) | Back-half (non-blocking, optional) | prompt_projection + thin soft cue | REQUIRED | via milestone-shared close |

Milestone-level §7: stanzas pre-filled per sub-sprint (proposal §4.1 /
§7.1 / §7.2 + CS3/CS4 §4.1 / §4.2). S-A is the only §7-EXEMPT slice.

## 2. Goal

The north star is **autoloop readiness for skill-yaml procedure
optimization, proven by running the CS4 entity-context pilot
end-to-end** — not finishing every bad-case semantic fix.

1. **Clear the four autoloop launch blockers** so the autoloop can run
   against an honest baseline with the projection slots and CaseSpecs
   it needs: CS1 (stop false-crediting `resolved` on CLOSE), CS3 (stop
   the synthesised DISCOVER placeholder burning the clarification
   budget), CS4 Part A (projection infra: `moderation_reason_available`
   + candidate-UC human-readable names), CS4 Part B (executable
   CaseSpecs from the appendix).
2. **Run the CS4 entity-context autoloop pilot (CORE GATE):** autoloop
   proposes skill-yaml procedure candidate(s) against the Part B
   CaseSpecs on the post-CS1/CS3 honest baseline; human reviews each
   under the §4.1 nine-question kernel; an accepted candidate is merged
   + re-blessed.
3. **CS2-original (perspective grounding) is additive back-half work**,
   NOT a pilot launch blocker and NOT in the core acceptance bar; it may
   close with M-Auto-7 if appetite remains, else carry to a follow-on.

## 3. Sub-sprint sequence

| # | Phase | Sub-sprint | R-item | Scope (3-sentence) | Depends on |
|---|---|---|---|---|---|
| 1 | Blocker | **S-A (CS1)** Sprint 084 / S-Auto-29 | `R-controlkernel-default-resolved-on-close-anti误杀` | Gate `ControlKernel.java:575-579` Path B CLOSE-arm resolved-stamp through `isResolvedSuccessTerminal`; leave containment null when no grounding; preserve handlingState/escalation/Path-D. 5 characterization tests; no enum value. | M-Auto-6 close (MET) |
| 2 | Blocker | **S-X (CS3)** Sprint 085 / S-Auto-30 | `R-discover-null-turn-counter-anti误杀` | Exclude the runtime-synthesised placeholder from the R2.a counter at `AgentRunLoopImpl.java:438-458`; add one §1.3-soft DISCOVER cue against null/filler turns; emit a `discover_null_turn_synthesised` trace event. | S-A close + mini re-bless |
| 3 | Blocker | **S-Y1 (CS4 readiness)** Sprint 086 / S-Auto-31 | `R-uc-a-entity-context-verify-procedure-via-autoloop` (readiness scope) | Part A: human projection infra (`moderation_reason_available` boolean + candidate-UC names + soft_signal_via_projection wiring). Part B: author appendix §2-§8 CaseSpecs (5 NEW + 2 EXTEND), reconcile placeholders to real fixtures, §5.6 tiering. NO procedure-text change (that is the pilot). | S-X close + mini re-bless |
| 4 | **CORE GATE** | **S-Y2 (CS4 pilot)** Sprint 087 / S-Auto-32 | `R-uc-a-entity-context-verify-procedure-via-autoloop` (pilot scope) | Part C: baseline run (targets fail / negative-control passes) → autoloop authors skill-yaml procedure candidate(s) → human §4.1 review → merge accepted candidate → re-run. Part D: mini re-bless + per-sub-sprint Codex + close. Fallback (§3.4) only if no candidate is acceptable. | S-Y1 close |
| 5 | Back-half (optional, non-blocking) | **S-B (CS2-original)** Sprint 088 / S-Auto-33 | `R-user-role-projection-slot-from-listing-ownership` | Data-derived `user_role` slot (`ad_owner \| unknown`) from form-email↔listing-`posted_by` match; one-sentence soft cue into discover_triage + resolve_faq. Reuses S-Y1 candidate-UC-name infra. | S-Y2 close + mini re-bless (additive) |

## 4. Non-goals (explicit)

- **CS2-original is NOT an autoloop pilot launch blocker** and NOT part
  of the core acceptance bar. It is additive back-half work; it may
  carry to a post-M-Auto-7 follow-on at the human's discretion.
- **D-full-issue-ledger-light** (cross-UC stash-and-resume, CS3/CS4
  §10.4) — post-close research-only workstream.
- **CS2-new empty-trace UX** — not a defect; P3 admin backlog only.
- **Not consumed by M-Auto-7** (stay backlog):
  `R-r5-citation-result-binding-grounding-strengthening`,
  `R-standalone-reconcile-entry-gate-test`,
  `R-docs-reconciliation-faq-grounding-contract-vs-must-cite-source`.
- No framework / CaseSpec-schema extension (Part B uses Path γ).
- No `containment_outcome` enum addition (C1.B deferred); no
  answer-repetition bucket (C4.D deferred). M-Auto-4 stays PAUSED.

## 5. Milestone acceptance bar

### 5.1 PRIMARY (core gate) — the CS4 entity-context autoloop pilot

The milestone closes on a **successful pilot run**, measured on the
post-CS1/CS3 honest baseline:

1. **Pilot launched + run to completion** against the Part B CaseSpecs.
2. **Baseline run (C.1)** confirms the gap is real: Tier-1 targets
   (`cs_uc_a_no_ad_id_ad_specific`, `cs_uc_a_loaded_listing`,
   `cs_uc_a_lookup_failed`) fail; the anti-误杀 negative-control
   (`cs_uc_a_generic_policy_question`) passes.
3. **Autoloop produces ≥1 skill-yaml procedure candidate**; human
   reviews each under the §4.1 nine-question kernel + §1.7 (reject any
   semantic hardcode — per-UC if-else, user_message keyword check,
   enum widening, Java guard).
4. **SUCCESS:** an accepted candidate, post-merge + re-bless, flips the
   three Tier-1 targets to PASS while the negative-control STAYS
   passing and the Tier-2 neighbor (`cs_uc_fp_loaded_moderation`)
   improves; the 2 EXTEND cases (`alice_uc_a_uc_h_misclass`,
   `wmkb_uc_a_trader_flag_secondary_uc_h`) stay green.
5. **VALID ALTERNATE OUTCOME:** if no autoloop candidate is acceptable,
   the §3.4 fallback (deliver/dev hand-authors the procedure text; same
   CaseSpec gate; same §4.1 review) is invoked in Part D — the pilot
   still produces a go/no-go + a merged, gated procedure. The pilot
   RUNNING is the gate; the candidate flip is the success metric.

### 5.2 Blocker gates (prerequisites)

- **CS1** — anchor trace `3e4f0aad-af7…`: DISCOVER→CLOSE 0-tool
  no-grounding is NOT stamped `resolved`; goal_achieved one-shot
  anti-误杀 preserved (Java characterization tests + manual trace
  review).
- **CS3** — anchor trace `33edc1eb-15d…`: synthesised placeholder does
  not increment the clarification counter; turn-3 reaches the LLM (no
  premature force-escalate); legitimate Sprint-33 clarification still
  counts.
- **CS4 readiness** — projection slots render in the "Projected
  Context" panel; 5 NEW + 2 EXTEND CaseSpecs authored, compile against
  `eval_interactive/eval_interactive/specs/schema.py`, pass §5.6
  tiering, placeholders reconciled to real fixtures.

### 5.3 Additive (NOT in the acceptance bar)

- **CS2-original** — anchor trace `89f4ab98…` + new bad case
  `cs_seller_buyer_perspective_001`: ad_owner framing holds;
  ambiguous → one clarifying question; UC-A baseline no regression.
  Only if run as back-half.

Safety floor + grounding floor unchanged across the milestone (HARD
gate). Documented expected shift: CS1 lowers the resolved-stamp count
(measurement honesty, not regression).

## 6. Hard fences

**Milestone-wide (all sub-sprints):**
- No keyword / regex / if-else / enum expansion on a semantic surface
  (§1.5 / §1.7).
- No eval-side CaseSpec widening to mask a genuine bot mistake (§5.4).
- No editing of `docs/sprints/*`, `docs/archive/*`, `docs/milestones/*`.
- No system_prompt edits to encode any CS fix.
- No Java guard on `user_message` content for any of the four.

**S-A (CS1):** no new `containment_outcome` enum value (C1.B deferred);
no eval-side composite-gate edit; no touch to `isResolvedSuccessTerminal`
/ `shouldVoidResolvedStamp` internals.

**S-X (CS3):** do NOT raise `max-clarification-rounds`; do NOT edit the
shared placeholder string in `templates.yaml` / `PhaseEvaluator` /
`ActionParser` (used on legitimate slow-LLM paths); do NOT touch the
pre-LLM budget gate at `ControlKernel.java:288-318`; the DISCOVER cue
is §1.3-soft, no hard guard on empty user_message.

**S-Y1 / S-Y2 (CS4):** no per-UC if-else on user_message; no Java guard
blocking `classify_use_case(UC-A)` on removed ads; do NOT project the
raw `moderation_reason_text` (only the boolean `moderation_reason_
available`); candidate-UC name field is additive (no UC-id
rename/migration); autoloop output is a proposal — human §4.1 review is
the binding gate; the negative-control CaseSpec is the anti-误杀 gate on
autoloop candidates.

**S-B (CS2-original):** `user_role.inferred_role` stays `ad_owner |
unknown` only (no `third_party_buyer` in v1); do NOT expose raw
`posted_by` email; role inference is data-derived only (no "my ad" /
"my buyer" keyword detector); soft cue is LLM-soft (no Java enforcement).

## 7. R-items consumed / surfaced

**Consumed (promoted from §5 candidate ledger):**
- `R-controlkernel-default-resolved-on-close-anti误杀` (S-A)
- `R-discover-null-turn-counter-anti误杀` (S-X)
- `R-uc-a-entity-context-verify-procedure-via-autoloop` (S-Y1 readiness
  + S-Y2 pilot — one R-item across two sub-sprints; primary = OBS-S1
  entity-context verification; secondary = OBS-S2 UC-A/UC-FP boundary
  cue)
- `R-user-role-projection-slot-from-listing-ownership` (S-B,
  non-blocking back-half)

**Surfaced (deferred):**
- `D-full-issue-ledger-light` (§4 deferred; post-close research trigger)
- `R-admin-empty-trace-zero-turn-affordance` (P3; CS2-new)

## 8. Codex review plan (§4.3)

Default = milestone-shared close review over the cumulative S-A..S-B
commit range (one §4.1 nine-question kernel walk + §4.2 header).
**Per-sub-sprint Codex REQUIRED for S-Y2 (the pilot)** — the
autoloop-authored procedure candidate is the §1.7 anti-hardcode binding
gate (§4.3 trigger #2); must complete before S-B begins (or before
milestone close if S-B carries out). S-A is §7-EXEMPT + §4.1-exempt
(pure infra). S-X / S-Y1 / S-B fold into the milestone-shared close.
Mini re-bless between sub-sprints is the proceed gate.

## 9. Estimated milestone duration

~3–4 calendar weeks (S-A 1-2 d; S-X 2-3 d; S-Y1 4-6 d; S-Y2 pilot
3-5 d; S-B 3-5 d; + re-bless cycles). Informational, not a gate.
