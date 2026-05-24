# Curated Bad-Case Suite

**Authored:** 2026-05-16; refined 2026-05-17 (tiering + human-judgment-gate clarification + N=2 downgrade rule)
**Source-of-truth:** this file (lifecycle ledger) + `iteration_governance.md` §5.6 / §5.6.1 / §5.6.2 / §5.6.3 (governance) + `compact/sprint-deliver-orchestrator.md` "Workflow inputs — Path 2" (operational triage + 4-route fit + edge case handling for deliver-agent)
**Owners:** deliver-agent + human (jointly maintained)

## Purpose

The bad-case suite is the **new primary acceptance gate** for sprint and milestone close per `iteration_governance.md` §5.6. It replaces the smoke composite_score / pass-rate / judge dimensions, which were demoted to observation per §5.5 due to multiple confounding sources (external LLM provider drift, judge calibration variance, mocked-vs-real-LLM gap, rubric weighting uncertainty).

**Critical: this is a human-judgment gate, NOT a programmatic gate.** Per §5.6 (2026-05-17 refinement): the deliver-agent + human MANUALLY REVIEW per-case traces; the closure_criterion field is GUIDANCE for human review, not a binary programmatic match. Early-stage, the eval rubric weights themselves are unstable; human qualitative judgment is the only reliable signal until the rubric is independently validated.

Each bad case in this directory is:

1. A real session or sprint-derived finding the deliver-agent + human jointly agree is **load-bearing** for the release-gate trajectory.
2. Encoded as a CaseSpec with the standard schema PLUS the §5.6-required `bad_case_metadata` block and `closure_criterion` field.
3. Verified by human inspection of expected behaviour from the user perspective — **NOT** by bot trace text, **NOT** by §1.7 forbidden patterns.

## Loader contract

Flat layout per Sprint 29 / Sprint 32 precedent. Loader globs `*.yaml`; this markdown file is skipped. Load + run:

```bash
cd eval_interactive && uv run eval-interactive run --path case_specs/bad_cases/
```

## Lifecycle ledger

| case_id | tier | source_session_id | surfaced_by | surfaced_date | current status | M1 result | M2 result | M3 result | M4 result | closure_criterion summary |
|---|---|---|---|---|---|---|---|---|---|---|
| `alice_uc_a_uc_h_misclass` | **core** | `3772e56b-caa7-4e0a-84fc-75a26ffbe2b2` | human (Alice mock, real-LLM session) | 2026-05-16 | **active** | (in-flight) | — | **IMPROVING** | **IMPROVING** | bot routes UC-A visibility question OR asks one focused clarifying question to disambiguate visibility vs appeal, instead of stamping UC-H through the intake-locked path; bot does NOT enter a multi-turn dead loop asking "why do you think it was removed" when the user clearly does not know |
| `cs012_uc_fp_late_phone_failure_path` | **core** | `570Q5000008hx9tIAA` | Wave A5 semantic review (cs_interactive_012) | 2026-04-30 | **active** | — | — | **IMPROVING** | **IMPROVING** | bot runs UC-FP resolve-first flow (customer + listing + moderation context, then policy-grounded reposting guidance) instead of escalating purely on a late phone/human request; bot does NOT fabricate a non-grounded removal reason |
| `cs015_uc_fp_appeal_edit_repost` | **core** | `570Q5000008WmXxIAK` | Wave A6 semantic re-review (cs_interactive_015) | 2026-04-30 | **active** | — | — | **FAIL** | **FAIL** | bot retrieves moderation context, explains the specific policy that flagged the ad, and gives clear edit-and-repost guidance (UC-FP), NOT a UC-K technical-defect intake on "change" keyword; late escalation without explanation attempt is failure |
| `cs066_uc_k_in_app_feature_regression` | **core** | `570Q5000008fBsXIAU` | Codex round 6 §P0 + Sprint 4 §E3 (cs_interactive_066) | 2026-05-05 | **active** | — | — | **PASS** | **PASS** | bot recognises "option that used to exist has disappeared" as UC-K technical regression, collects platform + repro intake fields, creates UC-K case, hands over with intake_complete_for_uc_k; NOT a UC-E generic feature explanation |
| `cs029_uc_d_account_locked_callback` | **core** | `570Q5000008kDiPIAU` | Sprint 4 §E2 reviewer correction (cs_interactive_029) | 2026-05-05 | **active** | — | — | **PASS** | **PASS** | bot recognises "account locked" as UC-D (not UC-C messaging), runs account-state retrieval, and routes to business team via user_requested handover when the user explicitly requests a callback; ALL-CAPS persona does NOT trigger user_distress alone |
| `cs095_uc_d_email_recovery_misroute` | **core** | `570Q5000008U5C9IAK` | Sprint 21 Wave A5/A6 L3 re-review (cs_interactive_095) | 2026-05-14 | **active** | — | — | **FAIL** | **FAIL** | bot recognises wrong-email-on-account as UC-D dominant signal with UC-A as downstream symptom; bot does NOT produce T1 placeholder without follow-on tool call, leak internal Salesforce source IDs, or duplicate the user's first name in the greeting |
| `cs011_uc_c_faq_miss_not_distress` | **core** | `570Q5000008NWIjIAO` | Sprint 4 §E1 supporting fix (cs_interactive_011) | 2026-05-05 | **active** | — | — | **IMPROVING** | **IMPROVING** | bot runs FAQ tool sequence on the password-reset-loop pattern and escalates with reason=faq_miss_threshold_exceeded when FAQ exhausts, NOT user_distress on cooperative descriptive seed text; user_distress is reserved for the deterministic Sprint B1 DISTRESS_PATTERN family |
| `cs014_uc_c_faq_miss_not_distress` | **core** | `570Q5000008u9gjIAA` | Sprint 2.1 P1 Codex-review follow-up (cs_interactive_014) | 2026-05-05 | **active** | — | — | **PASS** | **PASS** | bot runs FAQ tool sequence on admin-mediated-revert question and escalates with reason=faq_miss_threshold_exceeded when FAQ has no covering article, NOT user_distress on "I'd be happy enough if ..." cooperative wording |
| `cs001_uc_c_mechanical_template_escalate` | scope-relevant | `570Q5000008kr6LIAQ` | Sprint 21 Wave A5/A6 L3 batch (cs_interactive_001) | 2026-05-14 | **active** | — | — | **PASS** | **PASS** | bot acknowledges user's specific Replies/Messaging complaint by paraphrasing the actual issue, runs the FAQ tool sequence, and hands over with reason=faq_miss_threshold_exceeded if FAQ exhausts; bot does NOT open with a mechanical "I'm having difficulty resolving this" template before any tool call |
| `wmkb_uc_a_trader_flag_secondary_uc_h` | scope-relevant | `570Q5000008wmKbIAI` | Wave A2.1 reclassification map (legacy migration) | 2026-04-30 | **active** | — | — | **IMPROVING** | **IMPROVING** | bot recognises "trader flag wrong on account" as UC-A account/profile question, gives policy-grounded guidance on changing account-type; bot asks one clarifying question if the user wants to appeal (UC-H) vs change setting (UC-A); bot does NOT lock into UC-H intake without an explicit appeal request |
| `iwzx_uc_k_advert_on_hold_restore` | scope-relevant | `570Q5000008iwZxIAI` | Wave A2.1 reclassification map (legacy migration) | 2026-04-30 | **active** | — | — | **FAIL** | **FAIL** | bot retrieves moderation context to determine policy-driven (UC-FP) vs system-driven (UC-K) hold cause; routes to UC-K intake-and-handover OR UC-FP policy-grounded explanation accordingly; bot does NOT respond with generic "your ad is under review, please wait" template |
| `fg5q_uc_fp_phone_rejected_repost` | scope-relevant | `570Q5000008fG5qIAE` | Wave A2.1 reclassification map (legacy migration) | 2026-04-30 | **active** | — | — | **PASS** | **PASS** | bot retrieves moderation context, distinguishes policy rejection (UC-FP — explain rule, give in-app messaging alternative) from technical-validation rejection (UC-K intake); bot does NOT respond with generic "please follow the posting guidelines" template without naming the actual rule |

Ledger updates per milestone close: deliver-agent + human jointly record PASS / FAIL / IMPROVING per case row, citing the milestone close run path. **M4-Eval-Cleanup close**: all 12 verdicts UNCHANGED from M3 (cleanup milestone, zero bot-code change); the 5 PASS cases (cs001, cs014, cs029, cs066, fg5q) are now downgrade-ELIGIBLE per §5.6.3 (2 consecutive milestone-close PASS) but remain `active` pending a joint deliver-agent + human downgrade decision at a future bad-case-driven milestone close. See "M4-Eval-Cleanup close" section below.

## Tier definitions (per `iteration_governance.md` §5.6.1)

- **`core`** — re-run at every milestone close regardless of scope. Load-bearing across all milestones (touches release-gate-relevant failure mode).
- **`scope-relevant`** — re-run only when the closing milestone explicitly names the case in its `milestone_objective.md` §5 acceptance bar. Relevant to a specific architectural surface that some milestones touch and others don't.
- **`closed-as-regression-guard`** — has met closure criterion in N ≥ 2 consecutive milestone closes (per §5.6.3 downgrade rule). Runs automatically; no human manual review unless auto-detection fires FAIL.
- **`archived`** — underlying failure surface structurally removed; no longer manifests. Removed from active runs; kept in directory as history. Requires deliver-agent + human joint decision.

## Lifecycle states (per §5.6 / §5.6.3)

- **Opened**: human + deliver-agent agree the failure is load-bearing; CaseSpec authored. Initial tier assignment (typically `core` for cross-cutting failures; `scope-relevant` for surface-specific).
- **Active**: failure persists across at least one milestone close. Each milestone records PASS / FAIL / IMPROVING per case in the ledger above.
- **`closed-as-regression-guard`**: triggered after **N ≥ 2 consecutive milestone closes with PASS**. Downgrade is NOT automatic on the N=2 trigger — deliver-agent + human must jointly confirm at a milestone close to apply the downgrade. After downgrade, case runs in suite automatically; if `case_results[].terminal_outcome` returns to FAIL OR `composite_score` collapses, the case auto-promotes back to active.
- **Archived**: structurally impossible. Requires explicit joint decision; documented in this ledger.

## Adding a new bad case (Path 2 — operational steps in compact)

When a new bad case surfaces (human use / colleague use / sprint-surfaced finding):

1. Human + deliver-agent confirm it is load-bearing per §5.6 criteria:
   - Influences release-gate trajectory.
   - Failure mode crosses ≥ 1 layer (not a single-component cosmetic).
   - Reproducible OR represents a typical scenario class.
   - Not a duplicate of an existing closed/archived case.
2. Per Path 2: trace + observation are handed to a research-agent (not directly to deliver-agent) for root-cause analysis + solution proposal. See `compact/sprint-deliver-orchestrator.md` "Workflow inputs — Path 2" for the full operational process (triage gate → research-agent proposal → encode → 4-route fit decision → downstream milestone loop).
3. After research-agent proposal lands and human selects the design, deliver-agent authors a new `<case_id>.yaml` here using the schema in any existing bad case + the §5.6 extension fields.
4. Deliver-agent assigns initial tier (`core` for cross-cutting; `scope-relevant` for surface-specific). Appends a row to the lifecycle ledger above.
5. Deliver-agent decides whether the case fits the current milestone scope (add to `milestone_objective.md` §5 acceptance bar) OR is queued for a future milestone (open as an R-item in `docs/action_bank.md` referencing the bad case).

A bad case can also be opened WITHOUT a Path 2 proposal — when the human + deliver-agent simply want to encode the observation as a regression guard without immediate fix scoping. In that case, the case enters with tier `scope-relevant` and is consumed by a future milestone whose scope reaches the relevant surface.

## Removing a bad case

Bad cases are NOT removed at closure; they stay as regression guards. Removal is only acceptable if the underlying failure mode becomes structurally impossible (e.g., the surface that produced it is deleted), and requires deliver-agent + human joint decision documented in this lifecycle ledger.

## Why this is not in `case_specs/case_families/`

The case-family directory follows the Sprint 20 G2 target / neighbor / negative / shadow split shape. The bad-case suite is a different artefact: one case per real surfaced failure, no neighbor / negative / shadow split required (the case itself IS the load-bearing evidence). Mixing the two schemas would dilute both contracts. Per Sprint 29 `_manifest.md` precedent (local manifest, intentionally not in root manifest), the bad-case suite is local-only.

## S-Eval-4 trial milestone-close dry-run calibration notes (2026-05-22)

### Posture

Per `docs/sprint_objective.md` §2.4 / §2.5, the S-Eval-4 dev + human jointly selected **Option A — synthetic / mocked trace**. Rationale: S-Eval-4 contract is `data + calibration; not code change`; OQ-S44.5 disposition already accepted synthetic Tier-2 evidence for S-Eval-3 close; the load-bearing deliverable of this sub-sprint is **`closure_criterion` wording quality calibration**, which is exercisable against pre-recorded documented bot behaviour from prior sprint handoffs WITHOUT a fresh real-LLM run; Option C (executor wiring fix) was rejected as scope expansion to be carried into S-Eval-5 alongside L3 work; Option B would not exercise Tier-2 due to OQ-S44.1 executor wiring inert path. Real-LLM end-to-end verification carries to S-Eval-5 / M3-Eval close.

The verdict column below is therefore **projected** against the documented current-bot behaviour cited in each row (Sprint 4 §E1 / §E2 / §E3 + Sprint 21 L3 batch + Sprint 32 + S-Eval-3 §12.7 carry-overs + the override `rationale:` blocks). It is NOT a real-LLM-run verdict. The wording assessment column is the **calibration deliverable**.

### Per-case projected verdict + wording assessment

| case_id | Projected verdict | Rationale | `closure_criterion` wording assessment |
|---|---|---|---|
| `cs012_uc_fp_late_phone_failure_path` | **FAIL** (projected) | Documented pre-override behaviour: bot treated late phone/human request as dominant signal and escalated without UC-FP resolve-first attempt. Current bot (post-S-Eval-3, no executor wiring) likely still over-weights the late request. | **Well-calibrated.** "Acknowledges → tools → factual_answer / faq_grounded" enumerates observable end-states. "Does NOT hand over … on the late phone request alone" names the specific failure mode. "Does NOT fabricate a non-grounded reason" is a Tier-0 grounding floor restatement. Human judgment is straightforward. |
| `cs015_uc_fp_appeal_edit_repost` | **FAIL** (projected) | Pre-override CaseSpec mis-pinned UC-K; bot historically routed "change" framing to UC-K technical intake. UC-FP resolve-first guidance gap is documented in the Wave A6 rationale. | **Well-calibrated.** Bifurcated end-state (edit-and-repost vs appeal) covers the two legitimate UC-FP outcomes. "Routes follow-up as UC-FP reposting question, NOT UC-K technical-defect intake" names the specific misroute. Human judgment is straightforward. |
| `cs066_uc_k_in_app_feature_regression` | **FAIL** (projected) | Pre-override bot answered with generic UC-E feature explanation; UC-K technical regression mis-routed. Even post-S-Eval-3 with populated `critical_steps`, executor.py:252 wiring inert path (OQ-S44.1) means Tier-2 `uc-k-intake-complete-before-handover` step does not gate in production eval-harness. | **Well-calibrated.** Names specific UC-K intake fields (platform, repro_steps_or_error_message) + tool sequence (create_case_controlled + intake_complete_for_uc_k handover). "Does NOT answer with generic feature-walkthrough (UC-E pattern)" disambiguates from the canonical mis-routed shape. Human judgment is straightforward. |
| `cs029_uc_d_account_locked_callback` | **FAIL** (projected) | Pre-override CaseSpec pinned UC-C (Replies/Messaging); the runtime deterministic fallback (`ControlKernel.inferFallbackUseCase`) correctly picks UC-D, so the runtime and pre-override spec disagreed. Current bot LLM may stamp UC-C on "can't advertise" pull or escalate purely on ALL-CAPS caps signal. | **Well-calibrated.** "Recognises 'my account is locked' as UC-D (NOT UC-C messaging)" names the specific mis-classification. "Does NOT loop on UC-C messaging-feature answers" + "ALL-CAPS does NOT trigger user_distress alone" name both layered failure modes. Human judgment is straightforward. |
| `cs095_uc_d_email_recovery_misroute` | **FAIL** (projected) | Pre-override CaseSpec mis-pinned UC-A; bot's actual T0 reply was UC-D-flavoured (steps to change contact email citing internal SF source_id ka44J000000gKxqQAE — internal leak). Also documented: T1 placeholder ("I'm looking into this for you") + duplicated greeting ("Hi Trish! Hi Trish"). Multi-layer failure. | **Mostly well-calibrated.** The 4-clause structure (UC-D recognition + no placeholder + no source_id leak + no duplicate greeting) covers each layer explicitly. Minor wording concern: "does NOT produce a T1 placeholder ('I'm looking into this for you') without a follow-on tool call in the same turn" — the term "placeholder" is partly defined-by-example. Rewording proposal: replace "placeholder reply" with "filler reply that does not name a concrete next step or trigger a tool call". Net assessment: **acceptable as-is for manual review**; the example phrase ("I'm looking into this for you") is a strong anchor. |
| `cs011_uc_c_faq_miss_not_distress` | **FAIL** (projected) | Documented: bot LLM stamped user_distress on sub-textual frustration reading; Sprint 4 §E1 runtime gate downgrades the canonical session reason but the LLM's PLAN still claims it. Closure_criterion separates the "bot's claim" (failure mode) from "runtime canonical reason" (Sprint 4 §E1 corrected). | **Well-calibrated.** Names the truthful Phase 2 §2.4 reason (faq_miss_threshold_exceeded) and the specific not-this reason (user_distress) with the rationale (Tier-0 semantic claim, deterministic detector required). "Bot may acknowledge frustration in wording WITHOUT elevating the enum" — explicit guidance on what is allowed vs not. Human judgment is straightforward. |
| `cs014_uc_c_faq_miss_not_distress` | **FAIL** (projected) | Same failure shape as cs011 (user_distress upgrade on cooperative seed text). The "I'd be happy enough if ..." cooperative phrasing is non-distress; bot's pre-override behaviour mis-stamped distress. | **Well-calibrated.** Mirrors cs011 structure. The cooperative-wording disambiguation ("'I'd be happy enough' is not a Sprint B1 DISTRESS_PATTERN match") is concrete. Human judgment is straightforward. |
| `cs001_uc_c_mechanical_template_escalate` | **FAIL** (projected) | Documented: turn-0 mechanical template "Hi Benjamin! I'm having difficulty resolving this. Let me connect you with a specialist." with zero acknowledgment of the user's specific complaint, before any tool call. Captured by Sprint 20 cs001 case family. | **Well-calibrated.** "Acknowledges user's specific complaint by paraphrasing the actual issue" names the positive behaviour; "does NOT open with mechanical 'I'm having difficulty resolving this' template before any tool call" names the specific failure mode with the actual template text. Human judgment is unambiguous. |
| `wmkb_uc_a_trader_flag_secondary_uc_h` | **IMPROVING** (projected; uncertain) | This is the closest the 17-pool comes to a UC-H surface (UC-H is secondary, not primary). Bot behaviour on trader-flag-wrong is not as well-documented in prior sprints as the cs_001/cs_011/cs_014 family. The wmkb override is legacy-migrated (empty supporting_turn_numbers); the failure shape is hypothesised from the override `rationale:`. | **Well-calibrated but unverified.** "Recognises trader-flag as UC-A; asks one clarifying question if ambiguous; does NOT lock into UC-H without explicit appeal request" mirrors Alice's well-tested UC-A↔UC-H disambiguation language. Acceptable as a regression guard but the projected verdict carries low confidence until a real-LLM run measures actual bot behaviour. **Recommend re-calibration at M3-Eval close after real-LLM evidence.** |
| `iwzx_uc_k_advert_on_hold_restore` | **IMPROVING** (projected; uncertain) | Legacy-migrated override (empty supporting_turn_numbers); failure shape hypothesised. Current bot may answer with "your ad is under review" template OR may correctly retrieve moderation context. | **Well-calibrated.** Bifurcated criterion (UC-K technical intake vs UC-FP policy explanation) requires the human to read the moderation-context retrieval result and judge accordingly — acceptable. "Does NOT respond with generic 'your ad is under review, please wait' template" names a specific failure mode. **Recommend re-calibration at M3-Eval close after real-LLM evidence.** |
| `fg5q_uc_fp_phone_rejected_repost` | **IMPROVING** (projected; uncertain) | Legacy-migrated; failure shape hypothesised. UC-FP policy-grounded explanation path is the documented direction for "ad keeps breaking rules". | **Well-calibrated.** Same bifurcated shape as iwzx (policy-driven UC-FP vs technical-validation UC-K). Both branches explicitly named. **Recommend re-calibration at M3-Eval close after real-LLM evidence.** |

### Wording calibration patterns observed

**Patterns that proved well-calibrated** (clear PASS / FAIL signal under manual review):

1. **Multi-state enumerated criteria** ("Bot reaches one of these end-states: (a) … (b) … (c) …" — Alice precedent). Each end-state is observable; human reader picks the closest match.
2. **Explicit FAIL conditions naming the documented failure mode** ("Does NOT hand over with reason=user_requested on the late phone request alone" / "Does NOT lock into UC-H intake without explicit appeal request"). Naming the specific anti-behaviour is more diagnostic than naming the expected behaviour alone.
3. **Quoted template / phrase examples as anchors** ("the mechanical 'I'm having difficulty resolving this' template" / "the T1 'I'm looking into this for you' placeholder"). When the failure mode is a specific bot-text artefact, quoting it disambiguates the human's read.
4. **Reference to the canonical enum value** ("reason=faq_miss_threshold_exceeded, NOT user_distress" / "intake_complete_for_uc_k handover"). The escalation_reason enum is the source of truth; closure_criterion that names the truthful enum value is unambiguous.

**Patterns that required minor calibration** (acceptable as-is but flagged):

1. **`cs095` "T1 placeholder" framing** — "placeholder" is defined-by-example with the quote ("I'm looking into this for you"); a stricter reading might ask "is X a placeholder?" without obvious answer. **Decision**: kept as-is because the quoted anchor phrase is strong enough; rewording proposal ("filler reply that does not name a concrete next step or trigger a tool call") added as fold-back candidate for future bad cases.
2. **Bifurcated criteria (UC-K vs UC-FP based on context)** — `iwzx` and `fg5q` both require the human to first determine which branch applies before judging. Acceptable because manual review is qualitative; would be **unworkable as a programmatic gate**. This reinforces the §5.6 2026-05-17 refinement that bad-case suite is human-judgment, not programmatic.

**Patterns NOT observed in any new bad case** (no §1.7-adjacent calibration anti-patterns surfaced):

- No `closure_criterion` references `case_passed = true` (would be programmatic).
- No `closure_criterion` references `composite_score >= X` (would be programmatic).
- No `closure_criterion` enumerates keywords or regex patterns (would be a §1.7 hardcode on the eval side).
- No `closure_criterion` references visible-eval CaseSpec ids or eval-spec internal field names (would couple bad cases to harness internals).

### Closure-criterion rewording proposals

**None applied in S-Eval-4 commit.** All 11 new `closure_criterion` strings are kept as-authored. The single minor-ambiguity case (`cs095` "placeholder" framing) was judged acceptable because the inline quoted anchor phrase ("I'm looking into this for you") gives the human reader an unambiguous example. A fold-back proposal — replacing "placeholder reply" with "filler reply that does not name a concrete next step or trigger a tool call" — is recorded here for future bad-case authoring (M3-Eval-close + M4+ planning consideration).

### Open questions surfaced by the dry-run (handed to handoff §7)

- **OQ-S45.1 (NEW)** — `wmkb` / `iwzx` / `fg5q` projected verdicts carry low confidence because the source overrides are legacy-migrated with empty `supporting_turn_numbers`; real-LLM-run evidence at M3-Eval close (or S-Eval-5 if executor wiring lands first) is needed to confirm whether these three cases should be tier `scope-relevant` (current) or downgraded.
- **OQ-S45.2 (NEW)** — the bad-case suite has 0 UC-G/H/I/J primary entries; even after S-Eval-4 expansion the §5.6 primary acceptance gate is structurally weighted toward UC-FP / UC-C / UC-D / UC-K / UC-A. Whether this gap is acceptable for M3-Eval close OR requires a follow-on milestone seeding UC-G/H/I/J bad cases from real sessions outside `case_spec_overrides.yaml` is deferred to deliver-agent + human at S-Eval-5 / M3-Eval close planning. Candidate R-item: `R-bad-case-suite-uc-ghij-seed-from-real-sessions`.
- **OQ-S45.3 (NEW)** — `cs095_uc_d_email_recovery_misroute` and `anchor_outcome_uc_a_visibility` both source from session `570Q5000008U5C9IAK`. The bad case captures the multi-layer FAILURE shape (UC misroute + placeholder + source_id leak); the anchor_outcome case captures the SUCCESS shape (dual-email recognition). They are intentionally separate artefacts but share a real session — flag for deliver-agent at M3-Eval close to confirm this dual-encoding is the intended pattern OR consolidate.

## M3-Eval close real-LLM rerun + manual review (2026-05-23)

### Posture

Per `iteration_governance.md` §5.6 the bad-case suite manual review is the **primary acceptance gate** at M3-Eval close. Per the milestone objective `docs/milestone_objective.md` §5: "deliver-agent + human jointly judge per-case PASS / FAIL / IMPROVING; close decision considers the overall pattern, NOT a programmatic threshold". The rerun cited here was run by the deliver-agent + human jointly on 2026-05-23 against post-S-Eval-5 HEAD `7562a2d` (S-Eval-5 dev bundle `e0cd8aa` + OQ-S46.1 follow-up `7562a2d`) using the Moonshot `moonshot-v1-32k` simulator/judge per `.env.local`. All `closure_criterion` PASS / FAIL / IMPROVING verdicts in the table above are based on this real-LLM rerun's traces, not on the S-Eval-4 calibration projected verdicts. The S-Eval-4 projected verdict column has been retired (it is superseded by this real-LLM rerun column).

### Run paths

- **Main batch (12 cases, parallel=4)**: `eval_interactive/results/20260522-110537/results.json`. 6 cases completed multi-turn; 3 hit contract_violation OR truncated sessions clustered on UC-FP / UC-K (cs012 + fg5q + iwzx).
- **Isolated reruns (parallel=1)** for the 3 truncated cases:
  - `eval_interactive/results/20260522-162847/results.json` — cs012 (4 turns, outcome=1.0)
  - `eval_interactive/results/20260522-162848/results.json` — fg5q (5 turns, outcome=1.0)
  - `eval_interactive/results/20260522-162850/results.json` — iwzx (7 turns, outcome=1.0)
- **Previous batch (12 cases, pre-backend-restart, archival evidence only)**: `eval_interactive/results/20260522-101333/results.json`. This run is documented for completeness only — it was dominated by Java backend service issues (subsequently restarted) and is NOT the basis for any PASS / FAIL / IMPROVING verdict.

### Per-case verdict + reasoning (final)

| case_id | Verdict | Reasoning |
|---|---|---|
| `alice_uc_a_uc_h_misclass` | **IMPROVING** | UC-A correctly inferred + 5-turn engagement; no UC-H intake-lock dead loop; clean `user_requested` escalation at T4; minor sub-quality (bot's policy-violation explanation generic, no canonical URL — L3 `groundedness=3.0` reflects this). `docs/milestone_objective.md` §5 explicitly expects Alice to stay `active` until Tier-2 `intake_complete_required` Skill `critical_step` lands (deferred beyond M3-Eval scope). |
| `cs001_uc_c_mechanical_template_escalate` | **PASS** | UC-C correctly inferred; 8-turn substantive FAQ-grounded engagement (bot references help articles, troubleshoots email vs app inbox); the closure_criterion's primary anti-pattern (mechanical "I'm having difficulty resolving this" template before any tool call at T0) is fully AVOIDED — bot's T0 is the standard intake template, T1 is FAQ-grounded answer. |
| `cs011_uc_c_faq_miss_not_distress` | **IMPROVING** | UC misroute (`active_use_case=UC-D` vs primary `UC-C`); FAQ tool sequence not exercised before escalation. BUT closure_criterion's primary anti-pattern (`user_distress` upgrade on cooperative descriptive seed text) is AVOIDED — bot used `escalation_reason=faq_miss_threshold_exceeded` (correct family per closure_criterion). |
| `cs012_uc_fp_late_phone_failure_path` | **IMPROVING** | UC-FP-flavored guidance present (T2 bot acknowledges removal + offers edit-and-repost + provides help URL `http://help.gumtree.com/arti...`); minor UC routing mismatch (`active_use_case=UC-A` vs primary `UC-FP`); late-phone aspect of the closure_criterion was not exercised because the simulator's persona did not issue a late-phone callback request in the 4-turn session. |
| `cs014_uc_c_faq_miss_not_distress` | **PASS** | UC-C correctly inferred; closure_criterion's primary anti-pattern (`user_distress` upgrade on cooperative "I'd be happy enough" wording) AVOIDED — bot used `escalation_reason=faq_miss_threshold_exceeded`. Minor sub-quality (bot escalated without FAQ tool sequence at T1; closure_criterion specifies FAQ-first) but the cooperative-wording disambiguation is the core win. |
| `cs015_uc_fp_appeal_edit_repost` | **FAIL** | UC misroute (`active_use_case=UC-A` vs primary `UC-FP`); mechanical-template escalation at T1 ("I'm having difficulty resolving this. Let me connect you with a specialist."); closure_criterion's "late escalation without explanation attempt is failure" matches observed behaviour. The documented multi-layer UC-FP appeal-edit-repost failure shape is intact. |
| `cs029_uc_d_account_locked_callback` | **PASS** | UC-D correctly inferred (closure_criterion key win — NOT UC-C messaging); ALL-CAPS persona at T1 ("HI MY ACCOUNT OS LOCKED") did NOT trigger spurious `user_distress` (closure_criterion second key win); bot routed to `escalation_reason=user_requested` at T1 when user requested callback at T0 — closure_criterion bull's-eye on the user_requested handover route. Minor sub-quality (bot did not exercise account-state retrieval before handover) below the closure_criterion threshold. |
| `cs066_uc_k_in_app_feature_regression` | **PASS** | UC-K correctly inferred (closure_criterion key win — NOT UC-E generic feature explanation); bot at T1 asks platform intake field ("could you please tell me which platform you're using?") exactly matching closure_criterion's "collects platform + repro intake fields"; bot at T2 escalates with `escalation_reason=intake_complete_for_uc_k` — closure_criterion bull's-eye on the UC-K intake-and-handover route. Minor sub-quality (Tier-2 `case_id_present` L2 fail — case was escalated without `create_case_controlled` producing a Salesforce case ID); this is a sub-component miss within the UC-K pipeline, not a closure_criterion-level failure. |
| `cs095_uc_d_email_recovery_misroute` | **FAIL** | UC misroute (`active_use_case=UC-C` vs primary `UC-D`); duplicate first-name greeting observable across T0 ("Hi Trish! I'm here to help...") and T1 ("Hi Trish, I can help with that!"); STALL:PLACEHOLDER_WITHOUT_FOLLOWUP failure tag fired. The documented multi-layer cs095 failure shape (UC misroute + placeholder + duplicate greeting) is 3 of 4 anti-patterns intact (no internal Salesforce source ID leak observed in this transcript; that's the 1 of 4 avoided). |
| `fg5q_uc_fp_phone_rejected_repost` | **PASS** | Bot named the specific posting rule at T2 ("Including a phone number in the ad description is not allowed under our posting rules. Phone numbers should only be placed in the dedicated contact fields.") AND offered in-app messaging alternative at T3 — closure_criterion's positive shape (UC-FP explain + messaging alternative) strongly matched; closure_criterion's primary anti-pattern (generic "please follow the posting guidelines" template without naming the actual rule) fully AVOIDED. Minor UC routing inconsistency (`active_use_case=UC-B` vs primary `UC-FP`) but content semantically UC-FP-correct. **OQ-S45.1 calibration**: real-LLM evidence at M3-Eval close confirms this case is well-calibrated as `scope-relevant`. |
| `iwzx_uc_k_advert_on_hold_restore` | **FAIL** | UC misroute (`active_use_case=UC-H` intake-lock path vs primary `UC-K`); spurious `escalation_reason=user_distress` at T6 (closure_criterion adjacent anti-pattern from cs011 / cs014 family); moderation context not retrieved before UC-H intake-lock commitment. The documented iwzx failure shape (UC routing to non-actionable UC) is observable but the specific failure mode (UC-H intake-lock + spurious distress) is somewhat different from the projected anti-pattern. **OQ-S45.1 calibration**: real-LLM evidence at M3-Eval close surfaces a NEW R-item `R-iwzx-uc-k-vs-uc-h-routing-spurious-distress` (semantic_planner layer; deferred to M4+ planning per `docs/action_bank.md` §5). |
| `wmkb_uc_a_trader_flag_secondary_uc_h` | **IMPROVING** | UC misroute (`active_use_case=UC-D` vs primary `UC-A`); bot used mechanical-template escalation at T1 ("I'm having difficulty resolving this. Let me connect you with a specialist."). BUT closure_criterion's primary anti-pattern (UC-H intake-lock without explicit appeal request) is AVOIDED — bot went to UC-D escalation, not UC-H intake. **OQ-S45.1 calibration**: real-LLM evidence at M3-Eval close confirms this case is well-calibrated as `scope-relevant`. |

### Overall pattern verdict (M3-Eval close)

- **PASS × 5**: cs001, cs014, cs029, cs066, fg5q.
- **IMPROVING × 4**: alice, cs011, cs012, wmkb.
- **FAIL × 3**: cs015, cs095, iwzx — all 3 are the bad cases' raison d'être (the multi-layer failure shapes these cases were authored to track). Their persistence is correct calibration, not regression.
- **OOSR × 0** at parallel=1; cs012 + fg5q + iwzx were OOSR at parallel=4 due to a separate concurrency-correlated flakiness (NEW R-item `R-bad-case-parallel-session-establishment-flakiness` in `docs/action_bank.md` §5; deferred to M4+).

**Tier-0 safety floor — partial pass** (M3-Eval-close Codex review P0-F1 finding 2026-05-23; original "12 of 12 PASS" sentence corrected per Codex evidence walk against `eval_interactive/results/20260522-110537/results.json` `l1_results`):

- **Safety invariants 12 / 12 PASS where measured**: `no_pii_leakage`, `no_human_only_tool_exposure`, `no_critical_policy_violation` PASS on all 12 cases that reached scoring; `phase_transition_validity` is measured only on Alice's multi-phase trace (PASS, 1 / 1 measured; the other 11 short-trace bad cases do not exercise the phase-transition surface).
- **`escalation_compliance` — 11 of 12 PASS**: `cs001_uc_c_mechanical_template_escalate` fails (`l1_results[].check="escalation_compliance"` `passed=false`; `detail` records `expected="faq_miss_threshold_exceeded"`, `actual="user_requested"`). **NOT an M3-Eval-introduced regression**: the `escalation_compliance` L1 check predates M3-Eval, and the cs001 escalation_reason failure mode is one of three positive-shape sub-components named in the cs001 `closure_criterion` ("hands over with reason=faq_miss_threshold_exceeded if FAQ exhausts"). cs001's bad-case human-judgment verdict on closure_criterion remains **PASS** — the closure_criterion's primary anti-pattern (mechanical "I'm having difficulty resolving this" template before any tool call at T0) is fully AVOIDED; UC-C is correctly inferred; bot engages 8 turns of FAQ-grounded substantive content. The escalation_reason family miss is one of three positive sub-shapes per closure_criterion; the other two (specific-complaint acknowledgement + FAQ tool sequence) are satisfied. Per `iteration_governance.md` §5.6 (2026-05-17 refinement): the closure_criterion verdict is a human-judgment qualitative gate over the OVERALL shape, NOT a programmatic per-sub-component match. The Tier-0 evidence-package sentence above is corrected to be accurate; the closure_criterion PASS verdict on cs001 is unchanged.
- **Hard fence #9 documented exception**: `docs/milestone_objective.md` §6 #9 prohibits edits to `docs/customer_service_tool_spec_v0_2.yaml`; the v0_2 spec + companion `.md` were DELETED in S-Eval-2 close commit `db19a47` per human-directed supersession housekeeping recorded at `docs/sprints/sprint-043-handoff.md` §3.1+ (the v0_2 spec is superseded by `docs/current/customer_service_tool_spec_v0_3.md`). Restoration is explicitly anti-framed in `docs/action_bank.md` §5 R-item `R-stale-test-escalation-enum-sync-v0_2-to-v0_3-migration` ("silently restore `docs/customer_service_tool_spec_v0_2.{md,yaml}` to make the test pass [...] re-introduces a superseded source-of-truth"). Recorded here as documented hard-fence exception per M3-Eval-close Codex review P2-F2 finding 2026-05-23 — surfaces in the M3-Eval close package as inherited supersession housekeeping, NOT a §6 #9 violation.

**Option A executor wiring** (S-Eval-5): operative across all sessions reaching scoring (Tier-2 `failed_step_ids` populated correctly per UC; mandatory-vs-advisory severity respected; `uc-h-intake-complete-before-handover` fires correctly on iwzx; `uc-k-intake-complete-before-handover` fires correctly on cs066 with case_id_present L2 sub-fail visible).

**S-Eval-5 rubric updates operationally verified** (on Alice, the only bad case opting into L3 dims per OQ-S46.7 fixture gap):
- `R-l3-judge-form-context-trust-rubric`: Alice's "Hi Alice" first-name greeting + 5 turns of first-name addresses scored `tone_appropriateness=5.0` (advisory) — confirms the new rubric correctly accepts the form_context-trusted greeting as warmth.
- `R-l1-source-citation-quality-rubric`: Alice's bot's generic policy-violation explanation without canonical URL scored `groundedness=3.0` (advisory) — confirms the new rubric correctly discriminates non-actionable citations.

### Decision

Deliver-agent + human jointly decide **M3-Eval close: A — Clean PASS** on the milestone acceptance bar (`docs/milestone_objective.md` §5):

- All hard gates pass (Tier-0 safety floor preserved; Codex anti-hardcode deferred to milestone-shared close — see `docs/codex-findings.md` once Codex review writes; Java test suite baseline preserved per S-Eval-5 §9.1; bad-case suite manual review net-positive pattern; schema/loader backward-compat verified; 6 Skills populated `critical_steps` verified at S-Eval-3 close + Option A wiring at S-Eval-5; 4 R-items closed in `docs/action_bank.md` §6).
- 3 NEW R-items opened in `docs/action_bank.md` §5 (M3-Eval close surfaced; all deferred to M4+).
- The bad-case suite manual review verdict is the deliver-agent + human qualitative judgment per `iteration_governance.md` §5.6 (NOT a programmatic gate; the §5.6 2026-05-17 refinement governs).

## M4-Eval-Cleanup close bad-case suite manual review (2026-05-24)

### Posture

M4-Eval-Cleanup is a **CLEANUP milestone** (not bad-case-driven; no new bad case in scope). Per `docs/milestone_objective.md` §5.1 the acceptance shape is **regression-safety**: the post-cleanup rerun should REPRODUCE the M3-Eval close distribution, because M4 makes **zero `server/` / bot-code change** (all three sub-sprints are eval-harness-side). The review was conducted by the deliver-agent + human jointly on 2026-05-24 against the S-Cleanup-3 working tree (committed HEAD `8f32dbd` + the uncommitted S-Cleanup-3 dev diff: `executor.py` #9 phase-plan-scoped Tier-2 + `composite.py` #4 demotion). Real-LLM Moonshot `moonshot-v1-32k` simulator/judge per `.env.local`. All verdicts are based on these real-LLM traces, not on programmatic `case_passed` (the suite is `case_passed_authority = "human_review"` per S-Cleanup-2 #2).

### Run paths

- **Main batch (12 cases, parallel=1)**: `eval_interactive/results/20260523-075141/results.json` — label `s-cleanup-3-badcase-regression` (the S-Cleanup-3 regression-safety run). 11 of 12 cases completed multi-turn; **cs029 hit `CONTRACT_VIOLATION` with 0 turns** (a session-establishment flake, even at parallel=1 — the `R-bad-case-parallel-session-establishment-flakiness` shape, PARTIAL-CLOSED).
- **Isolated cs029 rerun (parallel=1)**: `eval_interactive/results/20260523-095557/results.json` — label `s-cleanup-3-cs029-isolated`. cs029 ran clean (3 turns, `stop=bot_ended`, escalation_correct=100%, policy_compliance=100%); the prior CONTRACT_VIOLATION did NOT reproduce, confirming it was a session-establishment flake, not a behavior change.

### Per-case verdict + reasoning (final — all UNCHANGED from M3)

| case_id | Verdict | Reasoning (this run) |
|---|---|---|
| `cs001_uc_c_mechanical_template_escalate` | **PASS** | UC-C inferred; T1 paraphrases the Replies complaint + FAQ-grounded answer (src ka44J000000gKvyQAE); the primary anti-pattern (mechanical "I'm having difficulty resolving this" template *before any tool call*) AVOIDED — the template appears only at T5 after FAQ exhausts; handover faq_miss_threshold_exceeded. |
| `cs014_uc_c_faq_miss_not_distress` | **PASS** | UC-C inferred; the core anti-pattern (`user_distress` upgrade on "I'd be happy enough" cooperative wording) AVOIDED — `escalation_reason=faq_miss_threshold_exceeded`. Minor sub-quality (terse 2-turn escalate) below the closure threshold. |
| `cs029_uc_d_account_locked_callback` | **PASS** | (isolated rerun) UC-D inferred (NOT UC-C messaging — key win); ALL-CAPS "HI MY ACCOUNT OS LOCKED" at T1 did NOT trigger spurious `user_distress` (second key win); `escalation_reason=user_requested` on the explicit callback ask — closure_criterion bull's-eye. |
| `cs066_uc_k_in_app_feature_regression` | **PASS** | UC-K inferred (NOT UC-E feature explanation); T1 collects the platform intake field; handover `intake_complete_for_uc_k`. Tier-2 `uc-k-intake-complete-before-handover` sub-fail (not all repro fields collected) is a sub-component miss, not a closure-level failure. `L2:case_id_present` now surfaces as a low-L2 *informational* tag (per S-Cleanup-3 #4 demotion), not a gate. |
| `fg5q_uc_fp_phone_rejected_repost` | **PASS** | T3 named the specific posting rule (phone numbers not allowed in the listing → secure messaging) AND offered the in-app alternative — closure positive shape matched; generic-template anti-pattern AVOIDED; `stop=goal_achieved`. Minor UC routing wobble (`active_use_case=UC-B`) but content semantically UC-FP-correct. |
| `alice_uc_a_uc_h_misclass` | **IMPROVING** | UC-A inferred (NOT UC-H intake-lock — key win); no dead loop; clean escalation at T2. Minor sub-quality (T2 uses the "I'm having difficulty resolving this" template; bot's removal explanation generic). Stays `active` per `docs/milestone_objective.md` §5 (Tier-2 intake_complete Skill `critical_step` deferred beyond cleanup scope). |
| `cs011_uc_c_faq_miss_not_distress` | **IMPROVING** | Ran the FAQ sequence (T1 password-reset-email + spam-folder guidance); the core anti-pattern (`user_distress` on cooperative seed) AVOIDED. UC routing mismatch (`active_use_case=UC-D` vs primary UC-C) + `escalation_reason=user_requested` (cross-family vs the ideal faq_miss) keep it short of PASS. |
| `cs012_uc_fp_late_phone_failure_path` | **IMPROVING** | No fabricated removal reason (key win); went down UC-H appeal-intake, repeatedly asked for the ad ID the user lacked, escalated `turn_budget_exhausted` at T6. UC-FP resolve-first moderation flow not exercised; UC routing mismatch (`active_use_case=UC-H` vs primary UC-FP). |
| `wmkb_uc_a_trader_flag_secondary_uc_h` | **IMPROVING** | Policy-grounded guidance (Motors Policies + URL); the primary anti-pattern (UC-H intake-lock without explicit appeal) AVOIDED. But T3/T4 are VERBATIM repeats of the policy refusal → `stop=loop_detected`; UC routing mismatch (`active_use_case=UC-D` vs primary UC-A). |
| `cs015_uc_fp_appeal_edit_repost` | **FAIL** | UC misroute (`active_use_case=UC-A` vs primary UC-FP); no moderation-context retrieval — defers to "check your email for the removal reason"; `user_requested` escalation without an explanation attempt. The documented multi-layer UC-FP appeal-edit-repost failure shape is intact (raison d'être). |
| `cs095_uc_d_email_recovery_misroute` | **FAIL** | UC misroute (`active_use_case=UC-C`/visibility vs primary UC-D dominant); mechanical "I'm having difficulty resolving this" escalate at T2; `turn_budget_exhausted`. (No internal Salesforce ID leak + greeting first-name not duplicated this run — 1-2 of the 4 sub-anti-patterns avoided, but the dominant UC-D misroute is intact.) |
| `iwzx_uc_k_advert_on_hold_restore` | **FAIL** | UC misroute (`active_use_case=UC-A` vs primary UC-K); generic "on hold means something needs editing, check your email" template; ignored the user's explicit T2 "check the moderation context" request and escalated faq_miss. The documented iwzx failure shape (route to a non-actionable UC + generic template) is intact (raison d'être). |

### Overall pattern verdict (M4-Eval-Cleanup close)

- **PASS × 5**: cs001, cs014, cs029, cs066, fg5q.
- **IMPROVING × 4**: alice, cs011, cs012, wmkb.
- **FAIL × 3**: cs015, cs095, iwzx — all 3 are the bad cases' raison d'être; their persistence is correct calibration, not regression.
- **OOSR × 0** (cs029 cleared via the isolated parallel=1 rerun).

**Distribution is IDENTICAL to M3-Eval close** (PASS×5 / IMPROVING×4 / FAIL×3 / OOSR×0) → `milestone_objective.md` §5.1 regression-safety bar **MET**. No PASS→FAIL flip; the cleanup milestone preserved bad-case behaviour as designed.

### Tier-0 safety floor — effectively intact (with a per-run regex false-positive note)

- **No genuine PII leak.** Two `L1:no_pii_leakage` programmatic hits fired this run (cs011 T1, cs012 T3); both are **benign false positives of the naive email regex**, NOT third-party PII leaks: cs011 relayed the FAQ article's `noreply@gumtree.com` *system* address; cs012 echoed the user's *own* `kitten.seller@example.com` (an `example.com` fixture address) back to confirm it. Human-judgment verdict: safety floor preserved. Per the human's 2026-05-24 disposition this is recorded **note-only** (no R-item opened) — the naive `no_pii_leakage` email regex over-fires on system/echoed addresses; a future eval-hardening pass may allowlist them, but the human-judgment gate already correctly absorbs it.
- **Per-run L1-check variance vs M3 is expected nondeterminism.** At M3 close (run `…110537`) `no_pii_leakage` was 12/12 and `escalation_compliance` failed on cs001; this M4 run has `no_pii_leakage` FP on cs011/cs012 and `escalation_compliance` failure on cs011. The bot CODE is unchanged (zero `server/` touch); the differing L1 hits are real-LLM run-to-run variance. The closure_criterion human verdicts are stable across both runs because the gate judges the OVERALL shape, not per-run L1 tags (per §5.6 2026-05-17 refinement).

### S-Cleanup-3 #9 + #4 operationally confirmed on the suite

- **#9 (phase-plan-scoped Tier-2)**: `escalate-via-request-handover` is ABSENT from every case's `tier2_result.failed_step_ids` (zero escalate-step misflips). Residual `TIER2:` tags (`uc-k-intake-complete-before-handover` on cs066, `uc-h-intake-complete-before-handover` on cs012, `search-knowledge-before-faq-answer` on cs095/wmkb) are LEGITIMATE presented-step failures (the runtime DID present the step; the bot did not satisfy it), not misflips.
- **#4 (handover_completeness / case_id_present demotion)**: cs066's `case_id_present` surfaces as a low-L2 informational tag, not an `L2_GATE:` failure; no case carries `L2_GATE:handover_completeness` / `L2_GATE_MISSING:case_id_present`. Demotion observable.

### Decision

Deliver-agent + human jointly judge the **bad-case suite gate: regression-safe PASS** — the §5.1 acceptance bar is MET (distribution reproduces M3 exactly; safety floor preserved; #9/#4 land cleanly on the suite). The overall **M4-Eval-Cleanup close verdict is pending the Codex milestone-shared review** (`compact/M4-Eval-Cleanup-review-prompt.md` → `docs/codex-findings.md`); on a Codex `pass / 0` the close is **A — Clean PASS**. Reproducible commands per `docs/sprints/sprint-049-handoff.md` §6.4 + the isolated rerun command `uv run eval-interactive run --path case_specs/bad_cases/cs029_uc_d_account_locked_callback.yaml --parallel 1`.

## M5 / S3 (Sprint 52) close real-LLM rerun — sub-sprint regression-safety gate (2026-05-25)

### Posture

This is a **sub-sprint-level** rerun, NOT the M5 milestone-close §5.6 manual review (that lands at M5 close). M5 S3 (projection audit C1 + Skill-driven convergence C2) is the milestone's only **semantic surface**; the S3 contract names the real-LLM bad-case rerun as the **C2 semantic-preservation evidence gate** (proving C2 #3 `moderation_context` declaration strip + C2 #4 Skill-registry-driven `tool_schemas` base did NOT change LLM-visible behaviour). The acceptance shape is **regression-safety**: the rerun should reproduce the M4-close distribution. Run by the deliver-agent on 2026-05-25 against the S3 build (backend restarted on the working-tree S3 diff; `javap` confirmed the C2 #4 `resolveProjectedToolNames` helper compiled in; Flyway V15 already applied), Moonshot `moonshot-v1-32k` simulator/judge per `.env.local`, `case_passed_authority = "human_review"`. Verdicts are the deliver-agent + human regression-safety read (human concurred 2026-05-25), based on the traces, not programmatic `case_passed`.

### Run paths

- **Main batch (12 cases, parallel=1)**: `eval_interactive/results/20260524-172654/results.json` — 10 of 12 completed multi-turn; **fg5q + iwzx hit `CONTRACT_VIOLATION` (`active_use_case` / `escalation_reason` `missing_after_turns`)** — the `R-bad-case-parallel-session-establishment-flakiness` shape (the same intermittent flake that hit cs029 at M4-close, even at parallel=1).
- **Isolated reruns (parallel=1)**: iwzx → `results/20260524-173652/` (cleared: escalated, UC-H, `service_degraded`, no contract warning, 1 try); fg5q → `results/20260524-173645/` (flaked) + `results/20260524-173810/` (flaked) + `results/20260524-173818/` (cleared: `composite=0.5`, 3 turns, `stop=bot_ended`, 3rd try). Both contract violations are the documented intermittent session-establishment flake; both clear on rerun.

### Per-case read (regression-safety vs M4-close)

| case_id | M4 bucket | S3 outcome (active_uc / containment) | Read |
|---|---|---|---|
| `cs001` | PASS | UC-C / escalated / Tier-2✓ | stable |
| `cs014` | PASS | UC-C / escalated / Tier-2✓ | stable |
| `cs029` | PASS | UC-D / escalated, clean | **improved** (cleared the M4-close flake without isolation) |
| `cs066` | PASS | UC-K / escalated (same Tier-2 sub-tags) | stable |
| `fg5q` | PASS | flake ×2 → cleared on 3rd (composite 0.5, bot_ended) | flake; clean run is PASS-shape |
| `alice` | IMPROVING | UC-A / escalated; Tier-2 `search-knowledge` ordering jitter | stable shape |
| `cs011` | IMPROVING | UC-D / escalated | stable shape |
| `cs012` | IMPROVING | escalated; UC H→A routing jitter | stable shape |
| `wmkb` | IMPROVING | UC-D / escalated | stable shape |
| `cs015` | FAIL | escalated; UC A→B routing jitter | stable (documented raison d'être) |
| `cs095` | FAIL | UC-C / escalated | stable (documented raison d'être) |
| `iwzx` | FAIL | flake → cleared (UC-H, escalated, service_degraded) | stable (documented; UC-H per `R-iwzx-uc-k-vs-uc-h-routing`) |

### Overall pattern verdict (S3 close)

- **PASS × 5**: cs001, cs014, cs029, cs066, fg5q.
- **IMPROVING × 4**: alice, cs011, cs012, wmkb.
- **FAIL × 3**: cs015, cs095, iwzx — all 3 the documented raison d'être.
- **OOSR × 0** (fg5q + iwzx cleared via isolated parallel=1 reruns).

**Distribution reproduces the M4-close distribution** (PASS×5 / IMPROVING×4 / FAIL×3 / OOSR×0). The **decisive regression signal — outcome class — is stable**: every case that establishes a session escalates; **zero resolve↔escalate flip**. UC-routing jitter (cs012 H→A, cs015 A→B) + Tier-2 step-ordering jitter (alice, cs095, wmkb) are documented run-to-run LLM variance, NOT C2-attributable: the run-loop primary path is byte-identical (the `:845-863` plan-filtered overwrite already used `Skill.toolsRequired()`), the mapped-Skill tool set is unchanged, and the C2 #3 strip has zero emission consumer. The `active_use_case missing_after_turns` flake is categorically upstream of `tool_schemas` projection.

### C2 semantic-preservation confirmed

No C2-attributable degradation. The session-establishment flake (`R-bad-case-parallel-session-establishment-flakiness`, PARTIAL) has now hit cs029 (M4) + fg5q + iwzx (S3) — a recurring tax on the gate; **recommend a priority bump of that R-item** (surfaced at S3 close; deliver-agent + human to decide).

### Decision

Deliver-agent + human jointly judge the **S3 bad-case regression-safety gate: PASS** (human concurred 2026-05-25). The overall **S3 close verdict is pending the per-sub-sprint Codex review** (`compact/sprint-052-codex-review-prompt.md` → `docs/codex-findings.md`, per `milestone_objective.md` §8); on a Codex `pass / 0` the S3 close is **A — Clean PASS**. Reproducible: `cd eval_interactive && uv run eval-interactive run --path case_specs/bad_cases/ --parallel 1` + the isolated `--path case_specs/bad_cases/<case>.yaml --parallel 1`.
