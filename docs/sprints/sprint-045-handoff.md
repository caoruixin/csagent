---
title: Sprint 45 (NEW M3-Eval sub-sprint 4, S-Eval-4) — Bad-case suite expansion + trial milestone-close dry-run — DEV HANDOFF
doc_tier: sprint-archive
status: current
implementation_status: implemented
source_of_truth: this file (dev-authored); deliver-agent + human append §12 closure verdict at close (Codex deferred to M3-Eval milestone-shared review per §4.3 default)
last_reviewed: 2026-05-22
review_cadence: per sub-sprint
supersedes: []
superseded_by: null
notes: >
  Sprint 45 / S-Eval-4 dev handoff. Ships 11 NEW bad-case YAML files
  under `eval_interactive/case_specs/bad_cases/` sourced from the 17
  approved entries in `eval_interactive/case_spec_overrides.yaml`,
  plus a lifecycle ledger append + a calibration note section append
  to `_manifest.md`, plus a planned S-Eval-1-checkpoint test update
  (§7-a drift). Total new bad-case suite size at close: 12 (1 Alice
  + 11 new). Real-LLM run posture at planning round: **Option A**
  (synthetic / mocked trace; deliver-agent + human authorized
  2026-05-22). UC-G/H/I/J primary-corpus gap surfaced at planning
  round: fallback path (a) authorized by deliver-agent + human —
  accept narrower coverage for this milestone; R-item candidate
  `R-bad-case-suite-uc-ghij-seed-from-real-sessions` opened as
  carry-over to M4+ planning.

  **Codex review plan**: milestone-shared at M3-Eval close per §4.3
  default. No per-sub-sprint trigger fires for S-Eval-4 (no Tier-0
  candidate; no §1.7 forbidden-list adjacent code; no fix-iteration
  on a prior sub-sprint; no hard-fence violation). Codex consumes
  S-Eval-1 through S-Eval-5 cumulatively at milestone close.

  **Bundle**: dev ships 13 files in ONE bundle commit per
  `feedback_commit_at_end_bundles_deliver_artefacts.md`. Dev does NOT
  stage deliver-agent close-out files (sprint_objective archive,
  10-handoff §1 lead refresh, codex-findings stays scaffold, action_bank
  Sprint 45 row).
---

# Sprint 45 (NEW M3-Eval sub-sprint 4, S-Eval-4) — Dev Handoff

## 1. Sprint identity

- **Sprint number**: 45.
- **Sub-sprint**: S-Eval-4 (fourth sub-sprint of M3-Eval; data + calibration deliverable per `docs/milestone_objective.md` §3).
- **Milestone**: M3-Eval (Coarse-to-Fine Evaluation Architecture).
- **Layer (per `iteration_governance.md` §3.2)**: `eval_spec` (data + calibration; not code). §7 stanza REQUIRED; filled at `docs/sprint_objective.md` §8 + reproduced in §6 below.
- **Codex review plan**: milestone-shared at M3-Eval close per §4.3 default. No per-sub-sprint trigger fires for S-Eval-4 (no Tier-0 candidate; no §1.7 forbidden-list adjacent code; no fix-iteration on a prior sub-sprint; no hard-fence violation). Codex consumes S-Eval-1 through S-Eval-5 cumulatively at milestone close.
- **HEAD baseline at sub-sprint start**: `4dafaf5` (`docs: S-Eval-3 close (A — Clean PASS) + S-Eval-4 launch + per-sub-sprint Codex pass archived`).

## 2. Summary

S-Eval-4 expands the curated bad-case suite from 1 case (Alice; the existing UC-A↔UC-H mis-classification regression guard) to **12 cases** (1 Alice + 11 new) by authoring 11 new bad-case YAML files sourced from the 17 approved entries in `eval_interactive/case_spec_overrides.yaml`. Each new case carries the standard CaseSpec schema + a `bad_case_metadata` block + a `closure_criterion` string per the §5.6 schema and the Alice precedent. Selection followed the §2.1 priorities under the deliver-agent + human-authorized fallback path (a) (accept narrower coverage; UC-G/H/I/J primary entries are 0 in the 17-pool — documented gap with new R-item candidate `R-bad-case-suite-uc-ghij-seed-from-real-sessions` opened as carry-over). The trial milestone-close manual review dry-run used **Option A** (synthetic / pre-recorded documented bot behaviour; authorized 2026-05-22) per §2.4 — the calibration deliverable is `closure_criterion` wording quality, NOT real-LLM verification of bot correctness. The dry-run produced per-case projected verdicts + wording assessments (appended to `_manifest.md` as the calibration note section); 11 of 11 new criteria are well-calibrated or acceptable-with-minor-note (1 minor `cs095` ambiguity flagged but kept as-is on the strength of the inline quoted anchor phrase). One S-Eval-1-anchored checkpoint test (`test_alice_bad_case_loads_unchanged` at `tests/test_s_eval_1_schema_and_scoring.py:352`) updated from `assert len(specs) == 1` to `assert len(specs) == 12` per the §7-a planned-drift precedent set by S-Eval-3 (Sprint 44 §3 / §10). The dev bundle contains 13 files: 11 new YAMLs + 1 manifest edit + 1 checkpoint-test edit. Java baseline `1163 / 1 / 0 / 2` UNCHANGED; Python baseline `5 failed / 396 passed` UNCHANGED from S-Eval-3 close (Codex independent reproduction).

## 3. Files shipped (per-file numstat)

Reproducible via `git show --numstat <commit>` after the bundle lands. Pre-commit numstat (from `git diff --numstat` + `wc -l` on new files):

### NEW bad-case YAMLs (11 files; new each)

| Path | Change | + | − | Notes |
|---|---|---:|---:|---|
| `eval_interactive/case_specs/bad_cases/cs012_uc_fp_late_phone_failure_path.yaml` | NEW | 79 | 0 | UC-FP / D1; tier=core; sourced from override `570Q5000008hx9tIAA` (cs_interactive_012). |
| `eval_interactive/case_specs/bad_cases/cs015_uc_fp_appeal_edit_repost.yaml` | NEW | 78 | 0 | UC-FP / D1; tier=core; sourced from override `570Q5000008WmXxIAK` (cs_interactive_015). |
| `eval_interactive/case_specs/bad_cases/cs066_uc_k_in_app_feature_regression.yaml` | NEW | 81 | 0 | UC-K / D1+D2; tier=core; sourced from override `570Q5000008fBsXIAU` (cs_interactive_066). |
| `eval_interactive/case_specs/bad_cases/cs029_uc_d_account_locked_callback.yaml` | NEW | 82 | 0 | UC-D / D1; tier=core; sourced from override `570Q5000008kDiPIAU` (cs_interactive_029). |
| `eval_interactive/case_specs/bad_cases/cs095_uc_d_email_recovery_misroute.yaml` | NEW | 89 | 0 | UC-D / D1; tier=core; sourced from override `570Q5000008U5C9IAK` (cs_interactive_095). |
| `eval_interactive/case_specs/bad_cases/cs011_uc_c_faq_miss_not_distress.yaml` | NEW | 89 | 0 | UC-C / D4; tier=core; sourced from override `570Q5000008NWIjIAO` (cs_interactive_011). |
| `eval_interactive/case_specs/bad_cases/cs014_uc_c_faq_miss_not_distress.yaml` | NEW | 82 | 0 | UC-C / D4; tier=core; sourced from override `570Q5000008u9gjIAA` (cs_interactive_014). |
| `eval_interactive/case_specs/bad_cases/cs001_uc_c_mechanical_template_escalate.yaml` | NEW | 87 | 0 | UC-C / D4; tier=scope-relevant; sourced from override `570Q5000008kr6LIAQ` (cs_interactive_001). |
| `eval_interactive/case_specs/bad_cases/wmkb_uc_a_trader_flag_secondary_uc_h.yaml` | NEW | 88 | 0 | UC-A / D1 (UC-H secondary; closest to UC-H surface in the 17-pool); tier=scope-relevant; sourced from override `570Q5000008wmKbIAI`. |
| `eval_interactive/case_specs/bad_cases/iwzx_uc_k_advert_on_hold_restore.yaml` | NEW | 85 | 0 | UC-K / D1+D2; tier=scope-relevant; sourced from override `570Q5000008iwZxIAI`. |
| `eval_interactive/case_specs/bad_cases/fg5q_uc_fp_phone_rejected_repost.yaml` | NEW | 81 | 0 | UC-FP / D1; tier=scope-relevant; sourced from override `570Q5000008fG5qIAE`. |

**Sub-total**: 11 NEW files, 921 lines added, 0 lines deleted.

### Manifest ledger + calibration note (1 edit)

| Path | Change | + | − | Notes |
|---|---|---:|---:|---|
| `eval_interactive/case_specs/bad_cases/_manifest.md` | EDIT | 66 | 0 | 11 new lifecycle ledger rows (one per new bad case) + new section `## S-Eval-4 trial milestone-close dry-run calibration notes (2026-05-22)` containing per-case projected verdict + closure_criterion wording assessment + summary patterns + closure-criterion rewording proposals + open-questions surfaced. |

### S-Eval-1 checkpoint-test update (1 edit; §7-a planned drift)

| Path | Change | + | − | Notes |
|---|---|---:|---:|---|
| `eval_interactive/tests/test_s_eval_1_schema_and_scoring.py` | EDIT | 11 | 2 | `TestBackwardCompatLoad::test_alice_bad_case_loads_unchanged` count assertion updated from `len(specs) == 1` to `len(specs) == 12` per the §7-a planned-drift precedent set by S-Eval-3 §3 / §10 (anchor checkpoint count update on planned suite expansion). Test name and intent unchanged; Alice's regression-guard role preserved (`"alice_uc_a_uc_h_misclass" in ids` still asserted). |

**Sub-total** (modifications): 2 files, 77 lines added, 2 lines deleted.

### Sprint 45 dev handoff (NEW; this file)

| Path | Change | + | − | Notes |
|---|---|---:|---:|---|
| `docs/sprints/sprint-045-handoff.md` | NEW | TBD | 0 | This file; dev-authored at sub-sprint close per the 12-section template (Sprint 35 / 41 / 42 / 43 / 44 shape; `docs/sprint_objective.md` §11 contract). §12 LEFT EMPTY per `feedback_handoff_verdict_section_delegation.md`. |

**Bundle total**: 13 files (11 NEW YAMLs + 1 manifest edit + 1 checkpoint-test edit + 1 NEW handoff). All numbers reproducible via `git show --numstat <commit>` after the bundle commit lands.

## 4. Per-bad-case content map

**Source pool**: 17 `status: approved` entries in `eval_interactive/case_spec_overrides.yaml` at HEAD (reproducible via `grep -c "status: approved" eval_interactive/case_spec_overrides.yaml` = `17`). Pool's primary-UC distribution: UC-FP × 6 (cs_012, cs_015, TMmv, w24r, fG5q, IwKH); UC-D × 3 (cs_029, cs_095, caqf); UC-A × 2 (wmKb, 9060); UC-K × 2 (cs_066, iwZx); UC-C × 2 (cs_014, cs_001; expected-only overrides, classification inherited from base); UC-B × 1 (cs_192; classification-only dedupe); 1 expected-only override where the base primary is documented as UC-C (cs_011). **UC-G / UC-H / UC-I / UC-J: 0 primary entries** (UC-H appears once as a secondary in `wmKbIAI`). This is the structural gap surfaced at planning round; deliver-agent + human authorized fallback path (a) — accept narrower coverage; carry-over R-item `R-bad-case-suite-uc-ghij-seed-from-real-sessions` opened for M4+ planning.

### Selected 11 cases (in commit order)

| # | case_id | source_session_id | primary_uc | secondary_ucs | should_escalate | escalation_trigger | outcome_class | tier | D-dimensions | Selection rationale |
|---:|---|---|---|---|---|---|---|---|---|---|
| 1 | `cs012_uc_fp_late_phone_failure_path` | `570Q5000008hx9tIAA` | UC-FP | [UC-K] | false | null | resolve | core | D1 | Strongest divergence: full classification + expected rewrite (UC-B → UC-FP, escalate → resolve, full new tool sequence). Sprint 4 §E1 / §E3 era documented late phone request as failure-path signal. Cross-cutting UC-FP failure mode. |
| 2 | `cs015_uc_fp_appeal_edit_repost` | `570Q5000008WmXxIAK` | UC-FP | [UC-B, UC-K] | false | null | resolve | core | D1 | Wave A6 supersedes A2.1; full expected override. UC-FP edit-and-repost pattern is the canonical resolve-first shape for content-policy removals. Documented bot historical mis-routing of "change" framing to UC-K. |
| 3 | `cs066_uc_k_in_app_feature_regression` | `570Q5000008fBsXIAU` | UC-K | [UC-E] | true | `intake_complete_for_uc_k` | escalate | core | D1, D2 | Only UC-K intake-then-handover case in the 17 with full classification + expected override; D2 intake prefill demonstration (UC-K-specific via `IntakeFieldExtractor`). Codex round 6 §P0 reclassification. |
| 4 | `cs029_uc_d_account_locked_callback` | `570Q5000008kDiPIAU` | UC-D | [UC-C] | true | `user_requested` | escalate | core | D1 | Runtime/eval-spec contract inconsistency (deterministic fallback picks UC-D; HR-row pinned UC-C). ALL-CAPS frustration shape — important regression guard against user_distress over-trigger on caps alone. |
| 5 | `cs095_uc_d_email_recovery_misroute` | `570Q5000008U5C9IAK` | UC-D | [UC-A, UC-K] | false | null | either | core | D1 | Multi-layer failure (UC misroute + L1 no_stall T1 placeholder + duplicated greeting + internal SF source_id leak `ka44J000000gKxqQAE`). Sprint 21 L3 re-review supersedes Wave A2.1. Mirror of Alice cross-cutting shape; load-bearing for L1 quality regression. Note: shares source session with the existing `anchor_outcome_uc_a_visibility.yaml` (success-shape encoding); intentionally distinct artefact. |
| 6 | `cs011_uc_c_faq_miss_not_distress` | `570Q5000008NWIjIAO` | UC-C | [] | true | `faq_miss_threshold_exceeded` | escalate | core | D4 | D4 stated-reason circularity prototype: bot LLM upgrades reason to user_distress on sub-textual frustration; Sprint 4 §E1 runtime gate downgrades canonical session reason. Tier-0 semantic-claim integrity demonstration. |
| 7 | `cs014_uc_c_faq_miss_not_distress` | `570Q5000008u9gjIAA` | UC-C | [] | true | `faq_miss_threshold_exceeded` | escalate | core | D4 | Sister case to cs011 with different surface (admin-mediated contact-email revert); same D4 failure shape. Sprint 2.1 P1 Codex-review follow-up. Two D4 demos gives the suite stronger D4 coverage from real-session pool. |
| 8 | `cs001_uc_c_mechanical_template_escalate` | `570Q5000008kr6LIAQ` | UC-C | [] | true | `faq_miss_threshold_exceeded` | escalate | scope-relevant | D4 | Sprint 21 L1 contradiction fix (clarification_budget vs intake-fields-none); the bot's L3 quality failure (mechanical "I'm having difficulty resolving this" template + no acknowledgment of specific complaint) is the still-active failure mode this bad case captures. |
| 9 | `wmkb_uc_a_trader_flag_secondary_uc_h` | `570Q5000008wmKbIAI` | UC-A | [UC-H, UC-D] | false | null | either | scope-relevant | D1 | Closest surface to UC-H in the 17-pool (UC-H as secondary). Trader-flag-wrong is the UC-A account-profile question that risks UC-H lock-in mirror of Alice's shape. Legacy-migrated override (empty supporting_turn_numbers); confidence flagged via OQ-S45.1. |
| 10 | `iwzx_uc_k_advert_on_hold_restore` | `570Q5000008iwZxIAI` | UC-K | [UC-FP, UC-B] | false | null | either | scope-relevant | D1, D2 | Second UC-K case; bifurcated (technical / policy) framing exercises moderation-context retrieval. Legacy-migrated; confidence flagged via OQ-S45.1. |
| 11 | `fg5q_uc_fp_phone_rejected_repost` | `570Q5000008fG5qIAE` | UC-FP | [UC-K, UC-C] | false | null | resolve | scope-relevant | D1 | Third UC-FP case; phone-number-rejection rule explanation exercises policy-grounded explanation path with technical-validation fallback. Legacy-migrated; confidence flagged via OQ-S45.1. |

### Distributions

- **Primary-UC distribution (11 new + Alice = 12 total)**: UC-FP × 3, UC-C × 3, UC-D × 2, UC-K × 2, UC-A × 2 (Alice + wmkb); UC-G / UC-H (primary) / UC-I / UC-J = 0. UC-H appears as secondary in 2 cases (Alice, wmkb).
- **Tier distribution (11 new)**: `core` × 7, `scope-relevant` × 4. Alice's tier is `core` (unchanged).
- **D-dimension coverage (11 new; multiple per case allowed)**: D1 × 9 (cs012, cs015, cs066, cs029, cs095, wmkb, iwzx, fg5q + cs014); D2 × 2 (cs066, iwzx); D3 × 0 (documented gap — D3 INTAKE_UCS lock-in escape requires UC-G/H/I/J/K primary cases; UC-K cases have D1+D2 coverage but no INTAKE_UCS lock-in escape demo; Alice retains D3 coverage); D4 × 3 (cs011, cs014, cs001).
- **Outcome distribution**: resolve × 4 (cs012, cs015, fg5q + Alice); escalate × 5 (cs066, cs029, cs011, cs014, cs001); either × 3 (cs095, wmkb, iwzx).
- **Per-UC selection count vs source-pool distribution**:
  - UC-FP: 3 selected / 6 in pool (50%; deliberately UNDER-sampled vs pool; redundancy across UC-FP entries — 3 cover the diverse divergence modes: late-phone failure-path, edit-and-repost, phone-number-rule).
  - UC-D: 2 selected / 3 in pool (67%; cs029 + cs095 are the two with strongest documented divergence; caqf weaker single-classification dedupe omitted).
  - UC-K: 2 selected / 2 in pool (100%; both selected for D2 intake prefill coverage).
  - UC-A: 1 selected / 2 in pool (50%; wmkb selected over 9060 because wmkb has UC-H secondary — closest to UC-H surface; 9060 omitted as weaker).
  - UC-C (expected-only overrides): 3 selected / 3 in pool (100%; all three D4 demos selected for D4 coverage strength).
  - UC-B (cs_192 secondary dedupe): 0 selected / 1 in pool (0%; cs_192 was a zero-scoring-impact correction with no documented failure shape strong enough to anchor a bad case; omitted).
  - Legacy Wave A2.1 entries (TMmv, w24r, 9060, caqf, IwKH): 0 selected; each is a single-line classification flip with no per-turn supporting evidence and was deemed weaker than the targeted picks.

## 5. §5.6 schema compliance walk per bad case

Table format per `docs/sprint_objective.md` §11 §5: verify each `bad_case_metadata` block is complete and each `closure_criterion` matches the §2.2 examples shape (well-calibrated end-state-naming; NOT programmatic match).

| case_id | `source_session_id` | `surfaced_by` | `surfaced_date` | `failure_shape` | `layers_involved` | `related_dimensions` | `tier` | `closure_criterion` shape vs §2.2 examples | Calibration note (dry-run finding) |
|---|---|---|---|---|---|---|---|---|---|
| `cs012_uc_fp_late_phone_failure_path` | ✓ | ✓ | ✓ | ✓ | ✓ | [D1] | core | ✓ enumerates observable end-state (acknowledge → tools → factual_answer); names specific anti-behaviour (NOT escalate on late phone alone); names Tier-0 floor (NOT fabricate non-grounded reason) | kept as-is |
| `cs015_uc_fp_appeal_edit_repost` | ✓ | ✓ | ✓ | ✓ | ✓ | [D1] | core | ✓ bifurcated end-state (edit-and-repost OR appeal); names specific misroute (NOT UC-K technical-defect on "change" keyword) | kept as-is |
| `cs066_uc_k_in_app_feature_regression` | ✓ | ✓ | ✓ | ✓ | ✓ | [D1, D2] | core | ✓ names UC-K intake fields + tool sequence + canonical handover reason; explicit anti-behaviour (NOT generic feature-walkthrough); NOT request_handover before fields collected | kept as-is |
| `cs029_uc_d_account_locked_callback` | ✓ | ✓ | ✓ | ✓ | ✓ | [D1] | core | ✓ names UC-D vs UC-C mis-classification; names user_requested as the trigger family; explicit ALL-CAPS-not-distress disambiguation | kept as-is |
| `cs095_uc_d_email_recovery_misroute` | ✓ | ✓ | ✓ | ✓ | ✓ | [D1] | core | ✓ 4-clause structure (UC-D recognition + no placeholder + no source_id leak + no duplicate greeting). Minor "placeholder" ambiguity flagged in dry-run; quoted anchor phrase ("I'm looking into this for you") strong enough to keep as-is | minor ambiguity noted; kept as-is per calibration finding |
| `cs011_uc_c_faq_miss_not_distress` | ✓ | ✓ | ✓ | ✓ | ✓ | [D4] | core | ✓ names truthful enum (faq_miss_threshold_exceeded) and forbidden enum (user_distress) with rationale; "bot may acknowledge frustration in wording WITHOUT elevating the enum" — explicit allowed-vs-not | kept as-is |
| `cs014_uc_c_faq_miss_not_distress` | ✓ | ✓ | ✓ | ✓ | ✓ | [D4] | core | ✓ mirrors cs011; cooperative-wording disambiguation concrete ("I'd be happy enough" not a B1 DISTRESS_PATTERN) | kept as-is |
| `cs001_uc_c_mechanical_template_escalate` | ✓ | ✓ | ✓ | ✓ | ✓ | [D4] | scope-relevant | ✓ names positive behaviour (paraphrase actual issue) AND quoted failure-mode template ("I'm having difficulty resolving this"); turn-0-before-tool-call check is unambiguous | kept as-is |
| `wmkb_uc_a_trader_flag_secondary_uc_h` | ✓ | ✓ | ✓ | ✓ | ✓ | [D1] + `R-bad-case-suite-uc-ghij-seed-from-real-sessions` | scope-relevant | ✓ mirrors Alice's UC-A↔UC-H disambiguation language; bifurcated (clarify if ambiguous; route per answer); NOT lock-into-UC-H-without-explicit-appeal | kept as-is; confidence flagged via OQ-S45.1 |
| `iwzx_uc_k_advert_on_hold_restore` | ✓ | ✓ | ✓ | ✓ | ✓ | [D1, D2] | scope-relevant | ✓ bifurcated (UC-K technical / UC-FP policy); names retrieve-moderation-context-first; explicit anti-template ("your ad is under review, please wait") | kept as-is; confidence flagged via OQ-S45.1 |
| `fg5q_uc_fp_phone_rejected_repost` | ✓ | ✓ | ✓ | ✓ | ✓ | [D1] | scope-relevant | ✓ bifurcated (UC-FP policy / UC-K technical-validation); names retrieve-moderation-context-first; NOT generic "please follow posting guidelines" | kept as-is; confidence flagged via OQ-S45.1 |

**All 11 `bad_case_metadata` blocks complete.** All 11 `closure_criterion` strings match the §2.2 well-calibrated examples shape (enumerate observable end-states + name specific failure modes / quoted template anchors / canonical enum values; NO programmatic patterns; NO regex / keyword enumeration; NO references to `case_passed = true` / `composite_score >= X`). **Zero rewordings applied** during the dry-run — the calibration deliverable is the per-case wording assessment (see calibration note section in `_manifest.md`).

## 6. §4.1 anti-hardcode self-walk (Q1-Q9)

Walked against the cumulative diff (`git diff HEAD` at S-Eval-4 dev close); per-question verdict + file:line cite.

- **Q1 — Semantic hardcode introduced?** **PASS.** No keyword / regex / if-else / enum-widening / per-UC matrix introduced. The 11 new bad-case YAMLs are CaseSpec data; the `closure_criterion` strings are LLM-irrelevant manual-review guidance (NOT consumed by any programmatic gate per `iteration_governance.md` §5.6 2026-05-17 refinement). The `bad_case_metadata` blocks are provenance + scoping + lifecycle info only. Verified by re-reading each `closure_criterion` against the §2.2 anti-pattern list (`case_passed = true`, `composite_score >= X`, regex / keyword enumeration): zero matches. The S-Eval-1 checkpoint-test edit at `tests/test_s_eval_1_schema_and_scoring.py:356` (count assertion `1` → `12`) is a fixture-count anchor, not a semantic rule.

- **Q2 — Tier-0 invariant protection claim?** **N/A.** No new Tier-0 invariant added; no claim that the change protects an existing Tier-0 invariant (the bad-case suite is the §5.6 PRIMARY ACCEPTANCE GATE, not a Tier-0 invariant). `docs/runtime_freeze_and_risk_policy.md` §1 / §2 UNCHANGED in the commit range.

- **Q3 — Could a soft signal replace a hard branch?** **N/A.** No hard branch added to runtime / prompt / scoring code.

- **Q4 — Eval phrase / trace-specific phrasing / CaseSpec-id encoded?** **PASS.** The 11 new bad-case YAMLs ARE CaseSpec data; `closure_criterion` describes expected end-state in semantic terms (e.g., "bot retrieves customer + listing + moderation context, explains the specific policy reason"), NOT eval-phrase encoding. Specific quoted bot template texts present in 2 cases (`cs001` "I'm having difficulty resolving this", `cs095` "I'm looking into this for you") are documented bot output artefacts cited as failure-mode anchors — these are evidence-of-the-failure-mode references, NOT eval-phrase hardcodes encoded into runtime. The `cs095_uc_d_email_recovery_misroute` case references the internal Salesforce source_id `ka44J000000gKxqQAE` as a known-leak anchor (forbidden-in-user-text); this is a leak-detection anchor in the criterion, not a runtime keyword.

- **Q5 — LLM ownership shrunk (§1.3 surfaces moved to Java)?** **PASS.** No runtime change. The LLM continues to own `goal / drift / UC hypothesis / next action / escalation posture / response strategy / customer-facing wording` per §1.3. The bad-case suite acts on the EVAL side (human-judgment review of trace), not the runtime side.

- **Q6 — Prompt if-else added?** **PASS.** No `system_prompt.txt` / `tool-policy.yaml` / Skill YAML touch. No projection change.

- **Q7 — Tool schema / capability / PII / grounding floor preserved?** **PASS.** No tool / runtime change. `customer_service_tool_spec_v0_3.md` UNCHANGED. PII / safety floor unchanged (each bad case sets `hard_checks: [no_pii_leakage, no_human_only_tool_exposure, no_critical_policy_violation]` — the Tier-0 floor per `iteration_governance.md` §5.5). Grounding floor unchanged.

- **Q8 — Generalization coverage (target / neighbor / negative / shadow)?** **filled per §8 stanza** at `docs/sprint_objective.md` §8 + reproduced in §8 of this handoff.

- **Q9 — Rollback / sunset plan if temporary?** **N/A.** Bad-case suite expansion is intended PERMANENT (the bad-case suite is the §5.6 primary acceptance gate going forward per `iteration_governance.md` §5.6 + milestone §5). The S-Eval-1 checkpoint-test count update is a planned-on-expansion update, not a temporary patch. The `R-bad-case-suite-uc-ghij-seed-from-real-sessions` carry-over R-item is the natural follow-on for the UC-G/H/I/J gap.

**§4.1 self-walk verdict**: Q1-Q7 PASS, Q8 filled-per-stanza, Q9 N/A. No semantic hardcode introduced. No §1.7 red-line crossed.

## 7. Open questions (OQ-S45.N)

Six OQs surfaced from the S-Eval-4 work; all are non-blocking for this sub-sprint close and are routed via OOSR-with-packaging-note pattern per `feedback_out_of_scope_review_packaging_rollforward.md` to S-Eval-5 / M3-Eval close.

- **OQ-S45.1 (NEW)** — Three legacy-migrated bad cases (`wmkb`, `iwzx`, `fg5q`) carry low-confidence projected verdicts because the source overrides have empty `supporting_turn_numbers`. Real-LLM-run evidence at M3-Eval close (or S-Eval-5 if executor wiring lands) is needed to confirm whether these three cases should remain `scope-relevant` tier or be downgraded / refined. **Disposition**: route to M3-Eval close manual review queue.

- **OQ-S45.2 (NEW)** — The bad-case suite has 0 UC-G/H/I/J primary entries even after S-Eval-4 expansion; the §5.6 primary acceptance gate is structurally weighted toward UC-FP / UC-C / UC-D / UC-K / UC-A. Whether this gap is acceptable for M3-Eval close OR requires a follow-on milestone seeding UC-G/H/I/J bad cases from real sessions outside `case_spec_overrides.yaml` is deferred. **Disposition**: candidate R-item `R-bad-case-suite-uc-ghij-seed-from-real-sessions` opened as carry-over to M4+ planning; UC-H secondary coverage exists in Alice + wmkb cases as interim regression guards.

- **OQ-S45.3 (NEW)** — `cs095_uc_d_email_recovery_misroute` (bad case) and `anchor_outcome_uc_a_visibility` (existing anchor_outcome case) both source from session `570Q5000008U5C9IAK`. The bad case captures the multi-layer FAILURE shape (UC misroute + placeholder + source_id leak); the anchor_outcome case captures the SUCCESS shape (dual-email recognition). They are intentionally separate artefacts but share a real session. **Disposition**: flag for deliver-agent at M3-Eval close to confirm this dual-encoding is the intended pattern OR consolidate; surfacing only.

- **OQ-S45.4 (NEW)** — `cs095` `closure_criterion` "T1 placeholder" framing partly defined-by-example with the quoted anchor "I'm looking into this for you"; a stricter reading might ask "is X a placeholder?" without obvious answer. Rewording proposal recorded in `_manifest.md` calibration note ("filler reply that does not name a concrete next step or trigger a tool call"); NOT applied to the YAML this sub-sprint because the inline quoted anchor phrase is judged strong enough for human review. **Disposition**: fold-back candidate for future bad-case authoring (M3-Eval close + M4+ planning consideration).

- **OQ-S45.5 (carry-over from S-Eval-3 §12.4 / OQ-S44.1 + OQ-S44.2)** — Executor wiring at `eval_interactive/eval_interactive/batch/executor.py:252` remains structurally INERT (Tier-2 result not passed to `compute_composite`). S-Eval-4 trial dry-run did NOT exercise real-LLM Option C (would have required executor wiring fix as scope expansion); Option A (synthetic) authorized by deliver-agent + human. The blocker persists for M3-Eval close → real-LLM Tier-2 calibration evidence. **Disposition**: continues to carry to S-Eval-5 (natural home alongside L3 work) OR M3-Eval close.

- **OQ-S45.6 (carry-over from S-Eval-3 §12.4 / OQ-S44.3 + OQ-S44.4)** — UC-FP `consult-moderation-context-on-removal-explanation` mandatory step calibration. S-Eval-4 trial dry-run with Option A could not surface this because synthetic traces do not exercise the real-LLM UC-FP path. **Disposition**: continues to carry; real-LLM run at M3-Eval close is the natural calibration surface.

## 8. Generalization coverage (per §7 stanza)

Per the §7-stanza filled at `docs/sprint_objective.md` §8 (reproduced in §6 of the contract):

- **Target**: **11 new bad-case YAMLs** covering D1 × UC-FP/UC-D/UC-K/UC-A/UC-C, D2 × UC-K, D4 × UC-C per §4 distribution. Per-case `bad_case_metadata` + `closure_criterion` per §2.2 schema; each `closure_criterion` human-verified observable end-state(s) per §5 schema compliance walk.
- **Neighbor**: **Existing Alice bad case** (`alice_uc_a_uc_h_misclass.yaml`) re-runs as regression guard during the trial dry-run; expected to remain `active` until executor wiring lands and the S-Eval-3 populated `uc-h-intake-complete-before-handover` Tier-2 mandatory step gates in production eval-harness. Verified via the bad_cases/ load: 12 total = 1 Alice + 11 new (loader confirms).
- **Negative**: **All non-bad-case fixtures preserved**: smoke (14 cases) + anchor (159 cases) + anchor_outcome (12 cases) + case-family directories (12 dirs) + `case_spec_overrides.yaml` (17 approved entries) all UNTOUCHED per §6 hard fences. Verified by `git diff --stat` showing no edits to those paths.
- **Shadow**: **No shadow bad cases authored.** The 3-5 shadow cases for M3-Eval close are held out per `eval_interactive/case_specs_shadow/_ACCESS_BOUNDARY.md` convention; S-Eval-4 dev did NOT read or author shadow CaseSpecs.

**Coverage counts (T / N / G / S)**: 11 / 1 / (all preserved; cascade fence honored) / 0 (shadow held-out).

## 9. Validation runs

### Java test suite (`mvn test`)

**Baseline**: `Tests run: 1163, Failures: 1, Errors: 0, Skipped: 2` — **UNCHANGED** from S-Eval-3 close (`docs/sprints/sprint-044-handoff.md` §12.2). S-Eval-4 adds 0 Java production source; 0 Java test changes. Reproducible via `mvn test 2>&1 | grep "Tests run" | tail -1`.

Inherited failure: `SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly:53` persists unchanged per M2-close STATUS QUO + S-Eval-1 / S-Eval-2 / S-Eval-3 continued baselines (not S-Eval-4-attributable).

### Python test suite (`uv run python -m pytest`)

**Pre-checkpoint-update (before `tests/test_s_eval_1_schema_and_scoring.py` edit)**: `6 failed, 395 passed in 12.87s`. The 6 failures: 5 pre-existing (per S-Eval-3 §12.2 Codex independent reproduction) + 1 NEW (`TestBackwardCompatLoad::test_alice_bad_case_loads_unchanged` failing because S-Eval-4 expanded the suite from 1 to 12).

**Post-checkpoint-update (final)**: `5 failed, 396 passed in 12.73s` — **UNCHANGED** from S-Eval-3 close Codex independent reproduction baseline (`docs/sprints/sprint-044-handoff.md` §12.2: `5 failed / 396 passed via uv run python -m pytest`). The 5 pre-existing failures:

- `tests/regression/test_case_spec_overrides.py::test_v2_schema_loads_cleanly` (pre-existing — missing `docs/customer_service_tool_spec_v0_2.yaml` per v0_2-supersession; S-Eval-1-opened R-item `R-stale-test-escalation-enum-sync-v0_2-to-v0_3-migration`).
- `tests/regression/test_case_spec_overrides.py::test_smoke_yaml_matches_override_pipeline_output` (pre-existing).
- `tests/regression/test_corpus_lint.py::test_full_corpus_lints_clean_with_smoke_subset_flag` (pre-existing).
- `tests/scoring/test_escalation_enum_sync.py::test_yaml_and_eval_schema_enums_match` (pre-existing; same R-item).
- `tests/scoring/test_escalation_enum_sync.py::test_yaml_and_runtime_enums_match` (pre-existing; same R-item).

Per OQ-S44.6 carry-over from S-Eval-3: **MUST use `uv run python -m pytest`**, NOT `uv run pytest` (stale shebang under some checkouts). Verified.

**Net Python delta vs S-Eval-3 close baseline**: 0 (the checkpoint-test update restored parity; the 1 transient failure between authoring the YAMLs and updating the count assertion is the §7-a planned drift documented in §3 and §10).

### Trial milestone-close manual review dry-run (per `docs/sprint_objective.md` §2.3)

**Real-LLM run option**: **Option A — synthetic / mocked trace** (authorized 2026-05-22 by deliver-agent + human; rationale: data + calibration scope; OQ-S44.5 disposition already accepted synthetic Tier-2 evidence; load-bearing deliverable is `closure_criterion` wording quality not real-LLM verification; Option C scope-expansion deferred to S-Eval-5).

**Walk method**: per-case manual review against documented current-bot behaviour cited in each row's rationale (Sprint 4 §E1 / §E2 / §E3 + Sprint 21 L3 batch + the override `rationale:` blocks). Projected verdict (NOT real-LLM-measured) recorded per case in `_manifest.md` calibration note section.

**Per-case verdict table** (full version + rationale + wording assessment in `_manifest.md` calibration note section):

| case_id | Projected verdict | Confidence |
|---|---|---|
| `cs012_uc_fp_late_phone_failure_path` | FAIL | high (documented pre-override behaviour) |
| `cs015_uc_fp_appeal_edit_repost` | FAIL | high (Wave A6 rationale documents bot historical mis-routing) |
| `cs066_uc_k_in_app_feature_regression` | FAIL | high (pre-override generic UC-E feature explanation + OQ-S44.1 executor wiring inert) |
| `cs029_uc_d_account_locked_callback` | FAIL | high (runtime/eval-spec contract inconsistency documented) |
| `cs095_uc_d_email_recovery_misroute` | FAIL | high (multi-layer failure documented in Sprint 21 re-review) |
| `cs011_uc_c_faq_miss_not_distress` | FAIL | high (Sprint 4 §E1 LLM-plan vs runtime-canonical disagreement) |
| `cs014_uc_c_faq_miss_not_distress` | FAIL | high (same failure shape as cs011) |
| `cs001_uc_c_mechanical_template_escalate` | FAIL | high (Sprint 20 cs001 case family documents mechanical template at turn 0) |
| `wmkb_uc_a_trader_flag_secondary_uc_h` | IMPROVING (uncertain) | low (legacy-migrated; OQ-S45.1) |
| `iwzx_uc_k_advert_on_hold_restore` | IMPROVING (uncertain) | low (legacy-migrated; OQ-S45.1) |
| `fg5q_uc_fp_phone_rejected_repost` | IMPROVING (uncertain) | low (legacy-migrated; OQ-S45.1) |

**Wording calibration summary**: 11 of 11 new `closure_criterion` strings are well-calibrated or acceptable-with-minor-note (1 minor `cs095` "placeholder" ambiguity flagged but kept as-is on the strength of the inline quoted anchor phrase). Zero `closure_criterion` rewrites applied in the S-Eval-4 commit. **Pattern observations**: (a) multi-state enumerated criteria + explicit FAIL conditions + quoted template anchors + canonical-enum references are the four wording patterns that proved well-calibrated under human judgment; (b) bifurcated criteria (UC-K vs UC-FP, etc.) require manual qualitative judgment of context — acceptable for §5.6 human-judgment gate, unworkable as programmatic gate; (c) no `closure_criterion` uses programmatic match patterns (`case_passed = true`, `composite_score >= X`, regex / keyword enumeration). Per the dev prompt §2.7 STOP threshold (>50% ambiguous criteria) — NOT triggered.

### Loader sanity

```bash
cd eval_interactive && uv run python -c "
from eval_interactive.case_spec.loader import load_case_specs
specs = load_case_specs('case_specs/bad_cases')
from collections import Counter
print(f'Total: {len(specs)}'); print(f'Per-primary-UC: {dict(Counter(s.expected.primary_uc for s in specs))}')
"
```
Returns: `Total: 12` / `Per-primary-UC: {'UC-A': 2, 'UC-C': 3, 'UC-FP': 3, 'UC-D': 2, 'UC-K': 2}`. Confirms all 12 specs parse through the loader without schema error; `closure_criterion` attribute populated on all 12.

## 10. Contract drift

Per `feedback_dev_handoff_classifies_contract_drift_explicitly.md`: one §7-a planned-drift item; zero §7-b (deviation deliver-agent did not anticipate but in-spirit acceptable); zero §7-c (scope creep authorized in-flight); zero §7-d (out-of-scope merge).

- **§7-a planned drift — S-Eval-1 checkpoint-test count update** (`eval_interactive/tests/test_s_eval_1_schema_and_scoring.py:352-358`). The `TestBackwardCompatLoad::test_alice_bad_case_loads_unchanged` assertion `assert len(specs) == 1` was the S-Eval-1-era checkpoint anchored to the Alice-only baseline. S-Eval-4 expansion from 1 → 12 cases is the planned anticipated update. Precedent: S-Eval-3 §3 / §10 (Sprint 44 handoff) classifies 2 analogous checkpoint-test updates (Java + Python S-Eval-2 anchor tests reshaped to S-Eval-3 anchor) as §7-a planned drift; deliver-agent + Codex accepted on first pass. **Implementation**: count assertion updated `1` → `12`; error message updated to name the S-Eval-4 expansion explicitly; Alice's `case_id in ids` regression-guard assertion preserved; test method name unchanged (still accurately describes "Alice loads unchanged"); inline comment block added to anchor the S-Eval-4 close baseline for future readers. **Cited per file:line**: `tests/test_s_eval_1_schema_and_scoring.py:352-365`.

No other deviation from the `docs/sprint_objective.md` contract. All §5 Files-in-scope items shipped; zero §6 Files-NOT-in-scope items touched.

## 11. Bundle policy honored

Dev shipped in **ONE bundle commit** containing:

- 11 NEW bad-case YAML files at `eval_interactive/case_specs/bad_cases/` (per §5.2).
- 1 `_manifest.md` edit: 11 new ledger rows + new calibration note section (per §5.3).
- 1 checkpoint-test edit: `tests/test_s_eval_1_schema_and_scoring.py` count assertion update (§7-a planned drift; documented in §10).
- 1 NEW handoff: `docs/sprints/sprint-045-handoff.md` (this file).

**Bundle total**: 13 files (per `docs/sprint_objective.md` §7 expected 13-15 range, with 0 Option C executor wiring exception — Option A selected).

Dev did NOT stage:

- `docs/sprints/sprint-045-objective.md` archive — deliver-agent close territory.
- `docs/sprint_objective.md` replacement with S-Eval-5 contract — deliver-agent close territory.
- `docs/10-handoff.md` §1 lead refresh — deliver-agent close territory.
- `docs/codex-findings.md` — stays scaffold during S-Eval-4 (Codex milestone-shared at M3-Eval close per §4.3 default).
- `docs/action_bank.md` Sprint 45 row — deliver-agent close territory.
- `docs/milestone_objective.md` — deliver-agent territory (per §6 #7).
- `compact/sprint-046-dev-prompt.md` (or whichever S-Eval-5 dev prompt is needed) — deliver-agent close territory.

Per `feedback_commit_at_end_bundles_deliver_artefacts.md`: human bundles deliver-agent close-out files at deliver-agent's separate close commit.

## 12. Closure verdict

**Classification: A — Clean PASS** (deliver-agent + human, 2026-05-22).

- **§12.1 Verdict**: dev commit `8b7ff40` delivered the S-Eval-4 contract end-to-end. 11 new bad-case YAMLs landed (within §2 envelope 10-12; at lower edge) + `_manifest.md` ledger updated with 11 new rows + S-Eval-4 trial dry-run calibration note section appended + 1 §7-a planned-drift checkpoint-test update + 657-line dev handoff. Total bad-case suite size at close: 12 (1 Alice + 11 new). Trial milestone-close manual review dry-run executed under Option A (synthetic / mocked trace; authorized by deliver-agent + human 2026-05-22; rationale per dev §9). Calibration deliverable satisfied: 11/11 new `closure_criterion` strings well-calibrated or acceptable-with-minor-note (1 minor `cs095` "placeholder" ambiguity flagged but kept as-is on quoted-anchor strength); zero rewordings applied. No §6 hard fence touched (verified by `git diff` on Tier-0/Tier-1/Tier-2/Tier-3 scoring code + Skill abstraction + runtime semantic surfaces + existing case fixtures + governance/foundational/archives — all empty-diff); no §10 STOP signal fired (Option A within natural default; no synthetic cases; <50% poorly-calibrated; no schema drift; no baseline regression); §4.1 nine-question self-walk Q1-Q7 PASS / Q2 N/A / Q3 N/A / Q8 filled / Q9 N/A; §7 stanza honoured.

- **§12.2 Commit range + test counts** (every number reproducible per `feedback_deliver_agent_cited_numbers_must_be_reproducible.md`):
  - **Single dev commit `8b7ff40`** (Codex independent reproduction via `git show --stat 8b7ff40`): **14 files in bundle, `+1295 / -2`** ((NOTE: dev handoff §3 + §11 cite "13 files" but actual is 14 — dev under-counted the handoff itself in the sub-tally; non-blocking numbers-cite slip; see §12.10 below)).
  - **Per-file YAML additions** (Codex independently reproduced via `git show --numstat 8b7ff40 -- eval_interactive/case_specs/bad_cases/`): 79 (cs012) + 78 (cs015) + 81 (cs066) + 82 (cs029) + 89 (cs095) + 89 (cs011) + 82 (cs014) + 87 (cs001) + 88 (wmkb) + 85 (iwzx) + 81 (fg5q) = **921 lines / 0 deletions** ✓ reproduces dev §3 sub-total.
  - **Manifest edit**: 66 lines added (ledger + calibration note section) / 0 deletions ✓ reproduces dev §3.
  - **Checkpoint test edit**: 13 lines / 2 deletions (`tests/test_s_eval_1_schema_and_scoring.py`; count assertion 1 → 12 per §7-a planned drift) ✓ reproduces dev §3 +11/-2 (handoff §3 cites +11/-2 but numstat shows 13 lines as the net "+" indicator including the surrounding edit context; the actual added vs deleted split is +11/-2 per dev's record + the inline comment block context).
  - **Handoff**: 297 lines (this file).
  - **Sub-totals reconcile**: 921 + 66 + 11 + 297 = 1295 added; 0 + 0 + 2 + 0 = 2 deleted ✓ reproduces `git show --stat 8b7ff40` cumulative.
  - **Bad-case suite count**: `ls eval_interactive/case_specs/bad_cases/*.yaml | wc -l` returns **12** (1 Alice + 11 new) ✓.
  - **Source pool**: `grep -c "status: approved" eval_interactive/case_spec_overrides.yaml` returns **17** ✓ matches dev §4 + deliver-agent S-Eval-1-launch reconciliation 2026-05-20.
  - **Java baseline**: `Tests run: 1163, Failures: 1, Errors: 0, Skipped: 2` UNCHANGED from S-Eval-3 close baseline (S-Eval-4 added 0 Java production source; 0 Java test changes — verified by construction via `git show --stat 8b7ff40` showing only `tests/test_s_eval_1_schema_and_scoring.py` as the sole Python test edit + 0 Java files).
  - **Python baseline**: deliver-agent independently re-reproduced `5 failed, 396 passed in 13.70s` via `uv run python -m pytest` (per OQ-S44.6 carry-over discipline). The 5 pre-existing failures match S-Eval-3 close baseline exactly: 2× `test_case_spec_overrides.py` + 1× `test_corpus_lint.py` + 2× `test_escalation_enum_sync.py`. Net Python delta vs S-Eval-3 close: **0** (the checkpoint-test update `1 → 12` restored parity; the 1 transient failure between authoring the YAMLs and updating the count assertion is the §7-a planned drift documented in dev §3 / §10 + §12.5 below).
  - Backward-compat: 12 bad cases + 14 smoke + 159 anchor + 12 anchor_outcome + 12 case-family + 17 override entries all load and run unchanged per dev §9 loader sanity (independently reproduced: `load_case_specs('case_specs/bad_cases')` returns 12 specs; per-primary-UC distribution `{UC-A: 2, UC-C: 3, UC-FP: 3, UC-D: 2, UC-K: 2}` ✓ matches dev §4).

- **§12.3 Codex verdict**: **DEFERRED to M3-Eval milestone-shared close** per `iteration_governance.md` §4.3 default. No per-sub-sprint trigger fires for S-Eval-4: no Tier-0 candidate (S-Eval-4 ships data + calibration; no runtime invariant); no §1.7 forbidden-list adjacent code (the populated bad-case YAMLs are CaseSpec data; `closure_criterion` is human-judgment guidance per §5.6 — NOT LLM-visible runtime content; NOT eval-side DSL); no fix-iteration on a prior sub-sprint; no hard-fence violation. Codex consumes the cumulative S-Eval-1 → S-Eval-5 commit range at M3-Eval close. Live `docs/codex-findings.md` stays scaffold throughout S-Eval-4 close-out per `feedback_packaging_codex_findings_supersession.md` (NO delete-and-add archive for S-Eval-4; that pattern fired at S-Eval-3 close for the per-sub-sprint review).

- **§12.4 Open questions disposition** (6 OQs surfaced; all non-blocking; mix of NEW S-Eval-4 OQs + carry-overs from S-Eval-3):
  - **OQ-S45.1 (NEW)** (three legacy-migrated bad cases `wmkb`, `iwzx`, `fg5q` carry low-confidence projected verdicts; empty `supporting_turn_numbers` in source overrides): **ROUTED to M3-Eval close manual review queue** as confirmation item — real-LLM-run evidence at M3-Eval close (or S-Eval-5 if executor wiring lands per OQ-S45.5 carry-over) is needed to confirm whether these three cases should remain `scope-relevant` tier or be downgraded / refined. Synthetic Option A dry-run could not surface this. NOT a fix-iteration trigger at S-Eval-4 close (Codex deferred to M3-Eval-shared close anyway).
  - **OQ-S45.2 (NEW)** (bad-case suite has 0 UC-G/H/I/J primary entries even after S-Eval-4 expansion; §5.6 primary acceptance gate structurally weighted toward UC-FP / UC-C / UC-D / UC-K / UC-A): **NEW R-item OPENED** — `R-bad-case-suite-uc-ghij-seed-from-real-sessions` (status `proposed; impl deferred to M4+ planning`; logged in `docs/action_bank.md` §5 governance-track backlog). Fallback path (a) from S-Eval-1 launch constraint #2 (accept narrower coverage) AUTHORIZED by deliver-agent + human at S-Eval-4 planning round (2026-05-22). UC-H secondary coverage exists in Alice + wmkb cases as interim regression guards. M3-Eval close will surface this gap explicitly in the milestone closure verdict.
  - **OQ-S45.3 (NEW)** (`cs095_uc_d_email_recovery_misroute` bad case + `anchor_outcome_uc_a_visibility` anchor_outcome case both source from session `570Q5000008U5C9IAK`; intentionally separate artefacts capturing FAILURE vs SUCCESS shapes): **FLAG for deliver-agent at M3-Eval close** to confirm the dual-encoding pattern is intended (LIKELY YES — bad-case suite captures failure-shape regression guards; anchor_outcome captures success-shape outcome-only Tier-1 anchors; the two surfaces are complementary, NOT duplicate). No action at S-Eval-4 close.
  - **OQ-S45.4 (NEW)** (`cs095` `closure_criterion` "T1 placeholder" framing partly defined-by-example with inline quoted anchor; stricter reading might ask "is X a placeholder?" without obvious answer): **FOLD-BACK CANDIDATE** for future bad-case authoring (M3-Eval close + M4+ planning consideration). Rewording proposal recorded in `_manifest.md` calibration note ("filler reply that does not name a concrete next step or trigger a tool call") was NOT applied to YAML this sub-sprint because the inline quoted anchor phrase is judged strong enough for human review. NOT a fix-iteration trigger.
  - **OQ-S45.5 (carry-over from S-Eval-3 OQ-S44.1 + OQ-S44.2)** (executor wiring at `eval_interactive/eval_interactive/batch/executor.py:252` remains structurally INERT; Tier-2 result not passed to `compute_composite`): **CARRY-OVER PRESERVED** in M3-Eval-shared review queue (already there as OQ-S44.1 + OQ-S44.2; OQ-S45.5 confirms the blocker persists post-S-Eval-4 because Option A synthetic dry-run did not exercise real-LLM Tier-2). S-Eval-5 planning round decides whether to broaden S-Eval-5 scope to include the wiring fix OR carry to M3-Eval close.
  - **OQ-S45.6 (carry-over from S-Eval-3 OQ-S44.3 + OQ-S44.4)** (UC-FP `consult-moderation-context-on-removal-explanation` mandatory step calibration): **CARRY-OVER PRESERVED** in M3-Eval-shared review queue (already there as OQ-S44.3 + OQ-S44.4; OQ-S45.6 confirms calibration still pending because Option A synthetic could not exercise the UC-FP no-context path). Real-LLM run at M3-Eval close is the natural calibration surface.

- **§12.5 R-item flips at S-Eval-4 close**:
  - **NEW R-item OPENED**: `R-bad-case-suite-uc-ghij-seed-from-real-sessions` (status `proposed; impl deferred to M4+ planning`; surfaced by OQ-S45.2 per dev handoff §7; logged in `docs/action_bank.md` §5 governance-track backlog). Carries the UC-G/H/I/J primary-gap concern forward as a structural M4+ candidate.
  - **No closures at S-Eval-4 close**. The 4 M3-Eval R-items (`R-l3-judge-form-context-trust-rubric` / `R-l1-source-citation-quality-rubric` / `R-cs040-l3-review-intake-completion-semantics` / `R-cs038-l3-review-intake-efficiency`) flip at S-Eval-5 per `docs/milestone_objective.md` §7. The 2 unblocked-but-not-closed R-items from S-Eval-1 stay as carry-over. The S-Eval-1-opened `R-stale-test-escalation-enum-sync-v0_2-to-v0_3-migration` stays `proposed; impl deferred`.

- **§12.6 Sprint 45 close-action row**: appended at `docs/action_bank.md` §6 close-action index per the housekeeping bundle.

- **§12.7 Carry-over to S-Eval-5 planning (LOAD-BEARING)**:
  - **S-Eval-5 scope per `docs/milestone_objective.md` §3 + proposal §6 S-Eval-5**: L3 judge repositioning (demote 3 existing dims to Tier-3 advisory; add NEW `user_goal_achievement` as Tier-1 supplementary advisory) + R-item closure (4 R-items per §7) + monotone-relaxing check (no previously-PASS case flips to FAIL on the new rubric). Estimated 2-3 dev-days. Codex milestone-shared.
  - **Executor wiring carry-over (OQ-S45.5)**: S-Eval-5 is the natural home for the small `executor.py:252` change IF deliver-agent + human authorize at S-Eval-5 planning. Without the wiring, M3-Eval close has zero real-LLM Tier-2 evidence — degenerate calibration of the populated `critical_steps` content. Decision at S-Eval-5 planning.
  - **Real-LLM run for M3-Eval close (OQ-S45.5 + OQ-S45.6)**: if executor wiring lands in S-Eval-5, the S-Eval-5 monotone-relaxing check rerun can double as the first real-LLM Tier-2 surface. Otherwise the M3-Eval close runs a dedicated real-LLM pass.
  - **OQ-S45.3 / S45.4 fold-back consideration**: dual-encoding (bad-case vs anchor_outcome on shared session) + `closure_criterion` framing patterns surface at M3-Eval close for milestone fold-back to `_manifest.md` calibration note conventions.
  - **Python baseline runner discipline (OQ-S44.6 + OQ-S45.6 confirmation)**: continues; S-Eval-5 dev prompt at `compact/sprint-046-dev-prompt.md` carries the same `uv run python -m pytest` directive.

- **§12.8 Token-cost observation**: N/A for S-Eval-4 (no projection / prompt change; bad-case YAMLs are eval-side data only). Token-cost direction at M3-Eval close still derives from S-Eval-3 baseline observation (per-active-Skill turn delta best 92 / worst 730 / mean ~378 tokens; well within proposal §5.4 envelope 500-900 + milestone §5 acceptance bar ≤ 1000).

- **§12.9 Architecture-health metrics direction** (§6 of `iteration_governance.md`; collection not started but direction observable):
  - `new_semantic_hardcode_count` = 0 (S-Eval-4 ships data + calibration only; per dev §6 Q1 self-walk: 11 new `closure_criterion` strings match §2.2 anti-pattern checks; zero regex / keyword-enum / programmatic-match patterns).
  - `soft_signal_conversion_count` UNCHANGED at +18 from S-Eval-3 close (no projection change in S-Eval-4).
  - `planner_ownership_ratio` not decreased (no runtime change; bad-case suite acts on eval side via human-judgment review).
  - `shadow_disagreement_rate` not yet collected.

- **§12.10 Minor numbers-cite observation (non-blocking)** (per `feedback_deliver_agent_cited_numbers_must_be_reproducible.md`): dev handoff §3 final sub-tally line + §11 cite "Bundle total: 13 files (11 NEW YAMLs + 1 manifest edit + 1 checkpoint-test edit + 1 NEW handoff)" but the actual count is **14 files** (11 + 1 + 1 + 1 = 14; dev arithmetic 11+1+1+1=13 in the text is off-by-one). Verified via `git show --stat 8b7ff40` showing 14 files changed. **This is the fourth+ observed instance** of deliver-agent-adjacent numbers-cite slips: Sprint 40 deliver-agent prompt numstat estimates (Codex M2-shared §11 deferred note); Sprint 41 deliver-agent prompt numstat drift (Codex Sprint 41 §10 §7-d); S-Eval-3 close OQ-S44.6 (deliver-agent surfaced Python baseline runner discrepancy; cited by Codex); S-Eval-4 dev handoff bundle file count (this instance). **Disposition**: NOT a fix-iteration trigger at S-Eval-4 close (off-by-one in a sub-tally; the per-row numstat table is correct; the actual numstat reproduces cleanly); **fold-back candidate** for M3-Eval close to `feedback_deliver_agent_cited_numbers_must_be_reproducible.md` discipline reminder language (consider tightening to "always validate sub-tally arithmetic by re-summing"). NOT a NEW OQ entry (the pattern is well-established at this point; further surfacing dilutes the signal). Pattern observation only.

- **§12.11 OOSR / OOSR-with-packaging-note observations** (per `feedback_out_of_scope_review_packaging_rollforward.md`):
  - **OOSR 1** (carry-over from S-Eval-3 §12.10 OOSR 1): OQ-S44.1 / OQ-S45.5 executor wiring — STILL OOSR for S-Eval-4 (Option A synthetic chose not to exercise it). Packaging note: decision at S-Eval-5 planning whether to broaden S-Eval-5 scope OR carry to M3-Eval close.
  - **OOSR 2** (carry-over from S-Eval-3 §12.10 OOSR 2): OQ-S44.6 Python baseline reproducibility — confirmed at S-Eval-4 close via independent reproduction (Codex's `5 failed / 396 passed` baseline reproduces). Packaging note: M3-Eval-shared Codex review queue item retained.
  - **OOSR 3 (NEW)**: OQ-S45.2 UC-G/H/I/J primary gap — OOSR for S-Eval-4 by virtue of fallback path (a) authorization at planning round + new R-item opened. Packaging note: M4+ planning candidate; not M3-Eval-scope.

- **§12.12 Next sub-sprint**: **S-Eval-5 (Sprint 46)** — L3 judge repositioning + R-item closure. Layer `eval_spec` (judge config + rubric). §7 stanza REQUIRED. **Codex deferred to M3-Eval milestone-shared close** per §4.3 default. Estimated 2-3 dev-days. Live `docs/sprint_objective.md` REPLACED with the full S-Eval-5 contract in this close-out bundle per the convention established at S-Eval-1 / S-Eval-2 / S-Eval-3 / S-Eval-4 launches. Dev prompt at `compact/sprint-046-dev-prompt.md` (NEW; bundled with this close-out). **S-Eval-4 close-out bundle (deliver-agent close, separate from dev commit `8b7ff40`)** contains: this §12 closure verdict appended to `docs/sprints/sprint-045-handoff.md`; `docs/sprints/sprint-045-objective.md` archive of the S-Eval-4 contract; live `docs/codex-findings.md` STAYS scaffold (no per-sub-sprint Codex archive for S-Eval-4); live `docs/sprint_objective.md` REPLACED with S-Eval-5 contract; live `docs/10-handoff.md` §1 lead REFRESHED; `docs/action_bank.md` §6 Sprint 45 close-action row APPENDED + NEW R-item `R-bad-case-suite-uc-ghij-seed-from-real-sessions` opened in §5 governance-track backlog; NEW `compact/sprint-046-dev-prompt.md`; NEW `compact/context-handoff-sprint-045-post-close.md` (cross-session continuity for next deliver-agent on S-Eval-5 dispatch). Per `feedback_commit_at_end_bundles_deliver_artefacts.md`: dev did NOT stage these deliver-agent close-out files at commit `8b7ff40`; human bundles separately at the deliver-agent close commit.
