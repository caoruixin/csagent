# Current Handoff

Date: 2026-05-09
Branch: `design-v1-without-human-review`

## 1. Current phase

Current phase:
Sprint 13 — Runtime Freeze, Risk Policy, and Eval Guardrails
(in flight; awaiting Codex review).

Latest closed sprint:
Sprint 12 — Runtime Alignment Hardening and Validation
(archived under `docs/sprints/sprint-012-*`).

## 2. Sprint 13 goal

Sprint 13 freezes the runtime main flow that converged across
Sprints 10 / 11 / 11.1 / 12 and shifts the next iteration layer to
**risk policy**, **prompt behaviour**, and **eval guardrails**.

The product principle: **risk signals are not always escalation
triggers**. The agent should be able to continue safely under low /
medium risk when it does not promise refunds, decide liability,
request sensitive credentials, or perform restricted actions.

Sprint 13 explicitly does NOT introduce a new broad runtime feature,
modify the runtime main state machine, edit the pre-plan reroute
architecture, edit the progressive resolve architecture, open a full
Issue Ledger / `issues[]` / per-issue budgets / all-UC task taxonomy /
handover payload rewrite, change the FAQ corpus, calibrate judges,
churn CaseSpecs, expand anchor / exploration / promotion hard gates,
add a new `escalation_reason` enum value, or run release candidate
hardening.

Scope is exactly the three Sprint 13 actions O0 / O1 / O2.

## 3. Sprint 13 implementation

### O0 — Runtime freeze decision + risk taxonomy doc

`docs/runtime_freeze_and_risk_policy.md` (new) — the canonical Sprint 13
deliverable. Contents:

- §1 Runtime freeze decision:
  - what is frozen: pre-plan reroute flow, same-UC progressive resolve
    flow, ResolveDisposition terminal-evidence guard, record_outcome
    guard, observability + validation layer.
  - what can still be tuned: prompt wording, reroute policy
    thresholds (eval-side), escalation policy wording, eval cases,
    risk taxonomy.
  - what requires a future runtime sprint: full Issue Ledger,
    per-issue budgets, all-UC task taxonomy, handover payload
    redesign, new escalation reason enum.
- §2 Frozen runtime contract (seven rules) recapping the
  Sprint-10/11/12 invariants Sprint 13 must preserve.
- §3 Risk taxonomy:
  - Level 1 — observe only.
  - Level 2 — constrained continue.
  - Level 3 — immediate escalation, split into 3a (explicit user
    request) and 3b (policy-driven: GDPR, scam/fraud, appeal /
    restoration, real payment dispute, sensitive credentials,
    distress / critical safety).
  - Full taxonomy table with signal example, routing, escalation
    yes/no, and bot behaviour for each level.
- §4 Term distinctions: `risk_flag` (informational, single turn)
  vs `escalation_trigger` (per-turn boolean) vs `handover_reason`
  (canonical 23-value enum, persisted on session) vs
  `intake_reason` (intake UC token, becomes
  `intake_complete_for_uc_*` at intake completion).
- §5 Five canonical examples walked end-to-end through the runtime
  decision path AND the desired bot behaviour:
  - "I paid for Top Ad but it is not showing." → Level 1.
  - "I want my money back because my ad is not visible." → Level 2.
  - "Delete my account." → Level 3b GDPR.
  - "I was scammed." → Level 3b trust & safety.
  - "I want a human." → Level 3a explicit user request.
- §6 O1 prompt / policy tuning decision (see below).
- §7 Cross-reference to the Sprint 13 §O2 deterministic test suite.
- §8 Acceptance criteria.
- §9 Out-of-scope list.
- §10 References.

### O1 — Risk-aware prompt / policy tuning

**Decision: docs-only deferral.** The system prompt
(`server/src/main/resources/prompts/system_prompt.txt`) is **not
edited** in Sprint 13.

Rationale (recorded in `docs/runtime_freeze_and_risk_policy.md` §6.1):

1. The existing prompt already encodes the safety-side rules:
   "Never claim to be human" / "Never promise actions you cannot
   take (refunds, account changes, ad removal)" / "Never fabricate
   facts; ground answers in retrieved knowledge or context" / "If
   you cannot help, request a human handover via the appropriate
   tool", plus the Sprint 6 §G1 ACTIVE-UC TIEBREAKER block and the
   GENUINE TIER-2 ESCAPE HATCH block.
2. The Sprint 13 risk policy is testable AT THE RUNTIME LAYER
   without a prompt edit (`Sprint13RiskPolicyGuardrailsTest` covers
   it deterministically).
3. A prompt edit cascades to live smoke runs, judge calibration,
   and golden snapshots. Sprint 13 explicitly defers smoke,
   anchor, and promotion gate work.
4. No live regression case requires a prompt change today.

`docs/runtime_freeze_and_risk_policy.md` §6.2 carries the **exact,
narrow prompt change proposal** (the new "Risk signal handling"
block) plus the five focused golden prompt tests that would land
with it. The Eval Governance sprint or a real-traffic incident is
the trigger to land that proposal; Sprint 13 ships the proposal as
documentation only.

### O2 — Eval guardrails for constrained continue vs immediate escalation

`server/src/test/java/com/gumtree/csagent/service/runtime/Sprint13RiskPolicyGuardrailsTest.java`
(new) — 12 deterministic tests pinning each Sprint 13 risk-policy
row:

| # | Scenario | Test |
|---|---|---|
| 1 | Level 1 — paid Top Ad not showing → stays UC-A, no escalation, no UC-I | `observeOnly_paidTopAdNotShowing_staysUcA_noEscalation_noUcI` |
| 2 | Level 1 — classifier-side negative guard fires + predicted UC stays UC-A | `observeOnly_paidTopAdNotShowing_classifierSurfacesNegativeGuard` |
| 3 | Level 2 — money back does not stamp escalation_reason | `constrainedContinue_moneyBack_doesNotStampEscalationReason` |
| 4 | Level 2 — classifier does not auto-escalate from "money back" | `constrainedContinue_moneyBack_classifierDoesNotEscalateImmediately` |
| 5 | Level 3a — resolver detects every Sprint 13 §3.3a explicit-human shape | `explicitHumanRequest_userRequested_resolverDetectsAllShapes` |
| 6 | Level 3a — classifier surfaces HUMAN_REQUEST relation | `explicitHumanRequest_classifierSurfacesHumanRequestRelation` |
| 7 | Level 3b — "I was scammed" risk-shifts to UC-J intake (not generic FAQ) | `scammed_routesToUcJ_intakePath_notFaq` |
| 8 | Level 3b — "Delete my account" risk-shifts to UC-G GDPR intake | `gdprDeleteMyAccount_routesToUcG_intakePath_notFaq` |
| 9 | Level 3b — alternate GDPR phrasings ("delete my data under GDPR") also route to UC-G | `gdprDeleteMyData_alsoRoutesToUcG` |
| 10 | Negative guard — generic "How do I find my ad?" stays UC-A, no over-escalation | `negativeGuard_genericAdVisibility_staysUcA_noOverEscalation` |
| 11 | Negative guard — UC-A same-UC follow-up "How long is it active for?" stays UC-A | `negativeGuard_followupDuration_staysUcA_noOverEscalation` |
| 12 | Cross-cutting — risk_flag does NOT imply escalation_trigger across L1 + L2 | `riskFlag_doesNotImplyEscalationTrigger_acrossLevel1AndLevel2` |

Each test runs the real `RuntimeIntentClassifier`, `RerouteDecider`,
`EscalationReasonResolver`, and (where applicable) the
`ControlKernel.applyRerouteDecision` path. No live LLM, no FAQ
service, no agent run loop. The fixture shape mirrors
`Sprint10RerouteDecisionTest` /
`Sprint11ProgressiveResolveTest` /
`Sprint12RuntimeAlignmentValidationTest`.

The suite asserts the CURRENT runtime contract. It does NOT modify
the kernel, the classifier, the decider, the resolver, the
disposition evaluator, or the system prompt; if a future change
regresses the risk taxonomy without an explicit migration, the
suite will fail loudly.

## 4. Files changed (Sprint 13)

Production code:
- (none) — Sprint 13 is a docs + tests sprint. No file under
  `server/src/main/` was edited.

Tests (new):
- `server/src/test/java/com/gumtree/csagent/service/runtime/Sprint13RiskPolicyGuardrailsTest.java`
  (12 deterministic tests, all green).

Docs:
- `docs/runtime_freeze_and_risk_policy.md` (new, normative).
- `docs/10-handoff.md` (this file; Sprint 12 closure already
  mirrored to `docs/sprints/sprint-012-handoff.md`).
- `docs/action_bank.md` (Sprint 13 row added; Sprint 12 entry
  moved to closed-action index).
- `docs/sprints/sprint-013-runtime-freeze-and-risk-policy-objective.md`
  (new — mirror of `docs/sprint_objective.md` for archival).
- `docs/sprint_objective.md` retained — already contains the
  Sprint 13 objective.

No FAQ corpus, CaseSpec, judge, broad routing taxonomy, handover
payload, Salesforce contract, prompt rewrite, or Eval Governance
file was touched.

No production runtime file (kernel / classifier / decider /
resolver / phase evaluator / agent run loop / projection builder /
session model / drift detector / disposition evaluator) was
touched.

The system prompt (`server/src/main/resources/prompts/system_prompt.txt`)
was not touched — see §3 / §6 of `docs/runtime_freeze_and_risk_policy.md`
for the deferred-prompt-edit decision and the exact proposal.

## 5. Tests run

- `mvn -pl server test -Dtest='Sprint13RiskPolicyGuardrailsTest'`
  → **12 / 0 / 0 / 0** (Sprint 13 §O2 risk-policy guardrails).
- `mvn -pl server test -Dtest='Sprint10*Test,Sprint11*Test,
  Sprint12*Test,Sprint13*Test,PhaseEvaluatorPlanTest,Cs014*,
  Cs066*,Cs095*,Cs002*,Cs029*,Cs176*,Cs001*,EscalationReason*Test,
  Sprint6*,Sprint7*Test,Sprint8*Test,Sprint9*Test'`
  → **269 / 0 / 0 / 0** (Sprint 6 / 7 / 7.1 / 8 / 8.1 / 8.2 / 9 /
  9.1 / 10 / 11 / 11.1 / 12 / 13 + Cs014 / Cs066 / Cs095 / Cs002 /
  Cs029 / Cs176 / Cs001 + Escalation reason).
- `mvn -pl server test`
  → **821 / 0 / 0 / 0** (was 809 pre-Sprint-13; +12 new Sprint-13
  §O2 regressions).
- `python -m pytest -p no:capture eval_interactive/tests/`
  → **294 / 0** (full Python eval test suite, including
  `test_agent_client_session_create_timeout.py` for the Sprint 6 §G0
  ReadTimeout no-retry contract, plus all `regression/`,
  `scoring/`, `trace/` packages).
- Smoke runs were NOT executed for Sprint 13. The change is a
  docs-only + targeted-test sprint that does NOT touch FAQ corpus,
  judges, CaseSpec, prompts, runtime production code, or LLM
  credentials. Live smoke remains optional and is recommended
  only when Codex explicitly requests runtime evidence; the focused
  JUnit + Python regression suite is the canonical Sprint 13
  evidence.

## 6. Risk-policy before / after

**Before Sprint 13.** The runtime-side risk taxonomy was implicit:
- Sprint 6 §G1 ACTIVE-UC TIEBREAKER pinned explicit-human → user_requested.
- Sprint 8 §K0 cs259 pinned active_use_case at the ESCALATE branch.
- Sprint 10 §L0 / §L1 added pre-plan reroute including
  `PAYMENT_AMBIGUITY_AD_VISIBILITY_PATTERN` (Top-Ad negative guard)
  and `UC_J_RISK_SHIFT_PATTERN` (scam → UC-J).
- Sprint 11 / 11.1 added progressive same-UC resolve + terminal-evidence
  guard.
- Sprint 12 §N0 added drift / task / phase observability so a reviewer
  can audit any one turn.
- The product distinction "risk signal vs escalation trigger" was
  encoded across the resolver / classifier / decider / system prompt
  but never centralised in a single canonical document or
  consolidated test suite.

**After Sprint 13.**
- `docs/runtime_freeze_and_risk_policy.md` is the single canonical
  source for: (a) which surfaces are frozen, (b) which can be tuned,
  (c) which need a future runtime sprint, (d) the Level 1 / 2 / 3
  risk taxonomy, (e) the term distinctions, (f) the five canonical
  examples, (g) the deferred prompt change proposal.
- `Sprint13RiskPolicyGuardrailsTest` deterministically pins each
  taxonomy row at the runtime layer, with no live LLM dependence.
- Sprint 13 introduced no production runtime change. All Sprint 10 /
  11 / 11.1 / 12 hard invariants remain green (see §7 below).

## 7. Regression guard outcomes

All Sprint 13 active guards pass:

- `L1:escalation_reason_consistency` = **0** (never re-introduced).
- `CONTRACT_VIOLATION:active_use_case` = **0**.
- Sprint 6 §G0 no ReadTimeout retry
  (`test_agent_client_session_create_timeout.py` 8 / 0).
- Sprint 6 §G2 FAQ-grounded-resolve guard
  (`AgentRunLoopS1FaqGroundedResolveGuardTest` green).
- Sprint 7 / 7.1 candidate_use_cases + intake-state persistence
  (`Sprint7CandidateUseCasesProjectionTest`, `Sprint7IntakeStateTest`,
  `Sprint71PartialIntakePersistenceTest` green).
- Sprint 8 §K0 cs259 active-use-case contract hardening
  (`Sprint8Cs259ActiveUseCaseHardeningTest`,
  `Sprint8Cs259EscalateBranchIntegrationTest` green).
- Sprint 8.1 §M3 DISCOVER → RESOLVE phase boundary green.
- Sprint 8.2 §M0a / §M0b green.
- Sprint 9 / 9.1 trace observability + record-outcome honesty
  (`Sprint9TerminalToolHonestyTest`,
  `Sprint9TraceObservabilityFidelityIntegrationTest`,
  `ToolCallTraceSanitizerTest` green).
- Sprint 10 reroute MVP
  (`Sprint10RuntimeIntentClassifierTest` 13 / 0,
  `Sprint10RerouteDecisionTest` 9 / 0).
- Sprint 11 progressive resolve MVP + 11.1 terminal-evidence closure
  (`Sprint11ProgressiveResolveTest` 14 / 0).
- Sprint 12 §N0 / §N1 / §N2 observability + validation
  (`Sprint12RuntimeAlignmentValidationTest` 13 / 0).
- cs014 remains UC-C (`Cs014RouteAndLoopHandoverIntegrationTest`,
  `Cs014RouteAndDistressRegressionTest`).
- cs066 remains UC-K.
- cs095 remains UC-A / not UC-K / not UC-FP.
- cs002 remains UC-C + `user_distress`.
- cs029 remains UC-D + `user_requested`.
- cs176 explicit-human-help → `user_requested`
  (`Cs176ExplicitHumanHelpHandoverIntegrationTest`).
- `RuntimeIntentClassifier` remains runtime-internal (NOT registered
  with the agent-visible tool surface; never exposed via
  `classify_use_case`).

New Sprint 13 §O2 guards:

- `Sprint13RiskPolicyGuardrailsTest.observeOnly_paidTopAdNotShowing_*` —
  Level 1 paid-Top-Ad does not blindly route to UC-I.
- `Sprint13RiskPolicyGuardrailsTest.constrainedContinue_moneyBack_*` —
  Level 2 "money back" does not auto-stamp `escalation_reason`.
- `Sprint13RiskPolicyGuardrailsTest.explicitHumanRequest_*` —
  Level 3a explicit-human shapes still produce HUMAN_REQUEST and
  `user_requested` precedence.
- `Sprint13RiskPolicyGuardrailsTest.scammed_routesToUcJ_intakePath_notFaq` —
  Level 3b "I was scammed" risk-shifts to UC-J intake.
- `Sprint13RiskPolicyGuardrailsTest.gdprDeleteMyAccount_*` /
  `gdprDeleteMyData_alsoRoutesToUcG` — Level 3b account deletion /
  GDPR risk-shifts to UC-G intake.
- `Sprint13RiskPolicyGuardrailsTest.negativeGuard_*` — generic ad
  visibility stays normal flow with no over-escalation.
- `Sprint13RiskPolicyGuardrailsTest.riskFlag_doesNotImplyEscalationTrigger_*` —
  L1 + L2 risk_flag does NOT imply an escalation_trigger.

## 8. Remaining P0 / P1 blockers

**No new P0 / P1 blockers opened by Sprint 13.** The change is
docs + targeted tests only; no production runtime file was touched
and no new schema / contract / tool surface was introduced.

Residuals carried forward from Sprint 12 §7 (unchanged):

- `R-cs015-description-keyword`, `R-cs176-UC-I-drift`,
  `R-S3-no-prior-search-guard`, `R-S5-Tier2-runtime-guard`,
  `R-stall-detector-calibration`, `R-L3-relevance-tone`,
  `R-FAQ-corpus-answerability`, `R-advert-link-product-decision`,
  `R-rerank-fallback-diagnostics`, `R-task-type-token-naming`,
  `R-record-outcome-loop`, `R-clean-baseline-promote`,
  `R-full-issue-ledger`, `R-skill-runtime-framework`.

New Sprint 13 deferral (documentation-only):

- **R-prompt-risk-signal-handling** — the narrow "Risk signal
  handling" block proposed in
  `docs/runtime_freeze_and_risk_policy.md` §6.2 is **deferred**
  pending an Eval Governance sprint or a P0 / P1 real-traffic case.
  Five focused golden prompt tests are pre-specified as the
  acceptance criteria for that future edit.

The current canonical eval baseline remains the post-Sprint-8 r1 / r2
runs documented in `docs/current_eval_baseline.md`:

- `eval_interactive/results/20260505-234448/results.json` (8/14,
  mean composite 0.4784, `sprint8-r1`).
- `eval_interactive/results/20260505-235231/results.json` (9/14,
  mean composite 0.5255, `sprint8-r2`).

Sprint 13 explicitly does NOT promote a new canonical eval baseline.

## 9. Next-phase recommendation

Recommended next phase:

**Eval Governance docs sprint** (or equivalent governance-only
work) — same recommendation Sprint 12 §8 made.

Justification:

- Sprint 13 closed the risk-policy / freeze-doc / eval-guardrails
  workstream that Sprint 12 §N2 carried forward. The runtime layer
  is fully frozen, with `docs/runtime_freeze_and_risk_policy.md` as
  the single source of truth.
- The largest residual category remains `judge_volatility` /
  `faq_corpus_gap` / `product_policy_gap`, all of which belong to
  Eval Governance, not runtime.
- The deferred prompt edit (R-prompt-risk-signal-handling) is the
  natural first item for the Eval Governance sprint to consider, IF
  a real-traffic case demonstrates the prompt is making a refund
  / liability / appeal promise OR requesting sensitive credentials.
- A clean live smoke run under uncontested Kimi credentials should
  be captured during Eval Governance so a Sprint-12-or-13-era
  canonical baseline can replace the post-Sprint-8 r1 / r2 runs in
  `docs/current_eval_baseline.md` when it is clean.

Alternative phases ranked:

1. **Eval Governance** — primary recommendation (above).
2. **Re-run validation after infra cleanup** — viable if Kimi /
   smoke credentials are blocking the baseline promote; runs
   alongside Eval Governance work.
3. **Release Candidate Hardening** — premature; depends on a
   clean canonical baseline that has not yet been promoted.
4. **Narrow Runtime Follow-up** — only if a real-traffic case
   surfaces a new P0 / P1 runtime blocker (none today).

Do not start another runtime sprint unless triage finds a new
P0 / P1 runtime blocker. Sprint 13 explicitly does NOT promote
a new canonical eval baseline.

## 10. Was Sprint 13 objective met?

Yes:

- O0 runtime freeze decision + risk taxonomy doc landed at
  `docs/runtime_freeze_and_risk_policy.md`. Includes runtime freeze
  decision, frozen runtime contract, three-level risk taxonomy with
  examples and bot-behaviour guardrails, the four-term distinction
  table (`risk_flag` / `escalation_trigger` / `handover_reason` /
  `intake_reason`), the five canonical examples walked through the
  runtime decision path, the prompt change proposal + deferral, and
  the Sprint 13 §O2 cross-reference.
- O1 risk-aware prompt / policy tuning DECISION recorded as
  docs-only deferral with the exact narrow prompt change proposal
  pre-specified (no system_prompt.txt edit landed). Five focused
  golden prompt tests pre-specified as the acceptance criteria for
  the future edit.
- O2 eval guardrails landed at
  `Sprint13RiskPolicyGuardrailsTest.java` (12 deterministic Java
  tests covering the six required cases plus a cross-cutting
  risk_flag-vs-escalation_trigger separation guard).
- All Sprint 6 / 7 / 8 / 9 / 10 / 11 / 11.1 / 12 hard invariants
  remain green; full server suite 821 / 0; full Python eval 294 / 0;
  `L1:escalation_reason_consistency = 0`;
  `CONTRACT_VIOLATION:active_use_case = 0`.

Out-of-scope items (runtime main-flow changes; DriftDetector /
RuntimeIntentClassifier architecture changes; full Issue Ledger;
`issues[]`; per-issue budgets; all-UC task taxonomy; broad routing
taxonomy rewrite; broad Java guard; new escalation reason enum; S5
Tier-2 runtime guard; FAQ corpus changes; product policy backend
changes; judge calibration; CaseSpec churn; CaseSpec override
changes; release candidate hardening execution; live smoke baseline
promotion) were NOT touched.

## 11. Current-doc maintenance rule

`docs/10-handoff.md`, `docs/codex-findings.md`,
`docs/sprint_objective.md`, and `docs/action_bank.md` are
overwrite-current-state files. Before replacing one of them:

1. archive the previous version under `docs/sprints/` if it
   belongs to a sprint closure, or
   `docs/archive/current-docs/` if it is an ad hoc transition;
2. then overwrite the working file;
3. keep only actionable current state in the working file.

Historical detail belongs in `docs/sprints/`,
`docs/archive/current-docs/`, `eval_interactive/results/`, and
`qa-reports/`.

Sprint 11 / 11.1 archives are at `docs/sprints/sprint-011-*`;
Sprint 12 archives are at `docs/sprints/sprint-012-*`;
Sprint 13 will archive to `docs/sprints/sprint-013-*` on closure.

The Sprint 13 objective is mirrored at
`docs/sprints/sprint-013-runtime-freeze-and-risk-policy-objective.md`
for historical reference.

## 12. Do not reopen

- broad full review
- broad routing rewrite
- judge calibration implementation
- CaseSpec churn
- anchor / exploration / promotion hard-gate expansion
- smoke 14/14 optimization
- S3 no-prior-search guard unless explicitly selected
- S5 Tier-2 runtime guard unless explicitly selected
- broad TraceViewer redesign
- llm_call_log dashboard / per-tool latency dashboard
- search threshold tuning / answer_miss / faq_miss semantic redesign
- tool-deadline guard / bypass-DISCOVER redesign
- advert-link generator / direct listing URL tool
- full Issue Ledger / `issues[]` / per-issue budgets / all-UC task
  taxonomy / full skill runtime framework / handover payload rewrite
- new escalation reason enum value
- runtime sprint unless a new P0 / P1 runtime blocker is found
- broad system prompt rewrite — narrow risk-signal-handling proposal
  in `docs/runtime_freeze_and_risk_policy.md` §6.2 only on Eval
  Governance trigger
