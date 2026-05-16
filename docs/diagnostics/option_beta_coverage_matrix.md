---
title: Option β coverage matrix — (topic_subject, description-shape) → AMBIGUOUS/ROUTED
doc_tier: diagnostic
status: diagnostic
implementation_status: implemented
source_of_truth: this file
last_reviewed: 2026-05-16
review_cadence: ad hoc
supersedes: []
superseded_by: null
notes: >
  Sprint 35 diagnostic output (M1 sub-sprint 3 per
  `docs/milestone_objective.md`). Characterizes the Option β
  (`alternate_candidate_use_cases` projection slot, Sprint 31 ship +
  Sprint 32 case family) coverage surface across the
  (`topic_subject` × `description`-shape) dimensions. Used to close
  `R-option-beta-coverage-gap-uc-a-uc-c-shape` (`docs/action_bank.md:667`)
  with a re-anchor decision on `iteration_governance.md` §7.2 worked
  example and an Option γ follow-on R-item draft.

  The probe corpus underlying this matrix lives at
  `eval_interactive/case_specs/probe/option_beta_coverage/` and is
  retained as the baseline reference. The matrix MAY be re-run on
  any future sub-sprint to refresh the observation.

  Sprint 35 ships zero production code change; this document
  describes what the existing routing surface already does.
---

# Option β coverage matrix

## 1. Purpose

This document answers the question Sprint 32 §13 surfaced and registered
as `R-option-beta-coverage-gap-uc-a-uc-c-shape`: **for every
(`topic_subject`, `description`-shape) tuple the intake router can
observe, what does the Sprint 31 `alternate_candidate_use_cases`
projection slot (Option β) produce?**

The matrix has three readers:

1. **The Sprint 35 dev + deliver-agent + human** at sub-sprint close —
   to choose between decision (a) re-anchor `iteration_governance.md`
   §7.2 worked example to a UC pair that DOES fall within Option β
   coverage, OR decision (b) open a follow-on R-item
   `R-option-gamma-alternate-uc-surveyor-design` for the post-routing
   drift shape coverage gap. See §5.
2. **Future readers of `iteration_governance.md` §7.2** — to verify
   the worked example matches the implemented mechanism (after the
   conditional fold-back, if (a) is confirmed).
3. **Future architects of a successor mechanism** — to know which
   shapes Option β covers and which it does not, before designing a
   replacement / extension.

## 2. Method

### 2.1 Probe corpus design

The corpus is a deliberate subset of the (`topic_subject` ×
`description`-shape) cartesian. **Topic_subject** axis takes 7 values: 4
weak-prior topics where Option β has potential coverage (`Ad Support`,
`Payments`, `Technical Support`, `Account Support`), 3 strong-prior
topics as controls (`Replies or Messaging`, `Delete My Account or
Data`, `Report a Safety Issue`), and 1 handover-only sample (`Delivery`)
to confirm the override path. **Description_shape** axis takes 5
values: `single-issue`, `multi-issue-same-topic`, `cross-topic-drift`,
`ambiguous-soft`, `empty-generic`.

The full 7 × 5 = 35 cells pruned to 25 — 10 trivially redundant cells
removed (strong-prior topics × `multi-issue-same-topic` collapse to
single-issue since strong-prior topics have only one UC; strong-prior
× `empty-generic` / `ambiguous-soft` collapse to the same routing
output as single-issue; handover-only × multiple shapes covered by
existing Sprint 10 / 11 / 32 tests). Pruning rationale recorded in
`eval_interactive/case_specs/probe/option_beta_coverage/_manifest.md`.

Each probe CaseSpec follows the standard `case_specs` schema PLUS a
`probe_metadata` block naming the dimension cell and a
`probe_observation_recipe` naming the extraction path. The
`expected.*` and `scoring.*` blocks carry minimal valid values to
satisfy the loader's `__post_init__` validators
(`eval_interactive/eval_interactive/case_spec/schema.py:170-204`); the
values are **not** used as a regression rubric — probe CaseSpecs are
observation instruments, NOT regression gates per Sprint 35 §5.1.
`max_turns: 2` keeps LLM cost bounded.

### 2.2 Run protocol

- Backend: local server at `http://localhost:8080`, `local` profile,
  real LLM channel (Moonshot/Kimi `moonshot-v1-32k` per
  `application-local.yml`). Per `feedback_mocked_llm_cannot_prove_prompt_causal_change.md`:
  the probe MUST run against a real LLM. Sprint 32 cs32t01 produced
  the original finding on the same channel.
- Harness invocation:
  ```bash
  cd eval_interactive
  uv run eval-interactive run --path case_specs/probe/option_beta_coverage/ \
      --label sprint-35-option-beta-probe
  ```
- Run output: `eval_interactive/results/20260516-140339/results.json`
  (label `sprint-35-option-beta-probe`, 25 cases, elapsed
  108 718 ms).

### 2.3 Extraction recipe

For each probe CaseSpec, the matrix cell value is derived from the
turn-0 trace. Two paths:

1. **Default path (19 of 25 cases)**: read from `results.json`
   directly:

   ```bash
   jq '.case_results[] |
       select(.case_id == "<probe_id>") |
       {active: .per_turn_trace[0].projection.session.active_use_case,
        alternates: .per_turn_trace[0].projection.alternate_candidate_use_cases,
        candidates: .per_turn_trace[0].projection.candidate_use_cases}' \
     eval_interactive/results/20260516-140339/results.json
   ```

2. **Fallback path (6 cases with `stop_reason: contract_violation` and
   `per_turn_trace: []`)**: read directly from the backend trace
   endpoint, since the harness emits the placeholder empty trace for
   contract-violation case_results. The full turn-0 trace remains
   recoverable from `/v1/demo/sessions/<session_id>/trace`:

   ```bash
   curl -s "http://localhost:8080/v1/demo/sessions/<session_id>/trace" \
     | jq '.[0] | {active: .activeUseCase,
                   pc: (.projectedContext | fromjson |
                        {alternates: .alternate_candidate_use_cases,
                         candidates: .candidate_use_cases})}'
   ```

   The 6 sessions hit the contract-violation path because the
   `(weak-prior topic) × (description that does NOT trigger any B2
   bias / UC-K regression regex)` combination produces a routing
   outcome of `AMBIGUOUS` at session creation, leaves
   `active_use_case = null`, and within the `max_turns: 2` budget the
   bot loop did not invoke `classify_use_case` to commit a UC before
   the simulator's `active_use_case` contract check fired. This
   itself is matrix evidence: those cells are the unambiguous
   `AMBIGUOUS` cells of the routing surface.

The recovered values are cached in
`/tmp/matrix_extract.json` during the dev session but the
authoritative source is the two recipes above against
`eval_interactive/results/20260516-140339/results.json` + the
backend trace endpoint.

## 3. Raw matrix

Rows = `topic_subject`. Columns = `description`-shape. Each cell
records the observed `alternate_candidate_use_cases` value
(non-empty → `AMBIGUOUS{...}`; empty + active UC set → `ROUTED{...}`).
The `candidate_use_cases` value is noted in parentheses where it
differs informatively. Source = `results/20260516-140339/results.json`
unless noted `[trace]` = recovered from backend `/trace` endpoint per
§2.3 fallback.

| topic_subject \ shape | single-issue | multi-issue same-topic | cross-topic-drift | ambiguous-soft | empty-generic |
|---|---|---|---|---|---|
| **Ad Support** (weak-prior; B2 bias eligible) | `ROUTED{UC-A}` alts=`[]` (visibility-shape; B2 ADS_VISIBILITY_BIAS fired) ◇ `ROUTED{UC-B}` alts=`[UC-A,UC-FP,UC-H]` (posting-shape; no bias; AMBIGUOUS routed mid-loop) | `AMBIGUOUS{UC-A,UC-B,UC-FP,UC-H}` `[trace]` | `AMBIGUOUS{UC-A,UC-B,UC-FP,UC-H}` `[trace]` ★ §7.2 worked-example cell — **UC-C absent** | `AMBIGUOUS{UC-A,UC-B,UC-FP,UC-H}` | `AMBIGUOUS{UC-A,UC-B,UC-FP,UC-H}` `[trace]` |
| **Payments** (weak-prior; NOT in B2 bias set) | `ROUTED{UC-F}` alts=`[UC-I]` (AMBIGUOUS routed mid-loop to UC-F) | `ROUTED{UC-I}` alts=`[UC-F]` (AMBIGUOUS routed mid-loop to UC-I) | `AMBIGUOUS{UC-F,UC-I}` — **UC-D absent** | `AMBIGUOUS{UC-F,UC-I}` | (pruned — same shape as single-issue) |
| **Technical Support** (weak-prior; UC-K regression override eligible) | `AMBIGUOUS{UC-E,UC-K}` `[trace]` (FAQ-shape; no regression) ◇ `ROUTED{UC-K}` alts=`[]` (regression-shape; UC-K override fired) | `AMBIGUOUS{UC-E,UC-K}` `[trace]` | `AMBIGUOUS{UC-E,UC-K}` `[trace]` — **UC-F absent** | (pruned) | (pruned) |
| **Account Support** (weak-prior, single-UC; B2 bias eligible) | `ROUTED{UC-D}` alts=`[UC-A,UC-B,UC-C,UC-E,UC-F,UC-FP,UC-G,UC-H,UC-I,UC-J,UC-K]` cands=`[UC-A..UC-K (12)]` (B2 ACCOUNT_LOGIN_BIAS fired) ⚠ 12-UC fallback list | (pruned — Account Support has single UC) | `ROUTED{UC-D}` alts=`[]` cands=`[12]` (B2 login bias fires before messaging bias) — **UC-C absent** | `AMBIGUOUS{UC-A..UC-K (12)}` cands=`[12]` ⚠ 12-UC fallback | (pruned) |
| **Replies or Messaging** (strong-prior UC-C; control) | `ROUTED{UC-C}` alts=`[]` cands=`[UC-C]` | (pruned — strong-prior single UC) | `ROUTED{UC-C}` alts=`[]` cands=`[UC-C]` — **UC-A absent** | `ROUTED{UC-C}` alts=`[]` cands=`[UC-C]` | (pruned) |
| **Delete My Account or Data** (strong-prior UC-G; control) | `ROUTED{UC-G}` alts=`[]` cands=`[UC-G]` | (pruned) | `ROUTED{UC-J}` alts=`[]` cands=`[UC-G]` ⚠ active mid-loop reclassified from strong-prior UC-G to UC-J | (pruned) | (pruned) |
| **Report a Safety Issue** (strong-prior UC-J; control) | `ROUTED{UC-J}` alts=`[]` cands=`[UC-J]` | (pruned) | `ROUTED{UC-J}` alts=`[]` cands=`[UC-J]` — **UC-I absent** | (pruned) | (pruned) |
| **Delivery** (handover-only; sample) | (pruned) | (pruned) | `ROUTED{UC-J}` alts=`[]` cands=`[]` (TOPIC_OVERRIDES FRAUD_SAFETY_PATTERN fired) | (pruned) | (pruned) |

Legend:
- ◇ = two probe CaseSpecs share the same dimension cell with different
  description shapes inside the broader `single-issue` category;
  both observations recorded for completeness.
- ★ = the §7.2 worked-example cell — directly tests the Sprint 32
  finding.
- ⚠ = unexpected observation worth analytical commentary (see §4).

Per-cell source citations (`probe_id` → result location):

```
probe_ad_support_single_uc_a_visibility_bias            → results.json case_id=probe_ad_support_single_uc_a_visibility_bias .per_turn_trace[0].projection
probe_ad_support_single_uc_b_posting_neutral            → results.json case_id=probe_ad_support_single_uc_b_posting_neutral .per_turn_trace[0].projection
probe_ad_support_multi_uc_a_uc_h_neutral                → /v1/demo/sessions/5cd217da-ada6-4dca-b4d0-6044ceda7554/trace [0]
probe_ad_support_cross_topic_uc_c_drift                 → /v1/demo/sessions/9d07f987-3a4f-4a1b-87e6-562b0dbd6cc5/trace [0]    ★
probe_ad_support_ambiguous_soft_alice_shape             → results.json case_id=probe_ad_support_ambiguous_soft_alice_shape .per_turn_trace[0].projection
probe_ad_support_empty_generic                          → /v1/demo/sessions/792d2bde-de2f-4491-9dd8-c8b0508eef29/trace [0]
probe_payments_single_uc_f_faq_neutral                  → results.json case_id=probe_payments_single_uc_f_faq_neutral .per_turn_trace[0].projection
probe_payments_multi_uc_f_uc_i_neutral                  → results.json case_id=probe_payments_multi_uc_f_uc_i_neutral .per_turn_trace[0].projection
probe_payments_cross_topic_uc_d_drift                   → results.json case_id=probe_payments_cross_topic_uc_d_drift .per_turn_trace[0].projection
probe_payments_ambiguous_soft                           → results.json case_id=probe_payments_ambiguous_soft .per_turn_trace[0].projection
probe_tech_support_single_uc_e_faq_neutral              → /v1/demo/sessions/3c26ed47-38d5-4d11-a3a8-b72cb5a1364d/trace [0]
probe_tech_support_regression_uc_k_override             → results.json case_id=probe_tech_support_regression_uc_k_override .per_turn_trace[0].projection
probe_tech_support_multi_uc_e_uc_k_neutral              → /v1/demo/sessions/da913451-5ce7-470a-81f9-37157222b9bb/trace [0]
probe_tech_support_cross_topic_uc_f_drift               → /v1/demo/sessions/96742752-0a1d-474a-b8f8-71209360e71b/trace [0]
probe_account_support_single_uc_d_login_bias            → results.json case_id=probe_account_support_single_uc_d_login_bias .per_turn_trace[0].projection
probe_account_support_cross_topic_uc_c_drift_with_login_bias → results.json case_id=probe_account_support_cross_topic_uc_c_drift_with_login_bias .per_turn_trace[0].projection
probe_account_support_ambiguous_soft_no_bias            → results.json case_id=probe_account_support_ambiguous_soft_no_bias .per_turn_trace[0].projection
probe_replies_messaging_single_uc_c_strong_prior        → results.json case_id=probe_replies_messaging_single_uc_c_strong_prior .per_turn_trace[0].projection
probe_replies_messaging_cross_topic_uc_a_drift          → results.json case_id=probe_replies_messaging_cross_topic_uc_a_drift .per_turn_trace[0].projection
probe_replies_messaging_ambiguous_soft                  → results.json case_id=probe_replies_messaging_ambiguous_soft .per_turn_trace[0].projection
probe_delete_single_uc_g_strong_prior                   → results.json case_id=probe_delete_single_uc_g_strong_prior .per_turn_trace[0].projection
probe_delete_cross_topic_safety_drift                   → results.json case_id=probe_delete_cross_topic_safety_drift .per_turn_trace[0].projection
probe_safety_single_uc_j_strong_prior                   → results.json case_id=probe_safety_single_uc_j_strong_prior .per_turn_trace[0].projection
probe_safety_cross_topic_uc_i_payment_drift             → results.json case_id=probe_safety_cross_topic_uc_i_payment_drift .per_turn_trace[0].projection
probe_handover_delivery_safety_override                 → results.json case_id=probe_handover_delivery_safety_override .per_turn_trace[0].projection
```

## 4. Observations + analytical commentary

### 4.1 The §7.2 worked-example cell

`probe_ad_support_cross_topic_uc_c_drift` — form `topic_subject = "Ad
Support"`, description mentions a UC-C messaging concern alongside an
Ad Support listing complaint — produces
`AMBIGUOUS{UC-A, UC-B, UC-FP, UC-H}` with **UC-C absent** from the
`alternate_candidate_use_cases` slot. The candidate set
(`candidate_use_cases`) is identical to the alternate set: both are
the topic_subject family. This empirically **confirms** the Sprint 32
§13 architectural finding (the original cs32t01 probe produced
`['UC-A','UC-FP','UC-H']` with UC-B filtered as active; this
probe corpus reproduces the same shape with `active = null` so all
four candidates are visible in the alternate slot).

The mechanism that bounds the alternates is the deterministic intake
router in `UseCaseRouter.routeNonBlocking`
(`server/src/main/java/com/gumtree/csagent/service/runtime/UseCaseRouter.java`):

1. The router consults `UseCaseRegistryService.getCandidateUcsForTopic`
   on the form-supplied `topic_subject` and returns ONLY UCs that map
   to that topic in
   `server/src/main/resources/config/use-case-registry.yaml`. UC-C
   maps to `Replies or Messaging`, not `Ad Support`, so UC-C is
   structurally excluded from the candidate list for an `Ad Support`
   form regardless of description content.
2. The B2 phrase bias in
   `matchAccountMessagingBias` can re-route the active UC across topic
   families (e.g., `Ad Support` + "ads not showing" → UC-A; `Account
   Support` + "messages not arriving" → UC-C). But the bias path
   resolves to `RoutingResult.routed(...)` — it does **not** widen
   the AMBIGUOUS candidate list. The B2 bias is a single-UC override,
   not an alternate-surface widener.
3. The AMBIGUOUS branch is reached only after handover-only, strong-
   prior, UC-K regression, and B2 bias have all failed to match. At
   that point the candidate list is fixed to the topic_subject's
   family by step 1.

So Option β can never surface a UC outside the
form_context.topic_subject's family on the AMBIGUOUS path. The
§7.2 worked example as currently written
(`docs/current/iteration_governance.md:615-639`) describes a
hypothetical Sprint 18 fix where the LLM sees a soft UC-A↔UC-C signal
on a drift turn — but UC-C will **never** appear in the alternate
slot for an Ad Support form, because UC-C is not an Ad Support
candidate. The worked example as written cannot be implemented under
Option β as shipped.

### 4.2 Generalization across the three multi-candidate weak-prior topics

The same pattern appears symmetrically on Payments and Technical
Support cross-topic-drift cells:

- `probe_payments_cross_topic_uc_d_drift` (Payments form + UC-D
  account mention) → `AMBIGUOUS{UC-F, UC-I}` — UC-D **absent**.
- `probe_tech_support_cross_topic_uc_f_drift` (Tech Support form +
  UC-F payment mention) → `AMBIGUOUS{UC-E, UC-K}` — UC-F **absent**.

The "alternate slot is bounded by the form_context.topic_subject
family" rule is general, not specific to UC-A↔UC-C. Option β is a
**within-topic** alternate surface, not a cross-topic one.

### 4.3 Where Option β DOES work — the within-topic cases

The Option β surface works as intended when both UCs share a
topic_subject:

- **Ad Support** (4 candidates UC-A / UC-B / UC-FP / UC-H): all four
  surface on the AMBIGUOUS path whenever no bias / regression
  override fires. The multi-issue same-topic probe
  (`probe_ad_support_multi_uc_a_uc_h_neutral`) is the central
  evidence — the user mentions UC-A and UC-H in the description, both
  are in the alternate slot, the LLM owns whether to act on it.
- **Payments** (2 candidates UC-F / UC-I): both surface on AMBIGUOUS;
  when the LLM picks one mid-loop, the other appears in the alternate
  slot as the projected soft signal. Symmetric to Ad Support.
- **Technical Support** (2 candidates UC-E / UC-K): same pattern when
  the UC-K regression override does NOT fire.

This is exactly what Sprint 32 neighbor #1 (`cs32n01_uc_a_uc_fp_drift`)
demonstrated empirically. The probe corpus generalizes that finding to
three topic_subject families.

### 4.4 Surprising observations (flagged for OQ + analysis)

**4.4.1 Account Support produces a 12-UC fallback list, not [UC-D].**

`probe_account_support_single_uc_d_login_bias` (active=UC-D, alts=
11 UCs minus UC-D) and `probe_account_support_ambiguous_soft_no_bias`
(active=None, alts=12 UCs — the entire UC registry) both show
`candidate_use_cases` carrying the full 12-element UC list rather than
the expected `[UC-D]` (the single UC mapped to `Account Support` in
the registry).

This matches the Sprint 31 §13 fix-iteration #1 observation on
`cs_interactive_040`, which produced a 12-element alternates fallback.
The behaviour is reproducible on Account Support more broadly. The
likely cause is `UseCaseRegistryService.getCandidateUcsForTopic` (or
its caller in `UseCaseRouter` / `SessionManager`) emitting an
"all-UCs" fallback when the weak-prior topic has only one UC, instead
of returning a singleton `[UC-D]`. Sprint 35 does NOT investigate the
root cause (read-only sprint per §6 hard fences) but flags this for
follow-up. **Candidate R-item**: `R-account-support-12-uc-fallback-investigation`
(diagnostic; layer `prompt_projection` — the slot widens too much
when it should narrow to `[UC-D]`).

**4.4.2 `probe_delete_cross_topic_safety_drift` mid-loop reclassification.**

The Delete topic is strong-prior UC-G. The probe form supplied
`topic_subject = "Delete My Account or Data"` + description
mentioning harassment. Initial routing → UC-G (candidates=[UC-G] in
the projection). But the projection on turn 1 shows
`active_use_case = UC-J`, indicating the LLM invoked
`classify_use_case` mid-loop and re-routed across the strong-prior
boundary. This is a different surface than Option β (`classify_use_case`
runtime tool, NOT the intake-router alternate slot) and out of scope
for the §7.2 question, but worth noting: cross-strong-prior drift is
reachable via the LLM's `classify_use_case` path even when the
intake router locked the session to a strong-prior UC. This is an
**Option γ-shaped** capability already partially present, just not in
the intake-router's alternate surface.

**4.4.3 Cross-topic drift on B2-eligible topics.**

`probe_account_support_cross_topic_uc_c_drift_with_login_bias` was
designed to surface a B2-bias ordering decision: the description
contained BOTH a login-shaped fragment ("locked out of my account")
AND a messaging-shaped fragment ("messages are not arriving"). The
matrix observation: `ROUTED{UC-D}` with empty alternates. This
confirms the documented bias ordering in
`UseCaseRouter.matchAccountMessagingBias` (UC-A wins, then UC-D, then
UC-C). The cross-topic UC-C signal is **swallowed** by the bias, not
surfaced as an alternate. This is a second flavour of "the alternate
surface cannot help here" — the B2 bias path takes a single-UC
deterministic route and produces empty alternates.

## 5. Recommended decision

**Primary recommendation: (a) re-anchor `iteration_governance.md`
§7.2 worked example.**

The §7.2 worked example currently uses UC-A↔UC-C as the hypothetical
drift target. The probe matrix confirms (§4.1, §4.2) that UC-A↔UC-C
drift cannot be addressed by Option β at all — the alternate slot is
structurally bounded by the form_context.topic_subject family, and
UC-A ("Ad Support") and UC-C ("Replies or Messaging") belong to
different families. Any reader who tries to implement the §7.2
worked example as written will hit the same Sprint 32 finding.

Re-anchoring the worked example to a UC pair that DOES fall within
Option β coverage aligns the governance doc with the implemented
mechanism. The natural choice is **UC-A↔UC-FP** — both share the
`Ad Support` topic_subject, Sprint 32 neighbor #1
(`cs32n01_uc_a_uc_fp_drift`) already validated this pair empirically,
and the case is shaped narrowly enough that the existing
`alternate_candidate_use_cases` slot is the load-bearing soft signal
the LLM reads. Alternative anchor candidates (also within-topic):
UC-F↔UC-I (Payments), UC-E↔UC-K (Technical Support), UC-A↔UC-H or
UC-A↔UC-B (Ad Support). UC-A↔UC-FP is preferred because it has the
strongest existing empirical evidence (Sprint 32 case family) and the
clearest customer-facing semantic split (visibility question vs
deletion-explanation question, both on the same listing).

The §7.2 fold-back is a deliver-agent commit at Sprint 35 close per
the Framing B contract in `docs/sprint_objective.md` §2 and the M1
plan §3 Sprint 35 row. Sprint 35 dev does NOT stage the
`iteration_governance.md` edit.

**Follow-on recommendation: (b) ALSO surface
`R-option-gamma-alternate-uc-surveyor-design`.**

Per Sprint 35 §10 stop condition #10, decision (a) is primary; (b) is
surfaced as a separate FOLLOW-ON architecture R-item, not a competing
choice. The matrix surfaces a real coverage gap: cross-topic drift —
where the user's underlying issue belongs to a different
`topic_subject` than what the form_context names — is **not**
addressable by the current Option β alternate surface. If this gap
matters for production behaviour (the Alice bad case's UC-A vs UC-H
shape is *within*-topic and so Option β + Sprint 33 disambiguation
signals address it, but other future bad cases may surface cross-
topic drift), a successor mechanism — call it Option γ
`AlternateUseCaseSurveyor` — would scan for cross-topic drift
signals after the intake router has committed. That design is its
own scope and belongs in a separate sub-sprint / milestone; Sprint 35
surfaces the R-item rather than designing it inline.

Per §1.7 of the Constitution: the recommendation does NOT introduce a
new keyword / regex / if-else / enum / per-UC matrix in the runtime.
It only proposes that the governance worked example match the
implementation, and that a follow-on R-item be opened to track the
architectural gap.

### 5.1 Per-cell evidence supporting decision (a)

- §7.2 worked-example shape impossible under Option β: §4.1 cites
  `probe_ad_support_cross_topic_uc_c_drift` —
  `AMBIGUOUS{UC-A,UC-B,UC-FP,UC-H}` with UC-C absent.
- UC-A↔UC-FP works under Option β: Sprint 32 §13 +
  `probe_ad_support_multi_uc_a_uc_h_neutral` (UC-H is a sibling within
  the Ad Support family; the same surface that would carry UC-FP).
- The Option β-coverable cases in the matrix all share a
  topic_subject: §4.3 generalizes across Ad Support / Payments /
  Technical Support.

### 5.2 Per-cell evidence supporting follow-on (b)

- Cross-topic drift bounded by topic_subject family: §4.2 cites
  Payments + Tech Support cross-topic-drift cells. The gap is
  systemic, not Ad Support-specific.
- Even where mid-loop reclassification IS possible (§4.4.2: Delete →
  UC-J via `classify_use_case`), it is the LLM's tool call, not the
  intake-router's projection surface. The "soft signal at intake
  time" channel (what Option β provides) has no analogue for
  cross-topic drift.
- The 12-UC Account Support fallback (§4.4.1) is a separate concern
  that may interact with any future Option γ design — flag.

## 6. Draft R-item text for `R-option-gamma-alternate-uc-surveyor-design`

(Deliver-agent decides at Sprint 35 close whether to open this R-item
in `docs/action_bank.md` based on the human-confirmed decision. The
text below is a draft for that decision.)

```
| id | source | description |
|----|--------|-------------|
| R-option-gamma-alternate-uc-surveyor-design | Sprint 35 Option β coverage matrix `docs/diagnostics/option_beta_coverage_matrix.md` §4.2 + §5.2 | `prompt_projection` / `semantic_planner`; **proposed** for next-milestone consumption (M2 candidate). The Sprint 35 matrix empirically confirms that the existing Option β `alternate_candidate_use_cases` projection slot is bounded by the form_context.topic_subject family — cross-topic drift signals never surface in the alternate slot. The probe corpus generalizes the Sprint 32 finding from UC-A↔UC-C across Ad Support / Payments / Technical Support cross-topic-drift cells. Scope for the consuming sprint: design (NOT implement) a successor mechanism that surfaces cross-topic drift candidates AFTER the intake router has committed. Three sub-options to evaluate: (γ.1) a `prompt_projection` slot fed by a lightweight description-shape classifier that flags "description mentions a UC outside the topic_subject family" as a soft signal — minimal new code, LLM owns the response; (γ.2) a live `AlternateUseCaseSurveyor` Java service that runs deterministic regex / embedding similarity over the description and emits cross-topic candidate UCs into the projection slot — more code, more deterministic, but introduces a new §1.7 semantic-hardcode boundary case; (γ.3) defer to LLM `classify_use_case` tool — let the bot loop's existing reclassification path (already observed in §4.4.2 on the Delete case) carry the load, with possibly a prompt teaching paragraph guiding when to invoke. The consuming sprint should walk the §3 layer classification, the §1.7 forbidden-list red lines, and the §4 generalization-coverage stanza for each sub-option before picking one. Acceptance bar: a design freeze document at `docs/proposals/option_gamma_alternate_uc_surveyor.md` plus a stanza-filled `docs/sprint_objective.md` for the implementation sprint. Disposition: **proposed (Sprint 35 matrix finding; succeeds R-option-beta-coverage-gap-uc-a-uc-c-shape after it is closed by the §7.2 fold-back).** |
```

The deliver-agent may rewrite the description for `action_bank.md`
register conventions; the text above captures the substance.

## 7. Lifecycle

This document is the Sprint 35 diagnostic output. It is referenced
by:

- `docs/sprint_objective.md` (Sprint 35) §2, §5 — the contract that
  produces this artefact.
- `docs/milestone_objective.md` (M1) §5 — secondary observation; M1
  acceptance bar.
- `docs/action_bank.md` `R-option-beta-coverage-gap-uc-a-uc-c-shape`
  (`docs/action_bank.md:667`) — the R-item this artefact closes.
- `docs/current/iteration_governance.md` §7.2 — the governance
  worked-example the recommendation re-anchors (post-Sprint-35-close
  deliver-agent fold-back, if decision (a) confirmed).

The matrix may be re-run on any future sub-sprint to refresh the
observation against current `UseCaseRouter` behaviour. The probe
corpus stays at
`eval_interactive/case_specs/probe/option_beta_coverage/` as the
baseline reference.
