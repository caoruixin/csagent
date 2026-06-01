---
title: Action Bank — Closed Index (pointers only)
doc_tier: durable-connective
status: current
source_of_truth: docs/sprints/ and docs/milestones/ (this file is a pointer index, not a record)
last_reviewed: 2026-06-01
review_cadence: append-only at each milestone close (see action_bank.md §7.1)
notes: >
  Compact pointer index for closed work relocated out of docs/action_bank.md.
  NO prose is copied here — every row points to the authoritative sprint /
  milestone archive. Historical `action_bank.md:<line>` citations in immutable
  archives refer to the pre-migration file at git 17991c6 (use `git show 17991c6:docs/action_bank.md`).
---

# Action Bank — Closed Index

Relocated from `docs/action_bank.md` at git `17991c6` (2026-06-01). This is an
index of pointers; the authoritative detail lives in the linked archives.

## A. Closed per-sprint index

| sprint | deliverable / code | status | archive |
|--------|--------------------|--------|---------|
| Sprint 1 | A1 / A2 / A3 | closed | docs/sprints/sprint-001-* |
| Sprint 2 | B0 / B1 / B2 / B3 | closed | docs/sprints/sprint-002-* |
| Sprint 2.1 | cs014 CaseSpec correction (override path | closed | docs/sprints/sprint-002-* |
| Sprint 3 | C0 / C1 / C2 | closed | docs/sprints/sprint-003-* |
| Sprint 4 | E1 / E2 / E3 | closed | docs/sprints/sprint-004-* |
| Sprint 5 | F0 / F1 / F2 / F3 (diagnostic only) | closed | docs/sprints/sprint-005-* |
| Sprint 6 | G0 / G1 / G2 | closed | docs/sprints/sprint-006-* |
| Sprint 6.1 | H0 / H1 closure-normalisation | closed | docs/sprints/sprint-006-* |
| Sprint 7 | I0 / I1 / I2 | closed | docs/sprints/sprint-007-* |
| Sprint 7.1 | J0 partial intake persistence | closed | docs/sprints/sprint-007-* |
| Sprint 8 | K0 cs259 active-use-case contract | closed | docs/sprints/sprint-008-* |
| Sprint 8.2 | M0a resolve_article source_id + M0b max- | closed | docs/sprints/sprint-008-* |
| Sprint 9 | O0 record_outcome + request_handover con | closed | docs/sprints/sprint-009-* |
| Sprint 9.1 | trace sanitization closure (failed-tool  | closed | docs/sprints/sprint-009-* |
| Sprint 10 | L0 RuntimeIntentClassifier + RerouteDeci | closed | docs/sprints/sprint-010-* |
| Sprint 11 | M0 minimal same-UC task/entity state; M1 | closed | docs/sprints/sprint-011-* |
| Sprint 12 | N0 drift/task/phase observability harden | closed | docs/sprints/sprint-012-* |
| Sprint 13 | O0 runtime freeze decision + risk taxono | closed | docs/sprints/sprint-013-* |
| Sprint 14 | L0 KB canonical URL / Help URL / publish | closed (initial review fix_required → resolved | docs/sprints/sprint-014-* |
| Sprint 14.1 | FAQ grounding observability persistence  | closed (Codex pass) | docs/sprints/sprint-014-* |
| Sprint 15 | M0 externalize DriftDetector / risk keyw | closed (Codex pass) | docs/sprints/sprint-015-* |
| Sprint 16 | H0 define handover exactly-once contract | closed (docs + characterization-test sprint; n | docs/sprints/sprint-016-* |
| Sprint 17 | G0.1 extend `docs/current/iteration_gove | closed (Codex pass; docs-only governance sprin | docs/sprints/sprint-017-* |
| Sprint 18 | G1 Human-led Failure Portfolio — 10 Fail | closed (docs-only governance; no Codex review  | docs/sprints/sprint-018-* |
| Sprint 19 | A + B parallel investigation — Track A s | closed (Codex pass; investigation-only sprint  | docs/sprints/sprint-019-* |
| Sprint 20 | G2 Interactive Case Family + Shadow Spli | closed (substantive findings closed; packaging | docs/sprints/sprint-020-* |
| Sprint 21 | Wave A5/A6 L3 Review Batch (per-case + 1 | closed (A-with-evidence-gap-acknowledgment; su | docs/sprints/sprint-021-* |
| Sprint 22 | phase 2 line 358 reconciliation + R-item | closed (docs-only governance) | docs/sprints/sprint-022-* |
| Sprint 23 | Track A bundle `R-already-called-prompt- | closed (Codex fix re-review pass; PASS branch  | docs/sprints/sprint-023-* |
| Sprint 24 | Slow-LLM Placeholder Coalesce + Coarse L | closed (Codex sprint-close pass on first revie | docs/sprints/sprint-024-* |
| Sprint 25 | Per-LLM-call latency instrumentation — s | closed | docs/sprints/sprint-025-* |
| Sprint 26 | Latency decision sprint — consumes Sprin | closed (A-with-Codex-skipped; decision (C) Acc | docs/sprints/sprint-026-* |
| Sprint 27 | PhasePlan directive-shape probe — invest | closed (A-with-Codex-skipped; recommendation ( | docs/sprints/sprint-027-* |
| Sprint 28 | Per-case trace dump for smoke harness —  | closed (A — Clean close; Codex sprint-close `p | docs/sprints/sprint-028-* |
| Sprint 31 | alternate_candidate_use_cases projection | closed (A-with-fix-iteration-investigation + A | docs/sprints/sprint-031-* |
| Sprint 36 | Skill foundation + UC-switching continui | closed (A — Clean close; Codex per-sub-sprint  | docs/sprints/sprint-036-* |
| Sprint 37 | Skill Registry + state-across-Skill desi | closed (A — Clean close; Codex per-sub-sprint  | docs/sprints/sprint-037-* |
| Sprint 38 | SkillRegistry core + 4 simpler phase Ski | closed (A — Sprint 38 closes B-fix-iterated; C | docs/sprints/sprint-038-* |
| Sprint 39 | RESOLVE_FAQ + RESOLVE_INTAKE Skill migra | closed (A — Clean PASS; Codex per-sub-sprint r | docs/sprints/sprint-039-* |
| Sprint 40 | Teaching extraction from `system_prompt. | closed (A — Clean PASS; Codex per-sub-sprint r | docs/sprints/sprint-040-* |
| Sprint 41 | UC switch + state preservation across Sk | closed (A — Clean PASS; Codex per-sub-sprint r | docs/sprints/sprint-041-* |
| Sprint 32 | alternate_candidate_use_cases case famil | closed (A-with-investigation-finding; first in | docs/sprints/sprint-032-* |
| Sprint 42 | Schema simplification + outcome-only anc | closed (A — Clean PASS; Codex deferred to M3-E | docs/sprints/sprint-042-* |
| Sprint 43 | Skill `critical_steps` schema + extracto | closed | docs/sprints/sprint-043-* |
| Sprint 44 | Populate `critical_steps` for the 6 Skil | closed (A — Clean PASS; **Codex per-sub-sprint | docs/sprints/sprint-044-* |
| Sprint 45 | Bad-case suite expansion + trial milesto | closed | docs/sprints/sprint-045-* |
| Sprint 46 | L3 judge repositioning + R-item closure  | closed (A — Clean PASS; **bundled with M3-Eval | docs/sprints/sprint-046-* |
| Sprint 47 | S-Cleanup-1 (NEW M4-Eval-Cleanup sub-spr | closed (A — Clean PASS; deliver-agent + human  | docs/sprints/sprint-047-* |
| Sprint 48 | S-Cleanup-2 (NEW M4-Eval-Cleanup sub-spr | closed (A — Clean PASS; deliver-agent + human  | docs/sprints/sprint-048-* |
| Sprint 49 | S-Cleanup-3 (M4-Eval-Cleanup sub-sprint  | closed (A — Clean PASS; LAST M4-Eval-Cleanup s | docs/sprints/sprint-049-* |
| Sprint 50 | S1 (NEW M5 — Observability Coherence sub | closed (A — Clean PASS 2026-05-24; deliver-age | docs/sprints/sprint-050-* |
| Sprint 51 | S2 (M5 — Observability Coherence sub-spr | closed (A — Clean PASS 2026-05-25; deliver-age | docs/sprints/sprint-051-* |
| Sprint 52 | S3 (M5 — Observability Coherence sub-spr | closed (A — Clean PASS 2026-05-25; Codex per-s | docs/sprints/sprint-052-* |
| Sprint 53 | S4 (M5 — Observability Coherence sub-spr | closed (A — Clean PASS 2026-05-25; Codex combi | docs/sprints/sprint-053-* |
| Sprint 54 | S-Auto-1 (NEW M-Auto-1A — Auto-Evolution | closed | docs/sprints/sprint-054-* |
| Sprint 55 | S-Auto-2 (M-Auto-1A — Auto-Evolution Bui | closed (A — Clean PASS 2026-05-27; deliver-age | docs/sprints/sprint-055-* |
| Sprint 56 | S-Auto-3 (M-Auto-1A — Auto-Evolution Bui | closed (A — Clean PASS 2026-05-27; deliver-age | docs/sprints/sprint-056-* |
| Sprint 57 | S-Auto-4 (M-Auto-1A — Auto-Evolution Bui | closed (A — Clean PASS 2026-05-27; deliver-age | docs/sprints/sprint-057-* |
| Sprint 58 | S-Auto-5 (NEW M-Auto-1B — Auto-Evolution | closed | docs/sprints/sprint-058-* |
| Sprint 59 | S-Auto-6 (M-Auto-1B — Auto-Evolution Cal | closed | docs/sprints/sprint-059-* |
| Sprint 60 | S-Auto-7 (M-Auto-1B — Auto-Evolution Cal | closed (B — Surfaced findings need fix-iterati | docs/sprints/sprint-060-* |
| Sprint 61 | S-Auto-7.1 (M-Auto-1B — Auto-Evolution C | closed (A — Clean PASS at sub-sprint level 202 | docs/sprints/sprint-061-* |
| Sprint 62 | S-Auto-7.2 (M-Auto-1C — Auto-Evolution C | closed (Class A — Clean close 2026-05-30 per C | docs/sprints/sprint-062-* |
| Sprint 63 | S-Auto-8 (M-Auto-1C — Auto-Evolution Cal | closed (PARTIAL — Class C-style in-flight down | docs/sprints/sprint-063-* |
| Sprint 64 | S-Auto-9 (M-Auto-2 — Local-Mac OQ-S62.3  | closed (Class A — Clean PASS at sub-sprint lev | docs/sprints/sprint-064-* |
| Sprint 65 | S-Auto-10 (M-Auto-2 — Local-Mac OQ-S62.3 | closed (**REFRAMED at M-Auto-2 close 2026-06-0 | docs/sprints/sprint-065-* |
| Sprint 66 | S-Auto-11 (M-Auto-3 — Substrate-hygiene  | closed | docs/sprints/sprint-066-* |

## B. Closed milestone index

| milestone | archive |
|-----------|---------|
| M-Auto-2 — Local-Mac OQ-S62.3 diagnostic + first ove | docs/milestones/M-Auto-2_objective.md |
| M1 — DISCOVER + Intake | docs/milestones/M1_objective.md |
| M2 — Skill Registry Abstraction + Wholesale Retroact | docs/milestones/M2_codex-review.md |
| M3-Eval — Coarse-to-Fine Evaluation Architecture | docs/milestones/M3-Eval_objective.md |
| M4-Eval-Cleanup — Evaluation harness + governance ga | docs/milestones/M4-Eval-Cleanup_objective.md |
| M5 — Observability Coherence | docs/milestones/M5_objective.md |
| M-Auto-1A — Auto-Evolution Build | docs/milestones/M-Auto-1A_objective.md |
| M-Auto-1C — Auto-Evolution Calibration Continuation | docs/milestones/M-Auto-1C_objective.md |
| M-Auto-1B — Auto-Evolution Calibration | docs/milestones/M-Auto-1B_objective.md |

## C. Closed R-item index

| id | closed-by | close date | archive pointer |
|----|-----------|------------|-----------------|
| R-generator-get-customer-context-policy-mismatch | Sprint 21–22 | 2026-05-14 | docs/sprints/sprint-021-* + sprint-022-* |
| R-l3-judge-form-context-trust-rubric | Sprint 46 (S-Eval-5) | 2026-05-22 | docs/sprints/sprint-046-* |
| R-cs001-escalation-trigger-l3-review | Sprint 21 | — | docs/sprints/sprint-021-* |
| R-cs038-l3-review-intake-efficiency | Sprint 46 (S-Eval-5) | 2026-05-22 | docs/sprints/sprint-046-* |
| R-cs040-l3-review-intake-completion-semantics | Sprint 46 (S-Eval-5) | 2026-05-22 | docs/sprints/sprint-046-* |
| R-cs095-uc-classification-l3-rereview | Sprint 21 | — | docs/sprints/sprint-021-* |
| R-l1-source-citation-quality-rubric | Sprint 46 (S-Eval-5) | 2026-05-22 | docs/sprints/sprint-046-* |
| R-cs176-escalation-reason-l3-review | Sprint 21 | — | docs/sprints/sprint-021-* |
| R-cs192-secondary-ucs-duplicate-uc-b | Sprint 21 | — | docs/sprints/sprint-021-* |
| R-prompt-projection-already-called-soft-signal | Sprint 20 | — | docs/sprints/sprint-020-* |
| R-per-case-trace-dump-for-smoke-harness | Sprint 28 | 2026-05-14 | docs/sprints/sprint-028-* |
| R-already-called-prompt-consumption | Sprint 23 | — | docs/sprints/sprint-023-* |
| R-phase2-uc-cdf-customer-context-policy-widen | Sprint 22 | 2026-05-14 | docs/sprints/sprint-022-* |
| R-slow-llm-placeholder-coalesce-honest-next-step | Sprint 24 | — | docs/sprints/sprint-024-* |
| R-per-llm-call-latency-instrumentation | Sprint 25 | — | docs/sprints/sprint-025-* |
| R-alternate-uc-signal-data-source | Sprint 31 | 2026-05-16 | docs/sprints/sprint-031-* |
| R-option-beta-coverage-gap-uc-a-uc-c-shape | Sprint 35 | 2026-05-17 | docs/sprints/sprint-035-* |
| R-grounding-discipline-iterative-search-fabrication | Sprint 39 | 2026-05-18 | docs/sprints/sprint-039-* |
| R-stale-test-escalation-enum-sync-v0_2-to-v0_3-migration | Sprint 47 (S-Cleanup-1) | 2026-05-23 | docs/sprints/sprint-047-* |
| R-bad-case-fixture-migrate-to-l3-judge-dims | Sprint 48 (S-Cleanup-2) | 2026-05-23 | docs/sprints/sprint-048-* |
| R-eval-report-observability | Sprint 50 (M5 S1) | 2026-05-24 | docs/sprints/sprint-050-* |
| R-S57-anti-hardcode-whenever-arrow-synonym-bypass | Sprint 58 (S-Auto-5) | 2026-05-28 | docs/sprints/sprint-058-* |
| R-S58-anti-hardcode-zero-width-when-arrow-bypass | Sprint 63 (S-Auto-8) | 2026-05-30 | docs/sprints/sprint-063-* |

