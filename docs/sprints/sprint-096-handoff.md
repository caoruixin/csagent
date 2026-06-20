---
title: "Sprint 096 / S-Auto-44 (M-Auto-9 WP1) — handoff: D-new-escalation-reason-enum migration (agent_unable_to_resolve)"
doc_tier: sprint-archive
status: current
implementation_status: implemented
source_of_truth: this file (dev handoff); runtime + eval code cited inline
last_reviewed: 2026-06-20
review_cadence: per sprint
supersedes: []
superseded_by: null
notes: >
  COMPLETE. The migration adds exactly one new canonical escalation_reason,
  `agent_unable_to_resolve` (human-blessed name), for the bot-initiated,
  in-scope, exhausted-resolution, unresolved handover — wired atomically
  across all producer/consumer surfaces in 4 separable commits (ad2191b5..
  f800618c) + handoff (87bc331e). All gates met: enum-sync green at 24; §6
  precedence proven (strictly lowest priority 50; every reason wins both
  orders); backward-compat clean (new value absent from all historical
  artifacts; no past evidence rewritten); zero-LLM scoring replay no
  gated-outcome change (family map additive + observation-only); Java
  1422/1/0/2 (+28 tests, sole failure inherited OQ-S41.5); eval_interactive
  620p (failed/error set byte-identical to baseline); autoloop 412p;
  scoring-SHA recorded, no re-bless (§5.7 N/A); §7.6 bounded real-LLM PASS —
  the model uses the new reason for the §2-class (uc_c_messaging 3/5,
  uc_d_login 1/5, all transcript-verified honest) with ZERO bleed onto the
  genuine-request control / GDPR / payment / safety guards; §4.1 Codex
  `approve` / `pass` (blocking_count 0). Env (Postgres/Redis/backend) was
  brought up via the project Makefile path and the backend rebuilt from
  Sprint 096 final; served projection verified at 24 values. Delivers WP1
  reason-honesty intent only; does NOT solve the M-Auto-9 PRIMARY closure
  question; WP0/WP2 remain HELD.
---

# Sprint 096 / S-Auto-44 (M-Auto-9 WP1) — handoff

One-line: add **exactly one** new canonical `escalation_reason`,
**`agent_unable_to_resolve`**, for the narrow class of bot-initiated, in-scope,
exhausted-resolution, unresolved handover (the honest fix the Sprint 095 WP1
BLOCK identified), wired atomically across every authoritative producer +
consumer so the bot stops mislabeling these handovers `user_requested`.

**Terminal status: COMPLETE.** All COMPLETE gates met: §3 atomic migration
(enum-sync green at 24); §5 name human-blessed; §6 precedence proven; §7.1-§7.7
gates all met (incl. the **§7.6 bounded real-LLM PASS — no over-use**); and the
**§4.1 per-sub-sprint Codex review returned `approve` / `pass`** (blocking_count
0; verdict verbatim in `docs/codex-findings.md`). Delivers WP1's reason-honesty
intent only; does **not** solve the M-Auto-9 PRIMARY closure question; WP0/WP2
remain HELD.

HEAD at sprint start: clean tree on `auto-loop-branch` at `567f018c`, baseline
Java `1394 / 1 / 0 / 2` (sole failure = inherited, provably-uncoupled
`SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly`,
OQ-S41.5).

## §0 Status table

| Step | State | Evidence |
|---|---|---|
| §5 name review + bless | **DONE** | `agent_unable_to_resolve` — human-blessed (AskUserQuestion, 2026-06-20); rationale in §1. |
| §3 atomic migration (11 surfaces) | **DONE** | 4 separable commits `ad2191b5`→`f800618c`; diff map in §2. |
| §6 precedence proof | **DONE** | resolver priority 50 (strictly lowest); 23-pairing `@ParameterizedTest`, both orders; §3 here. |
| §7.1 schema-consistency | **DONE** | `test_escalation_enum_sync.py` green at 24 (3-source walk); schema/runtime/resolver include the value. |
| §7.2 characterization | **DONE** | Java resolver + integration flow-through; Python contract-accept + trigger-match. §4 here. |
| §7.3 backward-compat replay | **DONE** | new value absent from 12 historical artifacts; family-map purely additive; no past evidence rewritten. §5 here. |
| §7.4 zero-LLM scoring replay | **DONE** | observation-only family-match; zero family drift on all 23 historical values; no gated-outcome change. §6 here. |
| §7.5 Java + Python suites | **DONE** | Java 1422/1/0/2 (+28); eval_interactive 620p/6f/5e (set byte-identical to baseline); autoloop 412p. §7 here. |
| §7.6 bounded real-LLM | **DONE — PASS (no over-use)** | env brought up; 5×10 live run; §2-class uses the new reason, controls/guards clean (0 bleed). §8 here. |
| §7.7 scoring-SHA + baseline-compat | **DONE** | HEAD `f800618c`; scoring blob `4ac4a698`; no re-bless (§5.7 N/A). §9 here. |
| §4.1 per-sub-sprint Codex | **DONE — `approve` / pass** | read-only `codex exec` (gpt-5.5, high) over `ad2191b5^..HEAD`; verdict verbatim in `docs/codex-findings.md`. |

## §1 Name decision + rationale (§5)

**Chosen: `agent_unable_to_resolve`** (human-blessed 2026-06-20).

Reviewed against the 23 existing snake_case values (`turn_budget_exhausted`,
`appeal_requires_human`, `account_compliance`, …) and every §3 consumer. It
names the **bot-initiated, attempted-but-unable-to-resolve** semantic
precisely (`agent` subject + `unable_to_resolve` state):

- distinct from the **budget family** — deliberately avoids the `_exhausted`
  suffix that is the budget-family signature (`turn_budget_exhausted`,
  `clarification_budget_exhausted`), so it does not read as a control-plane
  terminal-close;
- distinct from `out_of_scope` — the issue is *in scope*; this is about the
  agent's capability on a covered issue;
- distinct from `service_degraded` — no system-degradation claim; it is a
  per-case resolution limit;
- reads as a **semantic, LLM-owned** reason (§1.3), not a runtime stamp;
- not a generic "other"/"misc"/"fallback".

"agent" = the AI customer-service agent (the repo constitution calls the system
"an LLM-first customer-service agent"); the human side is consistently "human"
/ "human agent" / "human review", so there is no actor ambiguity.

Rejected alternatives: `resolution_exhausted` (the `_exhausted` suffix collides
with the budget family); `unresolved_needs_human` (honest but less precise
about the "agent attempted/exhausted" aspect).

## §2 Atomic migration — diff map (§3)

Single logical migration, 4 separable commits staged explicitly by file
(no `git add -A`); tree green (suites + enum-sync) at every boundary.

**Commit (a) `ad2191b5` — enum add across 3 sources + resolver/priority + sync test:**

| # | Surface | Path | Change |
|---|---|---|---|
| 1 | Canonical doc enum | `docs/current/customer_service_tool_spec_v0_3.md` | `(23 values)`→`(24)`; add value to the bullet list + a semantic-definition bullet (§2 class; explicitly NOT a generic fallback). |
| 2 | Runtime canonical set | `PhaseEvaluator.CANONICAL_ESCALATION_REASONS` | add value. |
| — | Belt-and-suspenders set | `ToolDispatcher.CANONICAL_ESCALATION_REASONS` | add value — **critical**: `ToolDispatcher.dispatch` coerces any non-canonical reason to `service_degraded` before the tool runs (`:114-125`); membership here is what lets an LLM-selected value flow through unchanged. |
| 3 | Resolver set + precedence | `EscalationReasonResolver.CANONICAL_REASONS` + `PRIORITY` | add value at a new **Tier-5 priority 50** (strictly lowest). NOT added to `TERMINAL_CLOSE_REASONS` (it is a semantic reason). |
| 7 | Eval enum (consumer) | `case_spec/schema.py` `EscalationTrigger` Literal | add value (the collector enum-check `:631` + post-init validator derive from this single source). |
| 9 | Enum-sync test | `tests/scoring/test_escalation_enum_sync.py` | `(23 values)`→`(24)` regex + docstring. |

**Commit (b) `266e803b` — projection teaching + confirm.yaml:**

| # | Surface | Path | Change |
|---|---|---|---|
| 5 | Projection vocabulary | `system_prompt.txt` (decision tree) + `ContextProjectionBuilder.buildRequestHandoverArgsSchema` | add value to the projected enum; add a **BOT-INITIATED, UNRESOLVED** decision-tree section + a reason `description`. LLM-soft semantic teaching (no keyword/regex/content heuristic): reserves `user_requested` for an actual user request; forbids `service_degraded`/`out_of_scope`/budget substitution; forbids catch-all use. Count `(23 values)`→`(24)`. |
| — | Handover payload text | `HandoverPayloadAssembler.buildPartialAnswerOrBlocker` | explicit reason-hint arm (default was already semantically correct). |
| 6 | CONFIRM skill | `skills/confirm.yaml:20` | replace `request_handover(reason='user_dissatisfied')` (which silently canonicalized to `service_degraded`) with `agent_unable_to_resolve`, gated on no-user-request + no-higher-reason, RESOLVE-retry-first, `user_requested` for genuine requests. |
| — | Golden snapshot | `PhaseEvaluatorSkillIntegrationTest` `CONFIRM_SYSTEM_INSTRUCTION` | update the existing confirm.yaml procedure snapshot in lockstep so the boundary stays green (existing snapshot, not a new test). |

**Commit (c) `34f7eb23` — eval scoring family placement:**

| # | Surface | Path | Change |
|---|---|---|---|
| 8 | Eval scoring | `scoring/escalation_reason_match.py` `_ESCALATION_REASON_FAMILY` | add value to the `bot_limit` family ("budget or inability to resolve" — the new value is the semantic "inability to resolve"). Observation-only (advisory; excluded from tier-0); no gating PASS change. |

**Commit (d) `f800618c` — characterization + §6 precedence tests:** §3/§4 below.

**Surfaces deliberately NOT changed (verified):**
- `canonicalize` (both `EscalationReasonResolver` + `PhaseEvaluator`) — **no
  remap of `user_dissatisfied`**; it still → `service_degraded` (the
  `PhaseEvaluatorPlanTest.interpretRunResult_confirm_escalate_transitionsToEscalate`
  characterization is preserved). Decision recorded: fix at the emission site
  (confirm.yaml), not a content-assumption string remap — respects the
  anti-误杀 "no auto-stamp into the new value" fence. Unknown → `service_degraded`
  unchanged.
- `ControlKernel.isInfraEscalationReason` + autoloop `_INFRA_ESCALATION_REASONS`
  (`loop.py` / `eval_runner.py`) — the new value is a **legitimate business
  escalation, not infra**, so it is intentionally absent (K0 evidence allowed;
  not infra-tagged).
- `trace/collector.py`, `baseline_loader.py`, `policy_table.py`,
  `case_outcome_resolver.py` — these do not enumerate the reason set
  (collector derives its enum from `schema.py`; loaders treat reasons as plain
  strings), so backward-compat is structural — no change needed.
- All frozen runtime/eval surfaces per objective §4 (phase machine,
  `record_outcome`, premature guard, RESOLVE→CONFIRM, `source_ids`, max-turn,
  budget family, `resolveMaxStepsReason`, `detectExplicitUserEscalation`,
  CaseSpec/PRIMARY expectations, canonical baseline pointer) — untouched.

## §3 Precedence proof (§6)

`agent_unable_to_resolve` is placed at **priority 50** — a new Tier-5
"semantic last-resort" slot, the strictly **highest priority number** (= lowest
priority) in the table. In `resolve(existing, candidate)` (lower number wins),
this means every genuinely-present reason wins over it.

Proven by `EscalationReasonResolverTest`:
- `agentUnableToResolve_hasLowestPriority` — `priorityOf == 50` and **every**
  other canonical reason has a strictly smaller priority number (loop over
  `CANONICAL_REASONS`).
- `everyOtherReasonBeatsAgentUnableToResolve_bothOrders` — `@ParameterizedTest`
  over all **23** other canonical reasons, asserting each WINS in **both**
  orders (`resolve(new, higher)==higher` and `resolve(higher, new)==higher`).
  Covers Tier-0 user signal (`imminent_harm`/`user_requested`/`user_distress`),
  Tier-1 safety/dispute/compliance, Tier-2 intake/scope, Tier-3 infra, and the
  Tier-4 budget family.
- `agentUnableToResolve_survivesWhenNothingHigherPresent` — when nothing higher
  is on the session, the LLM-selected value survives `resolve()` unchanged
  (the §2-class case).
- `agentUnableToResolve_isNotTerminalClose` / `agentUnableToResolve_isCanonical`.

Rationale (one paragraph): the §2-class is defined as "no higher-priority
reason applies, **including** a real budget/failure termination." Placing the
value below the budget family (40-42) is therefore correct, not anomalous: if
the bot genuinely hit a turn/clarification/FAQ cap, that budget reason is the
honest one and wins; `agent_unable_to_resolve` sticks only when the bot decided
on its own that it had exhausted its grounded options before any cap and
nothing higher is present. Being strictly lowest, it can never displace a
safety/dispute/user reason — the resolver precedence floor is fully preserved
(adds no Tier-0).

## §4 Characterization (§7.2)

**Genuine user request → `user_requested` (preserved):** existing resolver
tests (`userRequestedStillBeatsUserDistress`, `candidateUpgradesLowerPriorityExisting`,
…) are unchanged; the genuine `user_requested` path and `detectExplicitUserEscalation`
were not touched.

**§2-class bot-initiated → new value (not `user_requested`/`service_degraded`):**
- Java unit: `agentUnableToResolve_survivesWhenNothingHigherPresent`.
- Java integration: `AgentRunLoopHandoverReasonNormalizationIntegrationTest.llmEmitsAgentUnableToResolve_botInitiated_flowsThroughUnchanged`
  — a bot-initiated handover where the LLM selects `agent_unable_to_resolve`
  and no higher session reason exists reaches the persisted trace unchanged
  (NOT coerced to `service_degraded`, NOT relabelled `user_requested`).
- Python: `test_contract_validation.test_request_handover_agent_unable_to_resolve_is_accepted`
  (strict-mode collection accepts the value; no enum_violation);
  `test_escalation_trigger_match.test_agent_unable_to_resolve_is_valid_trigger_and_self_matches`
  + `...shares_bot_limit_family` (schema accepts it as a trigger; family =
  `bot_limit`; advisory/observation-only).

## §5 Backward-compat replay (§7.3)

Deterministic replay over 12 historical `eval_interactive/results/2026*/results.json`:
- The new value `agent_unable_to_resolve` is **absent** from every historical
  artifact → nothing rewritten, no contamination.
- All **canonical** values present are old-23 values; none became the new one.
- The 24-value schema rejects **no** value that the 23-value schema accepted
  (additive Literal member). Pre-existing non-canonical legacy literals in old
  artifacts (`drift_hard_shift`, `user_requested_escalation`, `faq_miss_exceeded`,
  `llm_determined_escalation`, `intake_complete`) were non-canonical **before**
  this change and remain so — the migration is neutral on them (no past
  evidence rewritten).
- Loaders (`baseline_loader.py`) read reasons as plain strings; the collector
  derives its enum check from the single schema source, so it accepts all old
  values unchanged.

## §6 Zero-LLM scoring replay (§7.4)

- The `_ESCALATION_REASON_FAMILY` change is **purely additive** (git-verified:
  only `+ "agent_unable_to_resolve": "bot_limit"`; the 23 existing entries
  byte-unchanged). A family-stability replay over all 23 historical values
  shows **zero family drift** — `reason_family()` is identical for every old
  value.
- The only consumer of the family map in `hard_checks.py` is
  `_check_escalation_reason_family_match` (Part-2) — **observation-only**
  (advisory severity; excluded from `_TIER0_PY_FAMILY`; re-elevated only by an
  APPROVED per-case override, of which the registry has **zero**). No gating
  hard-check enumerates the canonical reason set in a behavior-changing way
  (`escalation_compliance` Part-1, `escalation_reason_consistency`,
  `required_escalation`, `user_requested_escalation`, `trace_minimum` do not
  depend on the specific value or the set membership).
- The new value never appears in historical traces ⇒ replaying recorded
  bad_cases/anchor/shadow traces through the new scoring is byte-identical.
  **No gated outcome changes; no PASS widened.**
- **§8 standing guards** (`anchor_uc_g_gdpr`, `anchor_uc_fp_removed`,
  `cs095_uc_d_email_recovery_misroute`, `cs38s01` UC-J scam, UC-I payment, UC-G
  GDPR, explicit-human-request → `user_requested`, genuinely-unresolved
  escalate-after-help): their **scoring** is provably unaffected by the scoring
  change (new value absent + observation-only). Their LLM **behavior** under
  the new projection/prompt is covered by §7.6 (real-LLM), which is pending.

## §7 Suites (§7.5)

Re-measured at clean-tree start, then after edits, deltas attributed:

| Suite | Baseline (clean tree) | After edits | Delta |
|---|---|---|---|
| Java (`mvn test`) | 1394 / 1F / 0E / 2S | **1422 / 1F / 0E / 2S** | **+28 tests, all passing**; sole failure = inherited OQ-S41.5 `SystemPromptUserRequestedTiebreakerTest` (pre-existing, uncoupled). |
| eval_interactive pytest | 616p / 6f / 5e | **620p / 6f / 5e** | **+4 passing**; failed/error set **byte-identical** to baseline (5 corpus_lint env-drift + 1 alice count-drift + 5 rescore absent-fixture). |
| autoloop pytest | 412p | **412p** | unchanged. |

+28 Java = 27 in `EscalationReasonResolverTest` (4 standalone + 23 parameterized
precedence pairings) + 1 integration flow-through. No new regression in any
suite.

## §8 Bounded real-LLM validation (§7.6) — PASS (no over-use)

**Environment** (brought up via the project's established `Makefile` path):
Postgres@17 + Redis already running; backend **rebuilt from Sprint 096 final
(`87bc331e`)** via `mvn spring-boot:run -Dspring-boot.run.profiles=local` under
`caffeinate -dimsu`, with `.env.local` exported (DeepSeek bot LLM + Kimi/
`moonshot-v1-32k` simulator). Health UP (db/redis/ping). **Served-code verified**:
`javap` shows the new value in the constant pool of `EscalationReasonResolver`,
`ContextProjectionBuilder`, `PhaseEvaluator`, `ToolDispatcher`; the **live
projected `request_handover` schema captured in a trace lists all 24 values +
the new teaching `description`** (definitive served-projection confirmation).

**§5.9 pre-flight: GO** — 1-case pipeline validated end-to-end (backend →
simulator LLM → trace → `escalation_reason` captured).

**Run:** 5 passes × the 10-case subset = **50 live sessions** (run dirs
`20260620-140854 … 142148`), `parallel=1`, `caffeinate`. Subset: 4 §2-class
targets + 1 genuine-request control + 5 precedence/standing guards.

**Per-case `escalation_reason` distribution (5 attempts):**

| class | case | distribution | read |
|---|---|---|---|
| TARGET | `anchor_outcome_uc_c_messaging` | **`agent_unable_to_resolve` 3**, `turn_budget_exhausted` 1, none 1 | correct §2-class use (was `user_requested` in exp-90) |
| TARGET | `anchor_outcome_uc_d_login` | `user_distress` 3, `user_requested` 1, **`agent_unable_to_resolve` 1** | distress correctly wins; new reason used once |
| TARGET | `anchor_outcome_uc_a_visibility` | none 4, `user_requested` 1 | mostly resolves |
| TARGET | `cs001_uc_c_mechanical_template_escalate` | none 5 (4 resolved) | bot resolved |
| CONTROL | `cs029_uc_d_account_locked_callback` | **`user_requested` 3**, none 2, **new 0** | genuine request stays `user_requested` ✅ |
| GUARD | `anchor_outcome_uc_g_gdpr` | `intake_complete_for_uc_g` 5, **new 0** | ✅ |
| GUARD | `anchor_outcome_uc_i_payment` | `intake_complete_for_uc_i` 4, `incomplete_intake` 1, **new 0** | ✅ |
| GUARD | `anchor_outcome_uc_j_safety` | `intake_complete_for_uc_j` 1, none 4, **new 0** | ✅ |
| GUARD | `cs095_uc_d_email_recovery_misroute` | none 5 (resolved), **new 0** | ✅ |
| GUARD | `anchor_outcome_uc_fp_removed` | **`agent_unable_to_resolve` 1**, `clarification_budget_exhausted` 1, none 3 | see adjudication |

**No-over-use verdict (V3 noise-aware + transcript):** the new reason **does
not bleed** onto the genuine-request control or any high-priority guard (GDPR /
payment / safety / `service_degraded` 0 everywhere / `out_of_scope` 0). The one
non-target appearance — `anchor_outcome_uc_fp_removed` 1/5 — was **inspected by
transcript** and is a **legitimate §2-class use, not displacement**: on that
attempt the bot classified the ambiguous "what happened to my ad / I can't see
it" as **UC-A** (in-scope visibility), looked it up, the account/ad was not
found, the user never asked for a human, and no appeal/safety/payment/GDPR
framing applied → it honestly stamped `agent_unable_to_resolve` (summary:
"…account was not found… Needs human investigation"). No genuinely-higher reason
was displaced, so this is correct, not over-use.

**Correct-use evidence (transcripts):**
- `uc_c_messaging`: FAQ answer given → user still not receiving notifications
  after a friend's test → handover `agent_unable_to_resolve` "…despite correct
  email and test message… Needs account-level investigation." (the exact WP1
  Class-B case, formerly `user_requested`.)
- `uc_d_login`: reset steps given → "still can't log in, can you help me?" →
  handover `agent_unable_to_resolve` "…cannot log in after password reset. Needs
  account-level investigation."

**Precedence holds in live LLM behavior:** `user_distress` (uc_d 3/5),
`intake_complete_*` (GDPR/payment/safety), `user_requested` (control 3/5), and
the budget family all correctly win where genuinely present — the §6 floor is
preserved by the model, not just the resolver.

**Zero-LLM attribution cross-check:** the scoring code is unchanged (gates
1-5/7), so `escalation_reason` is the bot's raw LLM choice. The observed change
vs the pre-change WP1 exp-90 baseline (uc_c_messaging `user_requested` 3/5) is a
clean move onto `agent_unable_to_resolve` 3/5 — attributable **solely** to the
projection/prompt teaching (the only behavioral change), with no higher-priority
reason displaced.

**Disposition: §7.6 PASS.** The model uses the new reason for the §2-class and
does not over-use it. No §8 STOP condition fires. The bounded run is a
**validation run, not a re-bless** — no baseline/canonical movement (per §9).
No run artifacts committed.

## §9 Scoring-SHA + baseline-compat (§7.7)

- Scoring-code SHA: **HEAD `f800618c3a88e2f7b0ab55fdfbef820c42681b16`**; scoring
  blob (`escalation_reason_match.py`) `4ac4a698b075f2a69b9f3f9debcba7a43462274f`;
  schema touch `ad2191b5`, scoring touch `34f7eb23`.
- Analysis: the schema change is an additive Literal member; the scoring change
  is an additive family-map key. The only consumer is the observation-only
  `escalation_reason_family_match`. No existing canonical-baseline case's trace
  contains the new value, and no gating check changed behavior ⇒ **no existing
  baseline case's scored outcome changes**.
- **Decision: no re-bless, no canonical-pointer move (§5.7 N/A).** Additive +
  observation-only + no scored-outcome change. The baseline / canonical pointer
  stays frozen. (Had any scored outcome changed, the objective §8 requires STOP
  + human decision — it did not.)

## §10 Frozen-surface compliance (§4)

No frozen surface touched: phase machine / `PhaseEvaluator` post-loop /
`ResolveDispositionEvaluator` / premature guard / RESOLVE→CONFIRM /
`BotTurn.sourceIds` / `record_outcome` / `isResolvedSuccessTerminal` / max-turn
/ handover eligibility / `resolveMaxStepsReason` + budget family +
`TERMINAL_CLOSE_REASONS` / `detectExplicitUserEscalation` + genuine
`user_requested` path / CaseSpec-PRIMARY expectations (no expected-reason or
expected-outcome changed to require the new value; no PASS widened) / baseline +
canonical pointer. No user-message keyword/regex/content heuristic; no CaseSpec
ID / fixed utterance / ad ID / benchmark branch; no `canonicalize` auto-mapping
into the new value; no default stamp.

## §11 §7 stanza compliance

Target layer `prompt_projection` + `semantic_planner` (LLM selects the honest
reason, §1.3); the enum add is a **Runtime §1.4 vocabulary completion**,
human-authorized (cf. [[feedback_capability_config_vs_semantic_fence]] — narrow
lockstep schema edits + the golden enum-sync test updated). **Adds no Tier-0**
(precedence floor preserved; new value strictly lowest-priority, cannot displace
any safety/dispute/user reason). **No semantic hardcode** (vocabulary
completion; the LLM owns the choice; no keyword/regex/per-UC matrix/content
heuristic; NOT a generic fallback). Coverage target/neighbor/negative/shadow =
§2-class bot-initiated (Java + zero-LLM done; real-LLM pending §7.6) /
other-UC bot-initiated (no regression — suites green) / genuine `user_requested`
+ `out_of_scope` + `service_degraded` + safety/payment/GDPR/dispute/intake +
budget must NOT flip (§6 precedence + §7.4 zero-LLM replay proven; §7.6
real-LLM no-over-use pending) / shadow held-out no regression (§7.4).

## §12 Required explicit records (objective §12)

- This migration **delivers WP1's reason-honesty intent only.** It does not
  change whether or when handover occurs, nor any phase transition or outcome.
- It **does NOT solve the M-Auto-9 PRIMARY closure / product-contract question**
  (the unsatisfiable-persona problem; making `record_outcome(resolve)` land).
- **WP0** (`source_ids` / promotion-evidence) and **WP2** (CONFIRM
  record-vs-handover on satisfiable flows) **remain HELD** under charter §6.

## §13 Acceptance-gate disposition (objective §11)

**COMPLETE — all gates met.** §3 atomic (enum-sync green at 24); §5 name
reviewed + human-blessed; §6 precedence proven; §7.1-§7.5 + §7.7 gates met;
backward-compat clean; no PASS widening; suites green; scoring-SHA/baseline-compat
resolved without a re-bless; **§7.6 bounded real-LLM PASS (no over-use)**; **§4.1
Codex `approve` / `pass`** (blocking_count 0). No §8 STOP condition fired.

## §14 Evidence / closeout artifacts

- Commits `ad2191b5` (a) → `266e803b` (b) → `34f7eb23` (c) → `f800618c` (d) on
  `auto-loop-branch`; 16 files, +319/−27.
- Baseline logs: `/tmp/sprint096_java_baseline.log` (1394/1/0/2),
  `/tmp/sp096_java_d.log` (1422/1/0/2).
- `docs/codex-findings.md` — §4.1 per-sub-sprint Codex verdict (PENDING).
- No data/eval artifacts committed.
