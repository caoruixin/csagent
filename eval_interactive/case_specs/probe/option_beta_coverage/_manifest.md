# Sprint 35 probe corpus — Option β coverage matrix

**Authored:** 2026-05-16 (Sprint 35, M1 sub-sprint 3 per `docs/milestone_objective.md`)
**Source-of-truth:** this file (lifecycle ledger) + `docs/sprint_objective.md` (Sprint 35 contract) + `docs/diagnostics/option_beta_coverage_matrix.md` (analysis surface)
**Owners:** Sprint 35 dev (corpus authoring) + deliver-agent + human (lifecycle stewardship)

## Purpose

This directory holds the Sprint 35 probe CaseSpec corpus for the Option β coverage matrix. Probe CaseSpecs are **diagnostic instruments**, NOT regression gates.

They are read by `eval-interactive run --path case_specs/probe/option_beta_coverage/` to drive the existing intake router (`UseCaseRouter`) + projection builder (`ContextProjectionBuilder`) on a deliberately shaped (`topic_subject` × `description`-shape) grid, so the dev can extract `active_use_case` + `alternate_candidate_use_cases` + `candidate_use_cases` from the per-turn trace and populate the Option β coverage matrix in `docs/diagnostics/option_beta_coverage_matrix.md`.

Sprint 35 ships zero runtime code change; this corpus measures what the production routing surface already does.

## Loader contract

Flat layout per Sprint 29 / Sprint 32 / bad-cases precedent. Loader globs `*.yaml` in this directory (`eval_interactive/eval_interactive/case_spec/loader.py:144`); this markdown manifest is skipped. Load + run:

```bash
cd eval_interactive
uv run eval-interactive run --path case_specs/probe/option_beta_coverage/
```

## Schema extension (NOT loader-enforced)

Each probe CaseSpec carries the standard `case_specs` schema (`form_context` / `persona` / `expected` / `scoring`) PLUS two probe-specific blocks:

- `probe_metadata`: documentary block naming the probe's dimension cell.
  - `probe_dimension_topic_subject`: the topic axis value.
  - `probe_dimension_description_shape`: one of `single-issue` | `multi-issue-same-topic` | `cross-topic-drift` | `ambiguous-soft` | `empty-generic`.
  - `probe_expected_routing`: deliver-agent + dev pre-run prediction (`AMBIGUOUS{...}` or `ROUTED{UC-X}`); the matrix records the OBSERVED routing.
  - `probe_purpose`: one-paragraph rationale.
- `probe_observation_recipe`: the exact extraction path inside `results/<timestamp>/results.json` that produces the matrix cell value.

The loader ignores these blocks (extra YAML keys are dropped during dataclass construction in `loader.py:_parse_case_spec`). They exist purely as in-file documentation so a human reading any probe file can understand its role without leaving the file.

The `expected.*` and `scoring.*` blocks are populated with minimal valid values so the loader's `__post_init__` validators (`schema.py:170-204`) pass. The values are **not** used as a regression rubric — probe CaseSpecs are not scored against a pass/fail bar. `max_turns: 2` keeps LLM cost bounded (turn 0 routing + one bot response is the unit of observation).

## Lifecycle

- **Opened**: 2026-05-16 by Sprint 35 to close `R-option-beta-coverage-gap-uc-a-uc-c-shape` (`docs/action_bank.md:667`).
- **Active until**: superseded by an Option γ probe corpus (`R-option-gamma-alternate-uc-surveyor-design`, surfaced only if Sprint 35 decision (b) is confirmed) OR the (`topic_subject` × `description`-shape) routing surface materially changes (M2+ — fold-back trigger).
- **Re-run permission**: any future sub-sprint may re-run this corpus to refresh the matrix against current `UseCaseRouter` behaviour. Re-runs are observation-only; new findings should land in the existing `option_beta_coverage_matrix.md` or a follow-on analysis doc, NOT in this directory's CaseSpec contents.
- **Closure**: this directory is retained as the baseline reference once Sprint 35 ships, similar to the bad-cases retention convention. Probe CaseSpecs are not deleted at closure.

## Governance pointer

- `docs/sprint_objective.md` (Sprint 35) — design contract for this corpus.
- `docs/milestone_objective.md` (M1) — milestone scope (Sprint 35 = M1 sub-sprint 3).
- `docs/diagnostics/option_beta_coverage_matrix.md` — the analysis surface this corpus feeds.
- `docs/current/iteration_governance.md` §7.2 — the worked-example whose anchor decision (a/b) this corpus informs.
- `docs/sprints/sprint-032-handoff.md` §13 — the source finding (`R-option-beta-coverage-gap-uc-a-uc-c-shape`).

## Per-case index

Rows are ordered by topic_subject then description_shape. The expected_routing column is the pre-run prediction; consult `docs/diagnostics/option_beta_coverage_matrix.md` §3 for the OBSERVED routing per cell.

| case_id | topic_subject | description_shape | expected_routing (prediction) |
|---|---|---|---|
| `probe_ad_support_single_uc_a_visibility_bias` | Ad Support | single-issue | ROUTED{UC-A} via B2 ADS_VISIBILITY_BIAS |
| `probe_ad_support_single_uc_b_posting_neutral` | Ad Support | single-issue | AMBIGUOUS{UC-A, UC-B, UC-FP, UC-H} |
| `probe_ad_support_multi_uc_a_uc_h_neutral` | Ad Support | multi-issue-same-topic | AMBIGUOUS{UC-A, UC-B, UC-FP, UC-H} |
| `probe_ad_support_cross_topic_uc_c_drift` | Ad Support | cross-topic-drift | AMBIGUOUS{UC-A, UC-B, UC-FP, UC-H} (UC-C absent — §7.2 finding) |
| `probe_ad_support_ambiguous_soft_alice_shape` | Ad Support | ambiguous-soft | AMBIGUOUS{UC-A, UC-B, UC-FP, UC-H} |
| `probe_ad_support_empty_generic` | Ad Support | empty-generic | AMBIGUOUS{UC-A, UC-B, UC-FP, UC-H} |
| `probe_payments_single_uc_f_faq_neutral` | Payments | single-issue | AMBIGUOUS{UC-F, UC-I} |
| `probe_payments_multi_uc_f_uc_i_neutral` | Payments | multi-issue-same-topic | AMBIGUOUS{UC-F, UC-I} |
| `probe_payments_cross_topic_uc_d_drift` | Payments | cross-topic-drift | AMBIGUOUS{UC-F, UC-I} (UC-D absent) |
| `probe_payments_ambiguous_soft` | Payments | ambiguous-soft | AMBIGUOUS{UC-F, UC-I} |
| `probe_tech_support_single_uc_e_faq_neutral` | Technical Support | single-issue | AMBIGUOUS{UC-E, UC-K} |
| `probe_tech_support_regression_uc_k_override` | Technical Support | single-issue | ROUTED{UC-K} via regression override |
| `probe_tech_support_multi_uc_e_uc_k_neutral` | Technical Support | multi-issue-same-topic | AMBIGUOUS{UC-E, UC-K} |
| `probe_tech_support_cross_topic_uc_f_drift` | Technical Support | cross-topic-drift | AMBIGUOUS{UC-E, UC-K} (UC-F absent) |
| `probe_account_support_single_uc_d_login_bias` | Account Support | single-issue | ROUTED{UC-D} via B2 ACCOUNT_LOGIN_BIAS |
| `probe_account_support_cross_topic_uc_c_drift_with_login_bias` | Account Support | cross-topic-drift | ROUTED{UC-D} (login bias fires before messaging bias) |
| `probe_account_support_ambiguous_soft_no_bias` | Account Support | ambiguous-soft | AMBIGUOUS{UC-D} |
| `probe_replies_messaging_single_uc_c_strong_prior` | Replies or Messaging | single-issue | ROUTED{UC-C} via strong-prior |
| `probe_replies_messaging_cross_topic_uc_a_drift` | Replies or Messaging | cross-topic-drift | ROUTED{UC-C} (strong-prior wins; UC-A absent) |
| `probe_replies_messaging_ambiguous_soft` | Replies or Messaging | ambiguous-soft | ROUTED{UC-C} via strong-prior |
| `probe_delete_single_uc_g_strong_prior` | Delete My Account or Data | single-issue | ROUTED{UC-G} via strong-prior |
| `probe_delete_cross_topic_safety_drift` | Delete My Account or Data | cross-topic-drift | ROUTED{UC-G} (strong-prior; UC-J absent) |
| `probe_safety_single_uc_j_strong_prior` | Report a Safety Issue | single-issue | ROUTED{UC-J} via strong-prior |
| `probe_safety_cross_topic_uc_i_payment_drift` | Report a Safety Issue | cross-topic-drift | ROUTED{UC-J} (strong-prior; UC-I absent) |
| `probe_handover_delivery_safety_override` | Delivery | cross-topic-drift | ROUTED{UC-J} via TOPIC_OVERRIDES fraud pattern |

## Why this is not in `case_families/` or `bad_cases/`

The case-family directory follows the Sprint 20 G2 target/neighbor/negative/shadow split shape; probe CaseSpecs have no such split. The bad-cases directory holds real-session-derived failures with a `closure_criterion`; probes have no closure criterion (they characterize a surface, they do not gate it). Mixing the schemas would dilute both contracts. Per the M1 plan §3 Sprint 35 row, the probe directory is its own home.

## Pruning rationale (from the full 7 × 5 = 35 cell cartesian)

10 cells pruned:

1. **Strong-prior topics × multi-issue same-topic** (3 cells: Replies, Delete, Safety) — collapses to single-issue or cross-topic; a strong-prior topic has only one UC, so "multi-issue same-topic" is a misnomer.
2. **Strong-prior Delete × ambiguous-soft and × empty-generic** (2 cells) — strong-prior wins regardless; one strong-prior Delete cell + one cross-topic Delete cell is sufficient control.
3. **Strong-prior Safety × ambiguous-soft and × empty-generic** (2 cells) — same rationale as Delete.
4. **Handover-only Delivery × single-issue / ambiguous-soft / empty-generic** (3 cells) — one handover-only override cell is sufficient; the override path is heavily covered by Sprint 10 / 11 / 32 existing tests.

That leaves 25 cells = the corpus shipped.
